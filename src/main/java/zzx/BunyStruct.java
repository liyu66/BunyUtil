package zzx;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;

import zzx.utils.LittleEndianRandomAccessFile;

public class BunyStruct implements Closeable {
	
	public static int MAX_BUFFER_SIZE = Integer.MAX_VALUE;
	
	public class FileInside {
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
		private final long num; // Compressed Data Block Number
		
		public FileInside(int index) throws IOException {
			this.index = index;
			
			raf.seek(getOffsetInToc());
			
		    type = raf.readLong();
		    size = raf.readLong();
		    nameOffset = raf.readInt();
		    nameSize = raf.readInt();
		    offset = raf.readLong();
		    zsize = raf.readLong();

		    long currentPos = raf.getFilePointer();
		    raf.seek(nameTableOffset + nameOffset);

		    byte[] nameBytes = new byte[nameSize];
		    raf.readFully(nameBytes);
		    name = new String(nameBytes, StandardCharsets.US_ASCII);

		    raf.seek(currentPos);
		    
		    if (!isCompressed()) {
		    	num = 0;
		    } else {
		    	currentPos = raf.getFilePointer();
		    	raf.seek(offset + 0x10L);
		    	num = raf.readLong();
		    	raf.seek(currentPos);
		    }
		}
		
		public void extractTo(String outputPath) throws IOException {
		    File outputFile = new File(outputPath, name);
		    File parentDir = outputFile.getParentFile();
		    if (!parentDir.exists() && !parentDir.mkdirs()) {
		        throw new IOException("Failed to create directories: " + parentDir);
		    }

		    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
				if (isCompressed()) {
					extractCompressed(out);
				} else {
					extractRaw(out);
				}
		    }
		}
		
		private void extractRaw(OutputStream out) throws IOException {
			raf.seek(offset);
			
			long remainSize = zsize;
			while (remainSize > MAX_BUFFER_SIZE) {
				byte[] buffer = new byte[MAX_BUFFER_SIZE];
				raf.readFully(buffer);
				out.write(buffer);
				remainSize -= MAX_BUFFER_SIZE;
			}
			if (remainSize > 0) {
				byte[] buffer = new byte[(int) remainSize];
				raf.readFully(buffer);
				out.write(buffer);
			}
		}
		
		private void extractCompressed(OutputStream out) throws IOException {
			long actualOffset = offset + 0x18L + (num * 8L);
			long actualZsize = zsize - 0x18L - (num * 8L);
			
			raf.seek(actualOffset);
			if (actualZsize > Integer.MAX_VALUE) {
				// Even in data.buny, there is no file size exceeding 2GB
				throw new IOException(
						"Failed to decompress a FileInside in the bundle, " +
						"because its size exceeds the buffer limit (>2GB)");
			}
		    byte[] compressedData = new byte[(int) actualZsize];
		    raf.readFully(compressedData);
		    
		    if (!isActualCompressed()) {
		        out.write(compressedData);
		        return;
		    }

		    try (InputStream in = new ZstdInputStream(new ByteArrayInputStream(compressedData))) {
		        byte[] buffer = new byte[(int) actualZsize];
		        int bytesRead;
		        while ((bytesRead = in.read(buffer)) != -1) {
		            out.write(buffer, 0, bytesRead);
		        }
		    }
		}
		
		public void redirect(String newFile) throws IOException {
			if (isCompressed()) {
				redirectCompressed(newFile);
			} else {
				redirectRaw(newFile);
			}
		}
		
		private void redirectRaw(String newFile) throws IOException {
		    File file = new File(newFile);
		    long newSize = file.length();
		    long newOffset = raf.length();

		    // append newFile to the end of bunyFile
		    try (FileInputStream fis = new FileInputStream(file)) {
		        raf.seek(newOffset);

		        byte[] buffer = new byte[8192];
		        int bytesRead;
		        while ((bytesRead = fis.read(buffer)) != -1) {
		            raf.write(buffer, 0, bytesRead);
		        }
		    }
		    
		    redirect(newOffset, newSize, newSize);
		}
		
		private void redirectCompressed(String newFile) throws IOException {
			File file = new File(newFile);
			long newNum = 1;
		    long newSize = file.length();
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
		    raf.writeLong(newSize);			// size (only if it's non-compressed)
		    raf.writeLong(newNum);			// block number

		    raf.writeInt(0);				// 0: non-compressed;  1: compressed
		    raf.writeInt(calcUnknowValue((int) newZsize));	// unknow value
		    
		    // Write the content of the new file, but without actually compressing it.
		    try (FileInputStream fis = new FileInputStream(file)) {
		        byte[] buffer = new byte[8192];
		        int bytesRead;
		        while ((bytesRead = fis.read(buffer)) != -1) {
		            raf.write(buffer, 0, bytesRead);
		        }
		    }
		    
		    redirect(newOffset, newZsize, newSize); 
		}
		
