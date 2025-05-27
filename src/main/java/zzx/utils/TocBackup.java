package zzx.utils;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class TocBackup implements Closeable {

    public static class Item {
        public final long offset;
        public final long zsize;
        public final long size;
        public Item(long offset, long zsize, long size) {
            this.offset = offset;
            this.zsize = zsize;
            this.size = size;
        }
    }

    private final DataInputStream in;
    private int currentIndex = 0;

    public TocBackup(InputStream in) {
        this.in = new DataInputStream(new BufferedInputStream(in));
    }

    public void skip(int itemsToSkip) {
        if (itemsToSkip < 0) {
            throw new UnsupportedOperationException("Backward skipping is not supported");
        }

        try {
            long bytesToSkip = (long) itemsToSkip * 24;
            long skipped = 0;
            while (skipped < bytesToSkip) {
                long s = in.skip(bytesToSkip - skipped);
                if (s <= 0) throw new EOFException("Failed to skip " + itemsToSkip + " items");
                skipped += s;
            }
            currentIndex += itemsToSkip;
        } catch (IOException e) {
            throw new RuntimeException("Failed to skip items", e);
        }
    }

    public Item get() {
        try {
            long offset = in.readLong();
            long zsize = in.readLong();
            long size = in.readLong();
            currentIndex++;
            return new Item(offset, zsize, size);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read item at index " + currentIndex, e);
        }
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
