package zzx.fsb5;

import static zzx.fsb5.Config.*;

public class SampleMode {
	
	private final long sampleMode;
	
	public SampleMode() {
		this(0);
	}
	
	public SampleMode(long sampleMode) {
		this.sampleMode = sampleMode;
	}
	
	public long get() {
		return sampleMode;
	}
	
	public boolean hasChunk() {
    	return getBits(sampleMode, 0, 0) == 1;
    }
    
    public int getSampleRate() {
		return SAMPLE_RATE_MAP[(int) getBits(sampleMode, 1, 4)];
	}
    
    public int getChannels() {
		return CHANNELS_MAP[(int) getBits(sampleMode, 5, 6)];
	}
    
    public long getDataOffset() {
    	return getBits(sampleMode, 6, 33) * 16;
    }
    
    public int getSampleNum() {
		return (int) getBits(sampleMode, 34, 63);
	}
    
    public SampleMode setHasChunk(boolean hasChunk) {
        return new SampleMode(setBits(sampleMode, 0, 0, hasChunk ? 1 : 0));
    }
    
    public SampleMode setSampleRate( int sampleRate) {
        Integer index = REVERSED_SAMPLE_RATE_MAP.get(sampleRate);
        if (index == null) {
            throw new IllegalArgumentException("Invalid Sample Rate: " + sampleRate + 
                ", Valid value: " + java.util.Arrays.toString(SAMPLE_RATE_MAP));
        }
        return new SampleMode(setBits(sampleMode, 1, 4, index));
    }
    
    public SampleMode setChannels(int channels) {
        Integer index = REVERSED_CHANNELS_MAP.get(channels);
        if (index == null) {
            throw new IllegalArgumentException("Invalid Channels: " + channels + 
                ", Valid value: " + java.util.Arrays.toString(CHANNELS_MAP));
        }
        return new SampleMode(setBits(sampleMode, 5, 6, index));
    }
    
    public SampleMode setDataOffset(long dataOffset) {
        long adjustedOffset = dataOffset / 16;
        return new SampleMode(setBits(sampleMode, 6, 33, adjustedOffset));
    }
    
    public SampleMode setSampleNum(long sampleNum) {
    	return new SampleMode(setBits(sampleMode, 34, 63, sampleNum));
    }
}
