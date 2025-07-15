package zzx.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Represents a read-only slice of a file, providing a window into
 * a specific byte range. Handles resource lifecycle based on ownership flag.
 */
public class LERandomAccessFileSlice extends LERandomAccessFile {
    
    private final LERandomAccessFile master;  // Underlying file
    private final long sliceOffset;           // Start offset in master file
    private final long sliceLength;           // Length of this slice
    private long slicePointer;                // Current pointer within slice
    private boolean closed = false;           // Track closed state
    private final boolean ownsMaster;         // Whether this slice owns the master file

    // ================= Constructors (with ownership control) =================
    
    /**
     * Creates a standalone read-only file slice from a file path.
     * The entire file is used as the slice.
     * 
     * @param filePath  Path to the file
     * @throws IOException If file cannot be opened or accessed
     */
    public LERandomAccessFileSlice(String filePath) throws IOException {
        this(new File(filePath));
    }
    
    /**
     * Creates a standalone read-only file slice from a File object.
     * The entire file is used as the slice.
     * 
     * @param file  The file to open
     * @throws IOException If file cannot be opened or accessed
     */
    public LERandomAccessFileSlice(File file) throws IOException {
        this(file, 0, file.length());
    }
    
    /**
     * Creates a standalone read-only file slice from a file path.
     * 
     * @param filePath  Path to the file
     * @param offset    Starting byte offset in the file
     * @param length    Length of the slice
     * @throws IOException If offset/length are invalid or out of bounds
     */
    public LERandomAccessFileSlice(String filePath, long offset, long length) throws IOException {
        this(new File(filePath), offset, length);
    }
    
    /**
     * Creates a standalone read-only file slice from a File object.
     * 
     * @param file    The file to open
     * @param offset  Starting byte offset in the file
     * @param length  Length of the slice
     * @throws IOException If offset/length are invalid or out of bounds
     */
    public LERandomAccessFileSlice(File file, long offset, long length) throws IOException {
        this(new LERandomAccessFile(file, "r"), offset, length, true);
    }
    
    /**
     * Creates a read-only file slice from a master file.
     * 
     * @param master    The underlying file to slice
     * @param offset    Starting byte offset in master file
     * @param length    Length of the slice
     * @throws IOException If offset/length are invalid or out of bounds
     */
    public LERandomAccessFileSlice(LERandomAccessFile master, long offset, long length) throws IOException {
        this(master, offset, length, false);
    }
    
    /**
     * Creates a read-only file slice with explicit ownership control.
     * 
     * @param master      The underlying file to slice
     * @param offset      Starting byte offset in master file
     * @param length      Length of the slice
     * @param ownsMaster  Whether this slice owns the master file
     * @throws IOException If offset/length are invalid or out of bounds
     */
    public LERandomAccessFileSlice(LERandomAccessFile master, long offset, long length, boolean ownsMaster) 
        throws IOException {
        
        super(master.getRaf());
        this.master = master;
        this.sliceOffset = offset;
        this.sliceLength = length;
        this.slicePointer = 0;
        this.ownsMaster = ownsMaster;
        
        validateSliceBounds();
    }
    
    private void validateSliceBounds() throws IOException {
        if (sliceOffset < 0 || sliceLength < 0) {
            throw new IOException("Invalid negative offset or length");
        }
        
        long masterLength = master.length();
        if (sliceOffset > masterLength) {
            throw new IOException("Offset beyond end of file");
        }
        
        if (sliceOffset + sliceLength > masterLength) {
            throw new IOException("Slice extends beyond end of file");
        }
    }

    // ================= Resource Management =================
    @Override
    public void close() throws IOException {
        if (closed) return;
        
        closed = true;
        
        // Only close master if we own it
        if (ownsMaster && master != null) {
            master.close();
        }
    }
    
    private void checkOpen() throws IOException {
        if (closed) throw new IOException("File slice is closed");
    }

    private void checkBounds(int required) throws IOException {
        if (slicePointer + required > sliceLength) {
            throw new IOException("Read operation exceeds slice bounds");
        }
    }

    // ================= Core Position/Length Methods =================
    @Override
    public void seek(long pos) throws IOException {
        checkOpen();
        if (pos < 0 || pos > sliceLength) {
            throw new IOException("Seek position " + pos + " out of slice bounds [0, " + sliceLength + "]");
        }
        slicePointer = pos;
    }

    @Override
    public long getFilePointer() throws IOException {
        checkOpen();
        return slicePointer;
    }

    @Override
    public long length() throws IOException {
        checkOpen();
        return sliceLength;
    }

    @Override
    public int skipBytes(int n) throws IOException {
        checkOpen();
        if (n <= 0) return 0;
        
        int skip = (int) Math.min(n, sliceLength - slicePointer);
        slicePointer += skip;
        return skip;
    }

    // ================= Read Operations =================
    
    @Override
    public void readFully(byte[] b) throws IOException {
        checkOpen();
        checkBounds(b.length);
        super.seek(sliceOffset + slicePointer);
        super.readFully(b);
        slicePointer += b.length;
    }

