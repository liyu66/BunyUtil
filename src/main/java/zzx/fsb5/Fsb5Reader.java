package zzx.fsb5;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import zzx.utils.LERandomAccessFile;
import zzx.utils.LERandomAccessFileSlice;

public class Fsb5Reader implements Closeable {
	
	private LERandomAccessFile raf;
	
	private String idString;
	private int version;
	private int soundCount;
	private int tableSize;
	private int nameTableSize;
	private int dataSize;
	private int codec;
	
	private String[] names;
	private SoundFromFsb[] sounds;
	
	private HashMap<String, SoundFromFsb> nameToSound;
	
	private final long dataPartOffset;

	public Fsb5Reader(String file) throws IOException {
		this(new File(file));
    }
	
	public Fsb5Reader(File file) throws IOException {
		this(new LERandomAccessFile(file, "r"), isBankFile(file));
	}
	
	public Fsb5Reader(LERandomAccessFile raf, boolean isBank) throws IOException {
		this.raf = isBank ? createFsbSlice(raf) : raf;
		
        readHeader();
        
        if (containNames()) {
        	readNames();
        }
        
        dataPartOffset = Config.HEADER_SIZE + getTableSize() + getNameTableSize();
        readSounds();
	}
	
	private static boolean isBankFile(File file) {
        return file.getName().toLowerCase().endsWith(".bank");
    }
	
	/*
     * Compared to .fsb file,
     * .bank file simply contain some event-related metadata at the beginning.
     * By skipping that part and extracting the latter section,
     * it can be converted into a valid .fsb file.
     */
	private static LERandomAccessFile createFsbSlice(LERandomAccessFile bank) throws IOException {
		long fsbStart = findFsbMagic(bank);

        // Extract all remaining bytes starting from the magic number "FSB5"
        return new LERandomAccessFileSlice(bank, fsbStart, bank.length() - fsbStart, true);
	}
    
	private static long findFsbMagic(LERandomAccessFile bank) throws IOException {
	    final int ALIGNMENT = 0x20;
	    long position = 0;
	    
	    while (position <= bank.length() - 4) {
	        bank.seek(position);
	        
	        int magic = bank.readInt();
	        if (magic == 0x35425346) {  // "FSB5"
	            return position;
	        }
	        
	        position += ALIGNMENT;	// Interval 0x20 bytes each time
	    }
	    
	    long fallbackPos = Math.max(0, bank.length() - 4);
	    bank.seek(fallbackPos);
	    if (bank.readInt() == 0x35425346) {
	        return fallbackPos;
	    }
	    
	    throw new IOException("FSB5 magic number not found in .bank file");
	}

    private void readHeader() throws IOException {
    	// 0x00 ~ 0x03: a magic string, its value should be "FSB5"
        raf.seek(0x00);
        byte[] magic = new byte[4];
        raf.readFully(magic);
        idString = new String(magic, StandardCharsets.US_ASCII).trim();
        
        if (!idString.equals("FSB5")) {
        	throw new IOException("Illegal magic numbers: " + idString);
        }
        
        // 0x04 ~ 0x1B: header info
        version = raf.readInt();		// always 1
        soundCount = raf.readInt();
        tableSize = raf.readInt();
        nameTableSize = raf.readInt();	// always 0, the sound name is stored separately in .flo file
        dataSize = raf.readInt();
        codec = raf.readInt();			// always 0x0F, indicating that its internal audio is Vorbis encoded
        
        if (version != 1) {
        	/*
        	 * If the version is 1, the header size is 0x3C bytes; otherwise, it is 0x40 bytes.
        	 * Since all the fsb5 files I’ve encountered so far seem to have version 1,
        	 * I took a shortcut and didn’t implement any logic to handle this minor difference :/
        	 */
        	throw new IOException(
        			"Currently, only version 1 of .fsb files is supported."
        			+ "Sorry for the inconvenience.");
        }
        
        /*
         * 1.
         * 0x1C ~ 0x23: unknown
         * 0x24 ~ 0x33: 128-bit hash
         * 0x34 ~ 0x3B: maybe 64-bit sub-hash
         * 
         * 2.
         * 0x1C ~ 0x2B: unknown (all zero)
         * 0x2C ~ 0x3B: 128-bit hash (or two 64-bit hash)
         * 
         * 3.
         * 0x1C ~ 0x3B: unknown
         * 
         * PS: No matter what this part is, it seems to have no effect, just ignore it.
         */
        
        /*
         * [0x3C] ~ [0x3C + tableSize]: Table (A table that stores all sound metadata)
         */
        
        /*
         * [0x3C + tableSize] ~ [dataPartOffset]: Name Table (A table that stores all sound names)
         */
        
        /*
         * [dataPartOffset] ~ [raf.length()]: All sound data packets
         */
    }
    
    private void readNames() throws IOException {
    	names = new String[getSoundCount()];
    	
    	long nameTableStart = Config.HEADER_SIZE + getTableSize();
    	long[] nameOffsets = new long[getSoundCount()];
    	
    	raf.seek(nameTableStart);
    	for (int i = 0; i < getSoundCount(); i++) {
    		nameOffsets[i] = nameTableStart + raf.readInt();
    	}
    	
    	for (int i = 0; i < getSoundCount(); i++) {
    		raf.seek(nameOffsets[i]);
    		names[i] = raf.readString();
    	}
    }
    
    private void readSounds() throws IOException {
    	sounds = new SoundFromFsb[getSoundCount()];
    	
    	long offset = Config.HEADER_SIZE;
    	for (int i = 0; i < getSoundCount(); i++) {
    		SoundFromFsb sound = new SoundFromFsb(this, i, offset, containNames() ? names[i] : null);
    		offset = offset + sound.getSizeInTable();
    		sounds[i] = sound;
    	}
    	
    	for (SoundFromFsb sound : sounds) {
    		sound.init();
    	}
    	
    	if (containNames()) {
    		nameToSound = new HashMap<>();
    		for (SoundFromFsb sound : sounds) {
        		nameToSound.put(sound.getName(), sound);
        	}
    	}
    }
    
    public SoundFromFsb[] getAllSounds() {
    	return sounds;
    }
    
    public SoundFromFsb getSound(int i) {
    	return sounds[i];
    }
    
    public SoundFromFsb getSound(String name) {
    	return nameToSound.get(name);
    }
    
    public String getIdString() {
		return idString;
	}

	public int getVersion() {
		return version;
	}

	public int getSoundCount() {
		return soundCount;
	}

	public int getTableSize() {
		return tableSize;
	}

	public int getNameTableSize() {
		return nameTableSize;
	}

	public int getDataSize() {
		return dataSize;
	}

	public int getCodec() {
		return codec;
	}
	
	public long getDataPartOffset() {
		return dataPartOffset;
	}
	
	public boolean containNames() {
		return getNameTableSize() > 0;
	}
	
	LERandomAccessFile getRaf() {
		return raf;
	}
    
    @Override
    public void close() throws IOException {
        if (raf != null) {
            raf.close();
        }
    }
}




