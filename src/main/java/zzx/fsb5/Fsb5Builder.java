package zzx.fsb5;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import zzx.utils.LERandomAccessFile;

public class Fsb5Builder {
	private static final byte[] ZERO_PADDING_BUFFER = new byte[16]; // All zeros by default
	
	private List<Sound> sounds = new ArrayList<>();
	
	private int version = Config.DEFAULT_VERSION;
	// private int soundCount;
	// private int tableSize;
	private int nameTableSize = Config.DEFAULT_NAME_TABLE_SIZE;
	// private int dataSize;
	private int codec = Config.DEFAULT_CODEC;
	private byte[] unknownBytes = Config.DEFAULT_UNKNOWN_BYTES;
	
	public Fsb5Builder() {}
	
	public Fsb5Builder(Fsb5Reader reader) {
		for (Sound sound : reader.getAllSounds()) {
			addSound(sound);
		}
	}
	
	public void addSound(Sound sound) {
		sounds.add(sound);
	}
	
	public void setSound(int i, Sound sound) {
		sounds.set(i, sound);
	}
	
	public void buildTo(String outputFile) throws IOException {
		try (LERandomAccessFile raf = new LERandomAccessFile(outputFile, "rw")) {
			buildTo(raf);
		}
	}
	
	public void buildTo(LERandomAccessFile raf) throws IOException {
		long baseOffset = raf.getFilePointer();
		int soundCount = getSoundCount();
		
		// write header part
		// 0x00 ~ 0x1B
		raf.writeString("FSB5", "FSB5".length());	// id string
		raf.writeInt(version);						// version
		raf.writeInt(soundCount);					// sound count
		raf.writeInt(0);							// table size(Temporarily use 0 to occupy space)
		raf.writeInt(nameTableSize);				// name table size
		raf.writeInt(0);							// data size(Temporarily use 0 to occupy space)
		raf.writeInt(codec);						// codec
		
		// 0x1C ~ 0x3B
		raf.write(unknownBytes, 0, 32);
		
		// write table part
		long dataOffset = 0;
		for (int i = 0; i < soundCount; i++) {
			Sound sound = sounds.get(i);
			
			// write sampleMode (need to update it's data offset)
			SampleMode newSampleMode = sound.getSampleMode().setDataOffset(dataOffset);
			raf.writeLong(newSampleMode.get());
			
			// write chunk
			for (Chunk chunk : sound.getAllChunks()) {
				chunk.writeTo(raf);
			}
			
			// calc next sound's data offset(must be aligned to a 16-byte boundary)
		    dataOffset += sound.getDataSize();
		    dataOffset += calcPadding(dataOffset);
		}
		
		int tableSize = (int) (raf.getFilePointer() - baseOffset - Config.HEADER_SIZE);
		
		// write name table part
		// do nothing...
		
		// write data part
		long dataPartStart = Config.HEADER_SIZE + tableSize + nameTableSize;
		dataOffset = 0;
		for (int i = 0; i < soundCount; i++) {
			Sound sound = sounds.get(i);
			
			long actualDataOffset = baseOffset + dataPartStart + dataOffset;
			raf.seek(actualDataOffset);
			sound.writeDataTo(raf);
			raf.seek(actualDataOffset + sound.getDataSize());
			
		    dataOffset += sound.getDataSize();
		    int padding = calcPadding(dataOffset);
		    dataOffset += padding;
		    
		    // padding 0 to align 16 bytes
		    if (padding > 0) {
		        raf.write(ZERO_PADDING_BUFFER, 0, padding);
		    }
		}
		
		int dataSize = (int) (raf.getFilePointer() - dataPartStart - baseOffset);
		long fileEnd = raf.getFilePointer();
		
		// rewrite part of the header
		raf.seek(baseOffset + 0 + 4 + 4 + 4);
		raf.writeInt(tableSize);
		raf.skipBytes(4);
		raf.writeInt(dataSize);
		
		raf.seek(fileEnd);
	}

	public void setVersion(int version) {
		this.version = version;
	}

	// Only supports building Fsb5 files that do not contain sound names
	/*
	 * 	public void setNameTableSize(int nameTableSize) {
	 *		this.nameTableSize = nameTableSize;
	 * 	}
	 */

	public void setCodec(int codec) {
		this.codec = codec;
	}

	public void setUnknownBytes(byte[] unknownBytes) {
		this.unknownBytes = unknownBytes;
	}
	
	public int getSoundCount() {
		return sounds.size();
	}
	
	// Calculates the number of zero bytes needed to align to a 16-byte boundary
	private static int calcPadding(long offset) {
	    return (int) ((16 - (offset % 16)) % 16);
	}

}