package zzx.fsb5;

import java.io.IOException;

import zzx.utils.LERandomAccessFile;

public class LoopInfoChunk extends Chunk {
	private final int loopStart;
	private final int loopEnd;

	public LoopInfoChunk(ChunkMode mode, int loopStart, int loopEnd) {
		super(mode);
		this.loopStart = loopStart;
		this.loopEnd = loopEnd;
	}
	
	public int getLoopStart() {
		return loopStart;
	}

	public int getLoopEnd() {
		return loopEnd;
	}
	
	@Override
	public void writeTo(LERandomAccessFile dest) throws IOException {
	    dest.writeInt(getMode().get());
	    dest.writeInt(loopStart);
	    dest.writeInt(loopEnd);
	}
	
	@Override
	public String toString() {
		return "loopInfo: [Start = " + loopStart + ", End = " + loopEnd + "]";
	}
}