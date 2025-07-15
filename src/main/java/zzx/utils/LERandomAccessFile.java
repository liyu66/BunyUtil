package zzx.utils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

/**
 * A {@link RandomAccessFile} variant specifically designed for little-endian file access.
 * Automatically converts between Java's native big-endian format and little-endian format
 * when reading/writing primitive data types. Byte array operations remain direct with no conversion.
 * <p>
 * Key features:
 * <ul>
 *   <li>Read methods convert little-endian file data to Java's big-endian primitives</li>
 *   <li>Write methods convert Java's big-endian primitives to little-endian file format</li>
 *   <li>Provides efficient file-to-file data transfer via {@link #transferTo} and {@link #transferFrom}</li>
 *   <li>Byte array operations ({@code readFully}/{@code write}) perform raw I/O with no conversion</li>
 * </ul>
 * 
 * <b>Important:</b> Always use {@link #seek(long)} to position file pointers before operations.
 */
public class LERandomAccessFile implements Closeable {

    private final RandomAccessFile raf;

    /**
     * Creates a random access file stream to read from, and optionally write to, 
     * the specified file in little-endian format.
     * 
     * @param file the target file object
     * @param mode the access mode ("r", "rw", etc.)
     * @throws FileNotFoundException if the file doesn't exist or cannot be opened
     */
    public LERandomAccessFile(File file, String mode) throws FileNotFoundException {
        this.raf = new RandomAccessFile(file, mode);
    }
    
    /**
     * Creates a random access file stream to read from, and optionally write to,
     * the specified file by name in little-endian format.
     * 
     * @param name the system-dependent file name
     * @param mode the access mode ("r", "rw", etc.)
     * @throws FileNotFoundException if the file doesn't exist or cannot be opened
     */
    public LERandomAccessFile(String name, String mode) throws FileNotFoundException {
        this.raf = new RandomAccessFile(name, mode);
    }
    
    /**
     * Creates a LERandomAccessFile from an existing RandomAccessFile.
     * This is intended for advanced use cases, such as creating a slice of a file.
     * 
     * @param raf the underlying RandomAccessFile
     */
    protected LERandomAccessFile(RandomAccessFile raf) {
        this.raf = raf;
    }
    
    /**
     * Returns the underlying RandomAccessFile for advanced operations.
     * Use with caution, as it bypasses the little-endian conversions and boundaries of any slice.
     * 
     * @return the underlying RandomAccessFile
     */
    protected RandomAccessFile getRaf() {
        return raf;
    }
    
    /**
     * Sets the file pointer position for the next read/write operation.
     * 
     * @param pos the absolute byte offset from file start
     * @throws IOException if an I/O error occurs
     */
    public void seek(long pos) throws IOException {
        raf.seek(pos);
    }
    
    /**
     * Skips exactly {@code n} bytes in the file stream.
     * 
     * @param n number of bytes to skip
     * @return actual number of bytes skipped (always equals n)
     * @throws IOException if an I/O error occurs
     */
    public int skipBytes(int n) throws IOException {
        return raf.skipBytes(n);
    }

    /**
     * Gets the current byte offset in this file.
     * 
     * @return the current file pointer position
     * @throws IOException if an I/O error occurs
     */
    public long getFilePointer() throws IOException {
        return raf.getFilePointer();
    }

    /**
     * Returns the current file length.
     * 
     * @return the file length in bytes
     * @throws IOException if an I/O error occurs
     */
    public long length() throws IOException {
        return raf.length();
    }
    
    /**
     * Sets the file length. Can extend or truncate the file.
     * 
     * @param newLength the desired file length
     * @throws IOException if an I/O error occurs
     */
    public void setLength(long newLength) throws IOException {
        raf.setLength(newLength);
    }
    
    /**
     * Returns the underlying FileChannel for advanced operations.
     * 
     * @return the file channel associated with this stream
     */
    protected FileChannel getChannel() {
        return raf.getChannel();
    }
    
    /**
     * Closes the file stream and releases resources.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        raf.close();
    }

    //--------------------------------------------------
    // Little-Endian Read Methods (LE -> Java BE)
    //--------------------------------------------------
    
    /**
     * Reads a signed 16-bit short in little-endian format.
     * 
     * @return the converted big-endian short value
     * @throws IOException if end of file is reached prematurely
     */
    public short readShort() throws IOException {
        int b1 = raf.read();
        int b2 = raf.read();
        if ((b1 | b2) < 0) throw new IOException("Unexpected EOF");
        return (short) ((b2 << 8) | b1);  // Reconstruct from little-endian
    }

    /**
     * Reads an unsigned 16-bit short in little-endian format.
     * 
     * @return the converted big-endian unsigned short value (as int)
     * @throws IOException if end of file is reached prematurely
     */
    public int readUnsignedShort() throws IOException {
        int b1 = raf.read();
        int b2 = raf.read();
        if ((b1 | b2) < 0) throw new IOException("Unexpected EOF");
        return (b2 << 8) | b1;  // Reconstruct from little-endian
    }

