package zzx.buny;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.github.luben.zstd.ZstdInputStream;

import zzx.utils.LERandomAccessFile;
import zzx.utils.LERandomAccessFileSlice;

public class FileInside {
	private final BunyStruct buny;
	private final int index;
	
	// read from toc
	private final long type;
	private long size;
	private final int nameOffset;
	private final int nameSize;
	private long offset;
	private long zsize;	// The size after being compressed
	
	// read from nameTable
	private final String name;
	
	// read from compressed file header
	private final long blockNum; // Compressed Data Block Number
	
	FileInside(BunyStruct buny, int index) throws IOException {
		this.buny = buny;
		this.index = index;
		LERandomAccessFile raf = buny.getRaf();
		
		raf.seek(getOffsetInToc());
		
	    type = raf.readLong();
	    size = raf.readLong();
	    nameOffset = raf.readInt();
	    nameSize = raf.readInt();
	    offset = raf.readLong();
	    zsize = raf.readLong();

	    long currentPos = raf.getFilePointer();
	    raf.seek(buny.getNameTableOffset() + nameOffset);

	    byte[] nameBytes = new byte[nameSize];
	    raf.readFully(nameBytes);
	    name = new String(nameBytes, StandardCharsets.US_ASCII);

	    raf.seek(currentPos);
	    
	    if (!isCompressed()) {
	    	blockNum = 0;
	    } else {
	    	currentPos = raf.getFilePointer();
	    	raf.seek(offset + 0x10L);
	    	blockNum = raf.readLong();
	    	raf.seek(currentPos);
	    }
	}
	
	public void extractTo(String outputPath) throws IOException {
	    File outputFile = new File(outputPath, name);
	    File parentDir = outputFile.getParentFile();
	    if (!parentDir.exists() && !parentDir.mkdirs()) {
	        throw new IOException("Failed to create directories: " + parentDir);
	    }

	    extractTo(outputFile);
	}
	
	public void extractTo(File file) throws IOException {
		try (LERandomAccessFile out = new LERandomAccessFile(file, "rw")) {
			if (isCompressed()) {
				extractCompressed(out);
			} else {
				extractRaw(out);
			}
	    }
	}
	
	private void extractRaw(LERandomAccessFile out) throws IOException {
		LERandomAccessFile raf = buny.getRaf();
		raf.transferTo(out, offset, size);
	}
	
	private void extractCompressed(LERandomAccessFile out) throws IOException {
		LERandomAccessFile raf = buny.getRaf();
		long actualOffset = offset + 0x18L + (blockNum * 8L);
		long actualZsize = zsize - 0x18L - (blockNum * 8L);
		
		if (!isActualCompressed()) {
	        raf.transferTo(out, actualOffset, actualZsize);
	        return;
	    }
		
		if (actualZsize > Integer.MAX_VALUE) {
			// Even in data.buny, there is no file size exceeding 2GB
			throw new IOException(
					"Failed to decompress a FileInside in the bundle, " +
					"because its size exceeds the buffer limit (>2GB)");
		}
		
		raf.seek(actualOffset);
		
	    byte[] compressedData = new byte[(int) actualZsize];
	    raf.readFully(compressedData);

	    try (InputStream in = new ZstdInputStream(new ByteArrayInputStream(compressedData))) {
	        byte[] buffer = new byte[(int) actualZsize];
	        int bytesRead;
	        while ((bytesRead = in.read(buffer)) != -1) {
	            out.write(buffer, 0, bytesRead);
	        }
	    }
	}
	
	public void redirectTo(File newFile) throws IOException {
		try (LERandomAccessFile newRaf = new LERandomAccessFile(newFile, "r")) {
			if (isCompressed()) {
				redirectCompressed(newRaf);
			} else {
				redirectRaw(newRaf);
			}
	    }
	}
	
	private void redirectRaw(LERandomAccessFile newFile) throws IOException {
		LERandomAccessFile raf = buny.getRaf();
	    long newSize = newFile.length() - newFile.getFilePointer();
	    long newOffset = raf.length();

	    // append newFile to the end of bunyFile
	    raf.seek(newOffset);
	    newFile.transferTo(raf, newFile.getFilePointer(), newSize);
	    
	    redirectTo(newOffset, newSize, newSize);
	}
	
