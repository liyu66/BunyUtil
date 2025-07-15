package zzx.buny;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import zzx.utils.LERandomAccessFile;

public class BunyStruct implements Closeable {
    private LERandomAccessFile raf;

    // A magic string, its value should be "BunyArchTheForge"
    private String idString;

    // A table containing the info of the files inside .buny
    private long tocOffset;			
    private long tocSize;
    
    // A table containing the name of the files inside .buny
    private long nameTableOffset;	
    private long nameTableSize;
    
    // What's the use of toc2?
    // I don't know what it is. It doesn't seem to contain any file info.
    private long toc2Offset;
    private long toc2Size;
    
    private FileInside[] files;
    private Map<String, FileInside> nameToFile;
    
    public BunyStruct(String bunyFile) throws IOException {
    	open(bunyFile);
        readHeader();
        files = new FileInside[(int) getFileCount()];
        nameToFile = new HashMap<>((int) getFileCount());
        // readFiles();
    }

    private void open(String bunyFile) throws IOException {
        File file = new File(bunyFile);
        if (!file.exists()) {
            throw new FileNotFoundException("The buny file does not exist: " + bunyFile);
        }
        raf = new LERandomAccessFile(file, "rw");
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
    
    public void readFiles() throws IOException {
    	for (int i = 0; i < getFileCount(); i++) {
			readFile(i);
		}
    }
    
    public FileInside readFile(int index) throws IOException {
    	if (files[index] != null)	{
    		return files[index];
    	}
    	
    	FileInside file = new FileInside(this, index);
		files[index] = file;
		nameToFile.put(file.getName(), file);
		return file;
    }
    
    public FileInside getFile(int i) {
    	return files[i];
    }
    
    public FileInside getFile(String fileName) {
        return nameToFile.get(fileName.replace('\\', '/'));
    }
    
    public FileInside[] getAllFiles() {
    	return files;
    }
    
    public boolean containFile(String fileName) {
    	return nameToFile.containsKey(fileName.replace('\\', '/')); 
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

    public long getNameTableOffset() {
        return nameTableOffset;
    }

    public long getNameTableSize() {
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
    
    // Although exposing the raf is not a good design, it's just too convenient.
    public LERandomAccessFile getRaf() {
    	return raf;
    }
}