    /**
     * Reads a signed 32-bit integer in little-endian format.
     * 
     * @return the converted big-endian integer value
     * @throws IOException if end of file is reached prematurely
     */
 public int readInt() throws IOException {
        int b1 = raf.read();
        int b2 = raf.read();
        int b3 = raf.read();
        int b4 = raf.read();
        if ((b1 | b2 | b3 | b4) < 0) throw new IOException("Unexpected EOF");
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;  // Reconstruct LE bytes
    }

    /**
     * Reads a signed 64-bit long in little-endian format.
     * 
     * @return the converted big-endian long value
     * @throws IOException if end of file is reached prematurely
     */
    public long readLong() throws IOException {
        long b1 = raf.read();
        long b2 = raf.read();
        long b3 = raf.read();
        long b4 = raf.read();
        long b5 = raf.read();
        long b6 = raf.read();
        long b7 = raf.read();
        long b8 = raf.read();
        if ((b1 | b2 | b3 | b4 | b5 | b6 | b7 | b8) < 0) throw new IOException("Unexpected EOF");
        return ((b8 << 56) |
                (b7 << 48) |
                (b6 << 40) |
                (b5 << 32) |
                (b4 << 24) |
                (b3 << 16) |
                (b2 << 8)  |
                b1);  // Reconstruct LE bytes
    }
    
    /**
     * Reads a long value without endian conversion (uses underlying big-endian).
     * 
     * @return the unmodified big-endian long value
     * @throws IOException if an I/O error occurs
     */
    public long readOrginalLong() throws IOException {
        return raf.readLong();
    }

