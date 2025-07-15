package zzx.fsb5;

import java.io.IOException;
import java.util.List;

import zzx.utils.LERandomAccessFile;

public interface Sound {
	
	SampleMode getSampleMode();
	
	List<Chunk> getAllChunks();
	
	long getDataSize();
	
	String getName();
	
	void writeDataTo(LERandomAccessFile raf) throws IOException;
	
	default int getSampleNum() {
		return getSampleMode().getSampleNum();
	}
	
	default long getDataOffset() {
		return getSampleMode().getDataOffset();
	}
	
	default int getChannels() {
		return getSampleMode().getChannels();
	}
	
	default int getSampleRate() {
		return getSampleMode().getSampleRate();
	}
	
	default float getDurationSeconds() {
		return ((float) getSampleNum()) / getSampleRate();
	}
	
	default boolean hasChunk() {
		return getSampleMode().hasChunk();
	}
	
	default boolean hasName() {
		return getName() != null;
	}
}