		private int calcUnknowValue(int zsize) {
			// It's inferred through patterns, and I don't know why it's calculated that way
			// And I haven't tested whether this value has actual utility either
			return 512 * zsize - 16896;
		}
		
		public void redirect(long newOffset, long newZsize, long newSize) throws IOException {
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
			long actualZsize = zsize - 0x18L - (num * 8L);
			return size != actualZsize;
		}
		
		public long getOffsetInToc() {
			return tocOffset + (index * 0x28L);
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

		public long getNum() {
			return num;
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
		    	sb.append(", num=").append(num);
		    } else {
		    	sb.append(", isCompressed=").append(false);
		    	sb.append(", size=").append(size);
		    }
		    
		    sb.append(", nameOffset=").append(nameOffset);
		    sb.append(", nameSize=").append(nameSize);
		    
		    return sb.toString();
		}
		
		public String toStringCompressedHeader() throws IOException {
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
	}
    private LittleEndianRandomAccessFile raf;

    private String idString;

    private long tocOffset;
    private long tocSize;
    private long nameTableOffset;
    private long nameTableSize;
    private long toc2Offset;	// What's the use of toc2?
    private long toc2Size;		// I don't know what it is. It doesn't seem to contain any file info.
    
    private List<FileInside> files = new ArrayList<>();
    private Map<String, FileInside> nameToFile = new HashMap<>();
    
    public BunyStruct(String bunyFile) throws IOException {
    	open(bunyFile);
        readHeader();
    }

    private void open(String bunyFile) throws IOException {
        File file = new File(bunyFile);
        if (!file.exists()) {
            throw new FileNotFoundException("The buny file does not exist: " + bunyFile);
        }
        raf = new LittleEndianRandomAccessFile(file, "rw");
    }

    private void readHeader() throws IOException {
    	// 0x00 ~ 0x0F: a magic string, its value should be "BunyArchTheForge"
        raf.seek(0x00);
        byte[] magic = new byte[16];
        raf.readFully(magic);
        idString = new String(magic, StandardCharsets.US_ASCII).trim();
        
        // 0x10 ~ 0x1F: nothing

        // 0x20 ~ 0x50: header info
        raf.seek(0x20);
        tocOffset = raf.readLong();	// its value should be 0x50
        tocSize = raf.readLong();
        nameTableOffset = raf.readLong();
        nameTableSize = raf.readLong();
        toc2Offset = raf.readLong();
        toc2Size = raf.readLong();
    }
    
    public List<FileInside> readFiles() throws IOException {
    	// Read info and name of each file from toc and nameTable
    	for (int i = 0; i < getFileCount(); i++) {
    		readFile(i);
    	}
    	return files;
    }
    
    public List<FileInside> readFilesIfNotExist() throws IOException {
    	if (files.size() <= getFileCount()) {
    		readFiles();
    	}
		return files;
    }
    
    public FileInside readFile(int i) throws IOException {
    	FileInside file = new FileInside(i);
		files.add(i, file);
		nameToFile.put(file.name, file);
		return file;
    }
    
    public FileInside readFileIfNotExist(int i) throws IOException {
    	return getFile(i) == null ? readFile(i) : getFile(i);
    }
    
    public void update(String dirPath, 
    		Consumer<String> onSuccess, Consumer<String> onSkip)
    		throws IOException
    {
        Path start = Paths.get(dirPath);
        try (Stream<Path> stream = Files.walk(start)) {
        	
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                String fileName = start.relativize(path).toString();
                FileInside fileInside = getFile(fileName);
                if (fileInside != null) {
                	fileInside.redirect(path.toString());
                	onSuccess.accept(fileName);
                } else {
                	onSkip.accept(fileName);
                }
            }
        }
    }
    
    public FileInside getFile(int i) {
    	return files.get(i);
    }
    
    public FileInside getFile(String fileName) {
    	String normalized = fileName.replace('\\', '/');
        return nameToFile.get(normalized);
    }
    
    public List<FileInside> getFiles() {
    	return files;
    }

    public String getIdString() {
        return idString;
    }

    public long getTocOffset() {
        return tocOffset;
    }

    public long getTocSize() {
        return tocSize;
    }

    public long getNamesOffset() {
        return nameTableOffset;
    }

    public long getNamesSize() {
        return nameTableSize;
    }

    public long getToc2Offset() {
        return toc2Offset;
    }

    public long getToc2Size() {
        return toc2Size;
    }
    
    public long getFileCount() {
    	return getTocSize() / 0x28L;
    }
    
    public long getLength() throws IOException {
    	return raf.length();
    }

    public void setNewLength(long newLength) throws IOException {
    	raf.setLength(newLength);
    }

    @Override
    public void close() throws IOException {
        if (raf != null) {
            raf.close();
        }
    }
}