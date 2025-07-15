package zzx.fsb5;

import java.util.HashMap;
import java.util.Map;

class Config {
	static final long HEADER_SIZE = 0x3C;
	
    static final int DEFAULT_VERSION = 0x01;
    static final int DEFAULT_NAME_TABLE_SIZE = 0;
    static final int DEFAULT_CODEC = 0x0F;
    
    // Header[0x1C ~ 0x3B]: unknown 32 bytes
    static final byte[] DEFAULT_UNKNOWN_BYTES =
    	{0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
    	 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    
    
    static final int[] CHANNELS_MAP = {1, 2, 6, 8};
    static final int[] SAMPLE_RATE_MAP = {4000, 8000, 11000, 11025, 16000, 22050, 24000, 32000, 44100, 48000, 96000};
    
    static final Map<Integer, Integer> REVERSED_CHANNELS_MAP = new HashMap<>();
    static final Map<Integer, Integer> REVERSED_SAMPLE_RATE_MAP = new HashMap<>();
    
    static {
        for (int i = 0; i < CHANNELS_MAP.length; i++) {
            REVERSED_CHANNELS_MAP.put(CHANNELS_MAP[i], i);
        }
        
        for (int i = 0; i < SAMPLE_RATE_MAP.length; i++) {
            REVERSED_SAMPLE_RATE_MAP.put(SAMPLE_RATE_MAP[i], i);
        }
    }
    
    static int getBits(int value, int start, int end) {
	    int length = end - start + 1;
	    int mask = (1 << length) - 1;
	    return (value >> start) & mask;
	}
    
    static long getBits(long value, int start, int end) {
	    int length = end - start + 1;
	    long mask = (1L << length) - 1;
	    return (value >> start) & mask;
	}
    
    static long setBits(long original, int start, int end, long value) {
        int length = end - start + 1;
        long mask = (1L << length) - 1;
        long shiftedMask = mask << start;
        
        long cleared = original & ~shiftedMask;
        long shiftedValue = (value & mask) << start;
        return cleared | shiftedValue;
    }
    
    static int setBits(int original, int start, int end, int value) {
        int length = end - start + 1;
        int mask = (1 << length) - 1;
        int shiftedMask = mask << start;
        
        int cleared = original & ~shiftedMask;
        int shiftedValue = (value & mask) << start;
        return cleared | shiftedValue;
    }
}