    /**
     * Reads a 32-bit float from little-endian bytes.
     * 
     * @return the converted float value
     * @throws IOException if an I/O error occurs
     */
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());  // Convert LE int to float
    }

    /**
     * Reads a 64-bit double from little-endian bytes.
     * 
     * @return the converted double value
     * @throws IOException if an I/O error occurs
     */
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());  // Convert LE long to double
    }

    /**
     * Reads raw bytes into buffer with NO endian conversion.
     * 
     * @param buffer target byte array
     * @throws IOException if end of file is reached before filling buffer
     */
    public void readFully(byte[] buffer) throws IOException {
        raf.readFully(buffer);
    }
    
    /**
     * Reads an unsigned 8-bit byte.
     * 
     * @return byte value in range 0-255
     * @throws IOException if end of file is reached
     */
    public int readUnsignedByte() throws IOException {
        int b = raf.read();
        if (b < 0) throw new IOException("Unexpected EOF");
        return b;
    }

    /**
     * Reads a signed 8-bit byte.
     * 
     * @return the signed byte value
     * @throws IOException if end of file is reached
     */
    public byte readByte() throws IOException {
        int b = raf.read();
        if (b < 0) throw new IOException("Unexpected EOF");
        return (byte) b;
    }

    /**
     * Reads a UTF-8 string of fixed byte length.
     * 
     * @param length byte length to read
     * @return the decoded string (with trailing whitespace trimmed)
     * @throws IOException if an I/O error occurs
     */
    public String readString(int length) throws IOException {
        byte[] buf = new byte[length];
        raf.readFully(buf);
        return new String(buf, "UTF-8").trim();
    }
    
    /**
     * Reads a null-terminated UTF-8 string.
     * 
     * The method reads bytes from the current file position until a null byte (0x00)
     * is encountered, then decodes the collected bytes as a UTF-8 string.
     * 
     * @return the decoded string (with trailing whitespace trimmed)
     * @throws IOException if an I/O error occurs
     */
    public String readString() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = raf.read()) != -1) {
            if (b == 0) {
                break;
            }
            baos.write(b);
        }
        return new String(baos.toByteArray(), "UTF-8").trim();
    }
    
    //--------------------------------------------------
    // Little-Endian Write Methods (Java BE -> LE)
    //--------------------------------------------------
    
    /**
     * Writes a single byte.
     * 
     * @param v the byte to write (lower 8 bits used)
     * @throws IOException if an I/O error occurs
     */
    public void writeByte(int v) throws IOException {
        raf.write(v);
    }

    /**
     * Writes a 16-bit short in little-endian format.
     * 
     * @param v the big-endian short value to convert
     * @throws IOException if an I/O error occurs
     */
    public void writeShort(int v) throws IOException {
        raf.write(v & 0xFF);          // Write LSB first
        raf.write((v >>> 8) & 0xFF);  // Write MSB last
    }

    /**
     * Writes a 32-bit integer in little-endian format.
     * 
     * @param v the big-endian integer value to convert
     * @throws IOException if an I/O error occurs
     */
    public void writeInt(int v) throws IOException {
        // Write bytes in reverse order (LSB to MSB)
        raf.write(v & 0xFF);
        raf.write((v >>> 8) & 0xFF);
        raf.write((v >>> 16) & 0xFF);
        raf.write((v >>> 24) & 0xFF);
    }

    /**
     * Writes a 64-bit long in little-endian format.
     * 
     * @param v the big-endian long value to convert
     * @throws IOException if an I/O error occurs
     */
    public void writeLong(long v) throws IOException {
        // Write bytes in reverse order (LSB to MSB)
        raf.write((int)(v & 0xFF));
        raf.write((int)((v >>> 8) & 0xFF));
        raf.write((int)((v >>> 16) & 0xFF));
        raf.write((int)((v >>> 24) & 0xFF));
        raf.write((int)((v >>> 32) & 0xFF));
        raf.write((int)((v >>> 40) & 0xFF));
        raf.write((int)((v >>> 48) & 0xFF));
        raf.write((int)((v >>> 56) & 0xFF));
    }

    /**
     * Writes a 32-bit float in little-endian format.
     * 
     * @param v the float value to convert
     * @throws IOException if an I/O error occurs
     */
    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));  // Convert to LE int representation
    }

    /**
     * Writes a 64-bit double in little-endian format.
     * 
     * @param v the double value to convert
     * @throws IOException if an I/O error occurs
     */
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));  // Convert to LE long representation
    }

    /**
     * Writes raw bytes with NO endian conversion.
     * 
     * @param buffer the bytes to write
     * @throws IOException if an I/O error occurs
     */
    public void write(byte[] buffer) throws IOException {
        raf.write(buffer);
    }
    
    /**
     * Writes a portion of a byte array with NO endian conversion.
     * 
     * @param b source byte array
     * @param off starting offset in array
     * @param len number of bytes to write
     * @throws IOException if an I/O error occurs
     */
    public void write(byte[] b, int off, int len) throws IOException {
        raf.write(b, off, len);
    }

    /**
     * Writes a UTF-8 string as fixed-length bytes (null-padded).
     * 
     * @param str the string to write
     * @param length fixed byte length in file
     * @throws IOException if an I/O error occurs
     */
    public void writeString(String str, int length) throws IOException {
        byte[] bytes = str.getBytes("UTF-8");
        if (bytes.length > length) {
            raf.write(bytes, 0, length);  // Truncate if too long
        } else {
            raf.write(bytes);
            // Pad with zeros
            for (int i = bytes.length; i < length; i++) {
                raf.write(0);
            }
        }
    }
    
    //--------------------------------------------------
    // Special Transfer Method
    //--------------------------------------------------
    
    /**
     * Transfers bytes from this file to the specified destination file.
     * Uses NIO's zero-copy transfer for optimal performance. This method
     * transfers data directly between underlying file channels without
     * affecting the current file pointers of either file.
     * <p>
     * <b>Behavior details:</b>
     * <ul>
     *   <li>Does not modify this file's pointer (source)</li>
     *   <li>May modify destination file's pointer depending on its channel implementation</li>
     *   <li>Uses absolute source position ({@code srcOffset}) for reading</li>
     *   <li>Uses destination channel's current position for writing</li>
     * </ul>
     * 
     * @param dest   the destination file to write to
     * @param srcOffset the starting offset in this file
     * @param len    the number of bytes to transfer
     * @throws IOException if source region is invalid, destination is not writable,
     *                     or transfer fails
     */
    public void transferTo(LERandomAccessFile dest, long srcOffset, long len) throws IOException {
        if (len == 0) return;

        // Validate source region
        if (srcOffset + len > this.length()) {
            throw new IOException("Source file doesn't have enough data");
        }

        FileChannel srcChannel = this.getChannel();
        FileChannel destChannel = dest.getChannel();
        
        long transferred = 0;
        while (transferred < len) {
            long count = srcChannel.transferTo(
                srcOffset + transferred, 
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

    /**
     * Transfers bytes from the specified source file into this file.
     * Uses NIO's zero-copy transfer for optimal performance. This method
     * transfers data directly from the source channel without affecting
     * the current file pointer of this file.
     * <p>
     * <b>Behavior details:</b>
     * <ul>
     *   <li>Uses source channel's current position for reading</li>
     *   <li>May modify source file's pointer depending on its channel implementation</li>
     *   <li>Does not modify this file's pointer (destination)</li>
     *   <li>Uses absolute destination position ({@code destOffset}) for writing</li>
     * </ul>
     * 
     * @param src        the source file to read from
     * @param destOffset the starting offset in this file
     * @param len        the number of bytes to transfer
     * @throws IOException if source has insufficient data, this file is not writable,
     *                     or transfer fails
     */
    public void transferFrom(LERandomAccessFile src, long destOffset, long len) throws IOException {
        if (len == 0) return;

        // Validate source data availability
        long srcAvailable = src.length() - src.getFilePointer();
        if (srcAvailable < len) {
            throw new IOException("Source file doesn't have enough data");
        }

        FileChannel srcChannel = src.getChannel();
        FileChannel destChannel = this.getChannel();
        
        long transferred = 0;
        while (transferred < len) {
            long count = destChannel.transferFrom(
                srcChannel, 
                destOffset + transferred, 
                len - transferred
            );
            
            if (count <= 0) {
                throw new IOException("Transfer failed at position: " + 
                                    (src.getFilePointer() + transferred));
            }
            transferred += count;
        }
    }
}