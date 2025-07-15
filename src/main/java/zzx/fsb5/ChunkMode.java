package zzx.fsb5;

import static zzx.fsb5.Config.*;

public class ChunkMode {
	
	private final int chunkMode;
	
	public ChunkMode(int chunkMode) {
		this.chunkMode = chunkMode;
	}
	
	public int get() {
		return chunkMode;
	}

	public boolean isLast() {
    	return getBits(chunkMode, 0, 0) == 0;
    }
    
    public int getSize() {
    	return getBits(chunkMode, 1, 24);
    }
    
    public int getType() {
    	return getBits(chunkMode, 25, 31);
    }
    
    public ChunkMode setIsLast(boolean isLastChunk) {
        return new ChunkMode(setBits(chunkMode, 0, 0, isLastChunk ? 0 : 1));
    } 
    
    public ChunkMode setSize(int chunkSize) {
    	return new ChunkMode(setBits(chunkMode, 1, 24, chunkSize));
    }
    
    public ChunkMode setType(int chunkType) {
    	return new ChunkMode(setBits(chunkMode, 25, 31, chunkType));
    }
}