    // ================= Little-Endian Read Methods =================
    @Override
    public short readShort() throws IOException {
        checkBounds(2);
        super.seek(sliceOffset + slicePointer);
        short value = super.readShort();
        slicePointer += 2;
        return value;
    }

    @Override
    public int readUnsignedShort() throws IOException {
        checkBounds(2);
        super.seek(sliceOffset + slicePointer);
        int value = super.readUnsignedShort();
        slicePointer += 2;
        return value;
    }

    @Override
    public int readInt() throws IOException {
        checkBounds(4);
        super.seek(sliceOffset + slicePointer);
        int value = super.readInt();
        slicePointer += 4;
        return value;
    }

    @Override
    public long readLong() throws IOException {
        checkBounds(8);
        super.seek(sliceOffset + slicePointer);
        long value = super.readLong();
        slicePointer += 8;
        return value;
    }

    @Override
    public long readOrginalLong() throws IOException {
        checkBounds(8);
        super.seek(sliceOffset + slicePointer);
        long value = super.readOrginalLong();
        slicePointer += 8;
        return value;
    }

    @Override
    public float readFloat() throws IOException {
        checkBounds(4);
        super.seek(sliceOffset + slicePointer);
        float value = super.readFloat();
        slicePointer += 4;
        return value;
    }

    @Override
    public double readDouble() throws IOException {
        checkBounds(8);
        super.seek(sliceOffset + slicePointer);
        double value = super.readDouble();
        slicePointer += 8;
        return value;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        checkBounds(1);
        super.seek(sliceOffset + slicePointer);
        int value = super.readUnsignedByte();
        slicePointer++;
        return value;
    }

    @Override
    public byte readByte() throws IOException {
        checkBounds(1);
        super.seek(sliceOffset + slicePointer);
        byte value = super.readByte();
        slicePointer++;
        return value;
    }

    @Override
    public String readString(int length) throws IOException {
        checkBounds(length);
        super.seek(sliceOffset + slicePointer);
        String value = super.readString(length);
        slicePointer += length;
        return value;
    }

    @Override
    public String readString() throws IOException {
        checkOpen();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long startPos = slicePointer;
        long maxPos = sliceOffset + sliceLength;
        
        while (slicePointer < sliceLength) {
            super.seek(sliceOffset + slicePointer);
            int b = super.readByte() & 0xFF;
            slicePointer++;
            
            if (b == 0) break;
            baos.write(b);
        }
        
        return new String(baos.toByteArray(), "UTF-8").trim();
    }

    // ================= Disabled Write Operations =================
    @Override
    public void writeByte(int v) throws IOException {
        throw new IOException("File slice is read-only");
    }

    @Override
    public void writeShort(int v) throws IOException {
        throw new IOException("File slice is read-only");
    }

    @Override
    public void writeInt(int v) throws IOException {
        throw new IOException("File slice is read-only");
    }

    @Override
    public void writeLong(long v) throws IOException {
        throw new IOException("File slice is read-only");
    }

    @Override
    public void writeFloat(float v) throws IOException {
        throw new IOException("File slice is read-only");
    }

    @Override
    public void writeDouble(double v) throws IOException {
        throw new IOException("File slice is read-only");
    }

    @Override
    public void write(byte[] b) throws IOException {
        throw new IOException("File slice is read-only");
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        throw new IOException("File slice is read-only");
    }

    @Override
    public void writeString(String str, int length) throws IOException {
        throw new IOException("File slice is read-only");
    }

    @Override
    public void setLength(long newLength) throws IOException {
        throw new IOException("File slice is read-only");
    }

    // ================= Special Method Handling =================
    @Override
    protected FileChannel getChannel() {
        throw new UnsupportedOperationException("Direct channel access disabled for file slices");
    }

    @Override
    public void transferFrom(LERandomAccessFile src, long destOffset, long len) throws IOException {
        throw new IOException("File slice is read-only");
    }
    
    @Override
    public void transferTo(LERandomAccessFile dest, long srcOffset, long len) throws IOException {
        if (len == 0) return;
        
        if (srcOffset < 0) {
            throw new IllegalArgumentException("Source offset cannot be negative");
        }
        if (srcOffset + len > this.sliceLength) {
            throw new IOException("Requested transfer exceeds slice bounds");
        }

        final long physicalOffset = this.sliceOffset + srcOffset;
        
        if (physicalOffset + len > master.length()) {
            throw new IOException("Underlying file doesn't have enough data");
        }

        FileChannel srcChannel = master.getChannel();
        FileChannel destChannel = dest.getChannel();
        
        long transferred = 0;
        while (transferred < len) {
            long count = srcChannel.transferTo(
                physicalOffset + transferred,
                len - transferred, 
                destChannel
            );
            
            if (count <= 0) {
                throw new IOException("Transfer failed at position: " + 
                                    (srcOffset + transferred));
            }
            transferred += count;
        }
    }
}