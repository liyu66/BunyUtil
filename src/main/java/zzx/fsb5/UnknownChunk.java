package zzx.fsb5;

import java.io.IOException;

import zzx.utils.LERandomAccessFile;

public class UnknownChunk extends Chunk {
	private final byte[] data;

	public UnknownChunk(ChunkMode mode, byte[] data) {
		super(mode);
		this.data = data;
	}
	
	public byte[] getData() {
		return data;
	}

	public int getSize() {
		return getMode().getSize();
	}
	
	@Override
	public void writeTo(LERandomAccessFile dest) throws IOException {
	    dest.writeInt(getMode().get());
	    dest.write(data);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Unknown(0x").append(Integer.toHexString(getMode().getType()))
		  .append(", " + getSize() + " bytes")
		  .append("): [");

		for (int i = 0; i < data.length; i += 4) {
			int value = 0;
			int bytesToRead = Math.min(4, data.length - i);
			for (int j = 0; j < bytesToRead; j++) {
				value |= (data[i + j] & 0xFF) << ((3 - j) * 8);
			}
			sb.append(String.format("0x%08X", value));
			if (i + 4 < data.length) {
				sb.append(", ");
			}
		}

		sb.append("]");
		return sb.toString();
	}
}