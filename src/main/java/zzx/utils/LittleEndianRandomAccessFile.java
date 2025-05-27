package zzx.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public class LittleEndianRandomAccessFile {

    private final RandomAccessFile raf;

    public LittleEndianRandomAccessFile(File file, String mode) throws FileNotFoundException {
        this.raf = new RandomAccessFile(file, mode);
    }

    public short readShort() throws IOException {
        int b1 = raf.read();
        int b2 = raf.read();
        if ((b1 | b2) < 0) throw new IOException("Unexpected EOF");
        return (short) ((b2 << 8) | b1);
    }

    public int readUnsignedShort() throws IOException {
        int b1 = raf.read();
        int b2 = raf.read();
        if ((b1 | b2) < 0) throw new IOException("Unexpected EOF");
        return (b2 << 8) | b1;
    }

    public int readInt() throws IOException {
        int b1 = raf.read();
        int b2 = raf.read();
        int b3 = raf.read();
        int b4 = raf.read();
        if ((b1 | b2 | b3 | b4) < 0) throw new IOException("Unexpected EOF");
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }

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
                b1);
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public void readFully(byte[] buffer) throws IOException {
        raf.readFully(buffer);
    }

    public void seek(long pos) throws IOException {
        raf.seek(pos);
    }
    
    public int skipBytes(int n) throws IOException {
    	return raf.skipBytes(n);
    }

    public long getFilePointer() throws IOException {
        return raf.getFilePointer();
    }

    public long length() throws IOException {
        return raf.length();
    }

    public void close() throws IOException {
        raf.close();
    }

    public int readUnsignedByte() throws IOException {
        int b = raf.read();
        if (b < 0) throw new IOException("Unexpected EOF");
        return b;
    }

    public byte readByte() throws IOException {
        int b = raf.read();
        if (b < 0) throw new IOException("Unexpected EOF");
        return (byte) b;
    }

    public String readString(int length) throws IOException {
        byte[] buf = new byte[length];
        raf.readFully(buf);
        return new String(buf, "UTF-8").trim(); // 修改为你需要的编码
    }
    
    public void writeByte(int v) throws IOException {
        raf.write(v);
    }

    public void writeShort(int v) throws IOException {
        raf.write(v & 0xFF);
        raf.write((v >>> 8) & 0xFF);
    }

    public void writeInt(int v) throws IOException {
        raf.write(v & 0xFF);
        raf.write((v >>> 8) & 0xFF);
        raf.write((v >>> 16) & 0xFF);
        raf.write((v >>> 24) & 0xFF);
    }

    public void writeLong(long v) throws IOException {
        raf.write((int)(v & 0xFF));
        raf.write((int)((v >>> 8) & 0xFF));
        raf.write((int)((v >>> 16) & 0xFF));
        raf.write((int)((v >>> 24) & 0xFF));
        raf.write((int)((v >>> 32) & 0xFF));
        raf.write((int)((v >>> 40) & 0xFF));
        raf.write((int)((v >>> 48) & 0xFF));
        raf.write((int)((v >>> 56) & 0xFF));
    }

    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    public void write(byte[] buffer) throws IOException {
        raf.write(buffer);
    }
    
    public void write(byte[] b, int off, int len) throws IOException {
    	raf.write(b, off, len);
    }

    public void writeString(String str, int length) throws IOException {
        byte[] bytes = str.getBytes("UTF-8");
        if (bytes.length > length) {
            raf.write(bytes, 0, length); // truncate
        } else {
            raf.write(bytes);
            // pad with zeros
            for (int i = bytes.length; i < length; i++) {
                raf.write(0);
            }
        }
    }
    
    public void setLength(long newLength) throws IOException {
    	raf.setLength(newLength);
    }

    public FileChannel getChannel() {
    	return raf.getChannel();
    }
}

