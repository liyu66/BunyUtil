package zzx.buny;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

    private List<Item> items = new ArrayList<>();

    public TocBackup(InputStream in) {
        this.in = new DataInputStream(new BufferedInputStream(in));
    }
    
    public Item get(int index) {
    	try {
    	    while (items.size() <= index) {
    	        items.add(new Item(in.readLong(), in.readLong(), in.readLong()));
    	    }
    	    return items.get(index);
    	} catch (EOFException e) {
    	    throw new IndexOutOfBoundsException("No item at index " + index);
    	} catch (IOException e) {
    	    throw new RuntimeException("Failed to read TOC item at index " + index, e);
    	}
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
