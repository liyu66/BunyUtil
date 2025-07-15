package zzx.fsb5;

import java.io.IOException;

import zzx.utils.LERandomAccessFile;

/*
 * This Chunk is primarily related to the Setup Header in Vorbis encoding.
 * Vorbis encoding includes three special packets that store audio metadata:
 * the Identification Header, Comment Header, and Setup Header.
 *
 * When a Vorbis audio stream is packed into an FSB5 file, only two pieces of
 * metadata from the Identification Header—Channels and Sample Rate—are preserved
 * (saved in the SampleMode). The Comment Header is completely discarded.
 *
 * The Setup Header contains detailed encoding information. FMOD Studio will
 * re-encode (or compress?) the Vorbis audio so that the Setup Header matches one
 * of several predefined templates. This allows FMOD Studio to discard the original
 * Setup Header and replace it with a unique 32-bit key (e.g., 0x6A5436BF) that
 * identifies which template is being used, saving significant space.
 *
 * Yes, this 32-bit key is stored in the ExtraDataChunk.
 * However, the ExtraDataChunk is often larger than 32 bits.
 * What do the remaining bits mean? I don't know.
 * (Perhaps the key size is actually larger than 32 bits?) 
 * 
 * In conclusion, this is why an Ogg Vorbis audio file cannot be directly
 * packed into an FSB5 file — its Setup Header must match one of the predefined
 * templates. If you arbitrarily assign it an ExtraDataChunk that doesn't correspond
 * to its actual encoding, the resulting audio will become nothing but noise.
 */
public class ExtraDataChunk extends Chunk {
	private final byte[] data;

	public ExtraDataChunk(ChunkMode mode, byte[] data) {
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
		sb.append("ExtraData(0x").append(Integer.toHexString(getMode().getType()))
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