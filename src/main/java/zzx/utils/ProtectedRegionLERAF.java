package zzx.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * LERandomAccessFile variant that protects a specific region from write operations.
 * Throws an exception when any write method attempts to modify the protected region
 * defined by [protectedStart, protectedEnd] in the constructor.
 */
public class ProtectedRegionLERAF extends LERandomAccessFile {

    private final long protectedStart;
    private final long protectedEnd;
    
    /**
     * Creates a protected-region file stream from an existing LERandomAccessFile.
     * Shares the underlying file resource and file pointer state with the original.
     * <b>Important:</b> Closing either instance will close the shared resource.
     * 
     * @param file           the source LERandomAccessFile
     * @param protectedStart start offset of protected region (inclusive)
     * @param protectedEnd   end offset of protected region (inclusive)
     */
    public ProtectedRegionLERAF(LERandomAccessFile file, 
                                           long protectedStart, long protectedEnd) {
        super(file.getRaf());  // Reuse the underlying RandomAccessFile
        validateRegion(protectedStart, protectedEnd);
        this.protectedStart = protectedStart;
        this.protectedEnd = protectedEnd;
    }

    /**
     * Creates a protected-region file stream with the specified protected range.
     * 
     * @param file           the target file
     * @param mode           access mode ("r", "rw", etc.)
     * @param protectedStart start offset of protected region (inclusive)
     * @param protectedEnd   end offset of protected region (inclusive)
     * @throws FileNotFoundException if the file cannot be opened
     */
    public ProtectedRegionLERAF(File file, String mode, 
                                           long protectedStart, long protectedEnd) 
        throws FileNotFoundException {
        super(file, mode);
        validateRegion(protectedStart, protectedEnd);
        this.protectedStart = protectedStart;
        this.protectedEnd = protectedEnd;
    }

    /**
     * Creates a protected-region file stream with the specified protected range.
     * 
     * @param name           file name
     * @param mode           access mode
     * @param protectedStart start offset of protected region (inclusive)
     * @param protectedEnd   end offset of protected region (inclusive)
     * @throws FileNotFoundException if the file cannot be opened
     */
    public ProtectedRegionLERAF(String name, String mode, 
                                           long protectedStart, long protectedEnd) 
        throws FileNotFoundException {
        super(name, mode);
        validateRegion(protectedStart, protectedEnd);
        this.protectedStart = protectedStart;
        this.protectedEnd = protectedEnd;
    }

    /**
     * Validates the protected region boundaries.
     * 
     * @param start region start
     * @param end   region end
     * @throws IllegalArgumentException if region is invalid
     */
    private void validateRegion(long start, long end) {
        if (start < 0) {
            throw new IllegalArgumentException("Protected region start cannot be negative");
        }
        if (end < start) {
            throw new IllegalArgumentException("Protected region end cannot be before start");
        }
    }

    /**
     * Checks if the write operation overlaps the protected region.
     * 
     * @param writeStart  starting offset of write operation
     * @param bytesToWrite number of bytes to write
     * @throws IOException if write overlaps protected region
     */
    private void checkProtectedRegion(long writeStart, int bytesToWrite) throws IOException {
        if (bytesToWrite <= 0) return;

        long writeEnd = writeStart + bytesToWrite - 1;
        // Check for overlap: [writeStart, writeEnd] overlaps [protectedStart, protectedEnd]
        if (writeEnd >= protectedStart && writeStart <= protectedEnd) {
            throw new IOException(String.format(
                "Write operation overlaps protected region [%d, %d] (attempted write: [%d, %d])",
                protectedStart, protectedEnd, writeStart, writeEnd
            ));
        }
    }

    //--------------------------------------------------
    // Overridden Write Methods with Protection Checks
    //--------------------------------------------------
    
    @Override
    public void writeByte(int v) throws IOException {
        long pos = getFilePointer();
        checkProtectedRegion(pos, 1);
        super.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        long pos = getFilePointer();
        checkProtectedRegion(pos, 2);
        super.writeShort(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        long pos = getFilePointer();
        checkProtectedRegion(pos, 4);
        super.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        long pos = getFilePointer();
        checkProtectedRegion(pos, 8);
        super.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        long pos = getFilePointer();
        checkProtectedRegion(pos, 4);
        super.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        long pos = getFilePointer();
        checkProtectedRegion(pos, 8);
        super.writeDouble(v);
    }

    @Override
    public void write(byte[] b) throws IOException {
        long pos = getFilePointer();
        checkProtectedRegion(pos, b.length);
        super.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        long pos = getFilePointer();
        checkProtectedRegion(pos, len);
        super.write(b, off, len);
    }

    @Override
    public void writeString(String str, int length) throws IOException {
        long pos = getFilePointer();
        // Actual write length will be exactly 'length' bytes due to padding
        checkProtectedRegion(pos, length);
        super.writeString(str, length);
    }
}