	private void redirectCompressed(LERandomAccessFile newFile) throws IOException {
		LERandomAccessFile raf = buny.getRaf();
		long newNum = 1;
	    long newSize = newFile.length() - newFile.getFilePointer();
	    long newZsize = newSize + 0x18L + (newNum * 8L);
	    long newOffset = raf.length();
	    
	    long maxBlockSize = 0x40000L;
	    if (newSize > maxBlockSize) {
	    	/*
	    	 * Increasing maxBlockSize (simple), 
	    	 * or trying to add support for multiple blocks (somewhat cumbersome), 
	    	 * may be able to lift this restriction.
	    	 */
	    	throw new IOException(
	    			"Replacing certain file formats with sizes over 256KB is currently not supported. "
	    			+ "Sorry for the inconvenience — if you need this feature, "
	    			+ "please let me know and I’ll do my best to support it.");
	    }
	    
	    raf.seek(newOffset);
	    raf.writeLong(maxBlockSize);	// maximum bytes per block (256kb)
	    raf.writeLong(newSize);			// size (only if it's no actually compressed)
	    raf.writeLong(newNum);			// block number

	    raf.writeInt(0);				// 0: no acutally compressed;  1: compressed
	    raf.writeInt(calcUnknowValue((int) newZsize));	// unknow value
	    
	    // Write the content of the new file, but without actually compressing it.
	    newFile.transferTo(raf, newFile.getFilePointer(), newSize);
	    
	    redirectTo(newOffset, newZsize, newSize); 
	}
	
	private int calcUnknowValue(int zsize) {
		// It's inferred through patterns, and I don't know why it's calculated that way
		// And I haven't tested whether this value has actual utility either
		return 512 * zsize - 16896;
	}
	
	public void redirectTo(long newOffset, long newZsize, long newSize) throws IOException {
		LERandomAccessFile raf = buny.getRaf();
		offset = newOffset;
		zsize = newZsize;
		size = newSize;
		
		// update TOC to redirect this file
	    raf.seek(getOffsetInToc());
	    raf.skipBytes(Long.BYTES);		  	// skip   type
	    raf.writeLong(size);			  	// update size
	    raf.skipBytes(Integer.BYTES * 2); 	// skip   nameOffset and nameSize
	    raf.writeLong(offset);			  	// update offset
	    raf.writeLong(zsize);				// update zsize
	}
	
	public boolean isCompressed() {
		return size != zsize;
	}
	
	public boolean isActualCompressed() {
		/*
		 * If a file is supposed to be compressed before being packed into a .buny archive -
		 * due to its format (e.g., .txt), but is actually very small (e.g., only around 90 bytes), 
		 * this can result in a situation where compression header information is included, 
		 * but the file content itself is not actually compressed.
		 */
		long actualZsize = zsize - 0x18L - (blockNum * 8L);
		return size != actualZsize;
	}
	
	public long getOffsetInToc() {
		return buny.getTocOffset() + (index * 0x28L);
	}
	
	public int getIndex() {
		return index;
	}

	public long getType() {
		return type;
	}

	public long getSize() {
		return size;
	}

	public int getNameOffset() {
		return nameOffset;
	}

	public int getNameSize() {
		return nameSize;
	}

	public long getOffset() {
		return offset;
	}

	public long getZsize() {
		return zsize;
	}

	public String getName() {
		return name;
	}

	public long getBlockNum() {
		return blockNum;
	}
	
	public LERandomAccessFileSlice getSlice() throws IOException {
		return new LERandomAccessFileSlice(buny.getRaf(), getOffset(), getZsize());
	}

	@Override
	public String toString() {
	    StringBuilder sb = new StringBuilder();
	    sb.append("type=").append(type);
	    sb.append(", name=").append(name);
	    sb.append(", offset=").append(offset);
	    if (isCompressed()) {
	    	sb.append(", isCompressed=").append(true);
	    	sb.append(", zsize=").append(zsize);
	    	sb.append(", size=").append(size);
	    	sb.append(", blockNum=").append(blockNum);
	    } else {
	    	sb.append(", isCompressed=").append(false);
	    	sb.append(", size=").append(size);
	    }
	    
	    sb.append(", nameOffset=").append(nameOffset);
	    sb.append(", nameSize=").append(nameSize);
	    
	    return sb.toString();
	}
	
	/*
	public String toStringCompressedHeader() throws IOException {
		if (!isCompressed())	return "";
		
		LERandomAccessFile raf = buny.getRaf();
		raf.seek(offset);
		StringBuilder sb = new StringBuilder();
		sb.append("long: ").append(raf.readLong());
		sb.append(", long: ").append(raf.readLong());
		long num = raf.readLong();
		sb.append(", num: ").append(num);
		int chunk[] = new int[(int) num * 2];
		for (int i = 0; i < num * 2; i += 2) {
			chunk[i] = raf.readInt();
			chunk[i+1] = raf.readInt();
		}
		sb.append(", chunk: ").append(Arrays.toString(chunk));
		return sb.toString();
	}
	*/
}
