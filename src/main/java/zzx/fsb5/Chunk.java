package zzx.fsb5;

import java.io.IOException;

import zzx.utils.LERandomAccessFile;

public abstract class Chunk {
	private ChunkMode mode;
	public abstract void writeTo(LERandomAccessFile dest) throws IOException;
	
	public Chunk(ChunkMode mode) {
		this.mode = mode;
	}
	
	public ChunkMode getMode() {
		return mode;
	}
	
	public void setMode(ChunkMode mode) {
		this.mode = mode;
	}
}