package zzx.fsb5;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import zzx.utils.LERandomAccessFile;

public class SoundFromFsb implements Sound {
	
	private final Fsb5Reader fsb5;
	private final int index;
	private final long sizeInTable;
	
	private final SampleMode sampleMode;
	private long dataSize = 0;
	private List<Chunk> chunks = new ArrayList<>();
	
	private String name;

	/*
	 * The structure of a table item mainly consists of the following two parts:
	 *
	 * 1. A 64-bit value called SampleMode, which can be further broken down into
	 *    metadata such as the number of channels, sample rate, etc.
	 *    For more details, see the SampleMode class.
	 *
	 * 2. A series of Chunks. Each Chunk consists of a 32-bit value called ChunkMode,
	 *    followed by chunk data of variable size.
	 */
	SoundFromFsb(Fsb5Reader fsb5, int index, long offsetInTable, String name) throws IOException {
		this.fsb5 = fsb5;
		this.index = index;
		this.name = name;
		LERandomAccessFile raf = fsb5.getRaf();
		raf.seek(offsetInTable);
		
		// 1. sample mode
		sampleMode = new SampleMode(raf.readLong());
		
		// 2. all chunks
		boolean endOfChunk = !hasChunk();
		while (!endOfChunk) {
			ChunkMode chunkMode = new ChunkMode(raf.readInt());
			
			int type = chunkMode.getType();
			int size = chunkMode.getSize();
			endOfChunk  = chunkMode.isLast();
			
			switch (type) {
			case 0x3:
				// loop info
				if (size != 8) {
					throw new RuntimeException("The size of the loop info chunk can only be 8");
				}
				int loopStart = raf.readInt();
				int loopEnd = raf.readInt() + 1;
				chunks.add(new LoopInfoChunk(chunkMode, loopStart, loopEnd));
				break;
			case 0xb:
				// encoding-related information
				// For more information, see the comments in the ExtraDataChunk class
				byte[] extraData = new byte[size];
				raf.readFully(extraData);
				chunks.add(new ExtraDataChunk(chunkMode, extraData));
				break;
			default:
				// unknown
				byte[] unknownData = new byte[size];
				raf.readFully(unknownData);
				chunks.add(new UnknownChunk(chunkMode, unknownData));
			}
		}
		
		sizeInTable = raf.getFilePointer() - offsetInTable;
	}
	
	// Calculate the data size of this audio by using the data offset of the next audio resource
	void init() throws IOException {
		LERandomAccessFile raf = fsb5.getRaf();
		if (index >= fsb5.getSoundCount() - 1) {
			dataSize = raf.length() - fsb5.getDataPartOffset() - getDataOffset();
		} else {
			dataSize = fsb5.getSound(index + 1).getDataOffset() - getDataOffset();
		}
	}
	
	// Used to calculate the offset of the next table item
	long getSizeInTable() {
		return sizeInTable;
	}
	
	public int getIndex() {
		return index;
	}
	
	@Override
	public long getDataSize() {
		return dataSize;
	}
	
	@Override
	public void writeDataTo(LERandomAccessFile dest) throws IOException {
		LERandomAccessFile raf = fsb5.getRaf();
		long actualDataOffset = fsb5.getDataPartOffset() + getDataOffset();
		raf.transferTo(dest, actualDataOffset, getDataSize());
	}

	@Override
	public SampleMode getSampleMode() {
		return sampleMode;
	}
	
	@Override
	public List<Chunk> getAllChunks() {
		return chunks;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
	    StringBuilder sb = new StringBuilder();
	    sb.append("[" + getIndex() + "]");
	    if (hasName()) {
	    	sb.append(" " + getName());
	    }
	    sb.append(": {");
	    sb.append("sampleNum=").append(getSampleNum())
	      .append(", dataOffset=").append(getDataOffset())
	      .append(", dataSize=").append(getDataSize())
	      .append(", channels=").append(getChannels())
	      .append(", sampleRate=").append(getSampleRate())
	      .append(", duration=").append(String.format("%.2f", getDurationSeconds())).append("s");
	    sb.append(" }");
	    return sb.toString();
	}
}
