package zzx;

import zzx.Mod.SoundFile;
import zzx.buny.BunyStruct;
import zzx.buny.FileInside;
import zzx.fsb5.Fsb5Builder;
import zzx.fsb5.Fsb5Reader;
import zzx.fsb5.Sound;
import zzx.utils.LERandomAccessFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static zzx.Config.*;

public class SoundModifier {
	
	private BunyStruct buny;
	private File tempFile;
	
	private FileInside[] fsb5Files = new FileInside[LEVEL_COUNT];
	private Fsb5Builder[] fsb5Builders = new Fsb5Builder[LEVEL_COUNT];
	private int[] initialSizes = new int[LEVEL_COUNT];
	
	private FileInside[] floFiles = new FileInside[LEVEL_COUNT];
	private FloStruct[] floStructs = new FloStruct[LEVEL_COUNT];
	
	private boolean[] needWriteBackFsb = new boolean[LEVEL_COUNT];
	private boolean[] needWriteBackFlo = new boolean[LEVEL_COUNT];
	
	public SoundModifier(BunyStruct buny) throws IOException {
		this.buny = buny;
		this.tempFile = createTempFile();
	}
	
	public String modifyBy(SoundFile soundFile) throws IOException {
		int[] levelIndices = soundFile.getlevelIndex();
		
		int[] results = new int[levelIndices.length];
		
		for (int i = 0; i < levelIndices.length; i++) {
			
			int levelIndex = levelIndices[i];
			
			if (fsb5Builders[levelIndex] == null) {
				initFsb(levelIndex);
			}
			
			if (soundFile.isIndexBased()) {
				results[i] = replaceByIndex(fsb5Builders[levelIndex], initialSizes[levelIndex],
								soundFile.getSoundIndex(), soundFile.getSounds()[0]);
				if (results[i] == 0) {
					needWriteBackFsb[levelIndex] = true;
				}
			} else if (soundFile.isEventBased()) {
				if (floStructs[levelIndex] == null) {
					initFlo(levelIndex);
				}
				results[i] = replaceByEvent(fsb5Builders[levelIndex], floStructs[levelIndex],
								soundFile.getEventName(), soundFile.getSounds());
				if (results[i] == 0) {
					needWriteBackFlo[levelIndex] = true;
					needWriteBackFsb[levelIndex] = true;
				}
			}
		}
		
		return combine(levelIndices, results);
	}
	
	public boolean needWriteBack() {
		for (int i = 0; i < LEVEL_COUNT; i++) {
			if (needWriteBackFsb(i)) {
				return true;
			}
			if (needWriteBackFlo(i)) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean needWriteBackFsb(int levelIndex) {
		return fsb5Builders[levelIndex] != null && needWriteBackFsb[levelIndex];
	}
	
	private boolean needWriteBackFlo(int levelIndex) {
		return floStructs[levelIndex] != null && needWriteBackFlo[levelIndex];
	}
	
	public void writeBackWithPrint() throws IOException {
		for (int i = 0; i < LEVEL_COUNT; i++) {
			FloStruct flo = floStructs[i];
			if (needWriteBackFlo(i)) {
				/*
				 * Use a file to temporarily cache the .flo content,
				 * indirectly completing the data transfer.
				 */
				System.out.print("Writing the rebuilt [pc_" + LEVEL_NAMES[i] + ".flo] back to data.buny......");
				flo.writeTo(tempFile);
				floFiles[i].redirectTo(tempFile);
				clearFile(tempFile);
				System.out.println("done!");
			}
		}
		
		for (int i = 0; i < LEVEL_COUNT; i++) {
			Fsb5Builder fsb = fsb5Builders[i];
			if (needWriteBackFsb(i)) {
				/*
				 * Directly append the data to the .buny file without using a temporary intermediate file.
				 * However, manual file redirection is required.
				 */
				System.out.print("Writing the rebuilt [" + LEVEL_NAMES[i] + ".fsb] back to data.buny......");
				LERandomAccessFile raf = buny.getRaf();
				long offset = raf.length();
				raf.seek(offset);
				fsb.buildTo(raf);
				long size = raf.getFilePointer() - offset;
				fsb5Files[i].redirectTo(offset, size, size);
				System.out.println("done!");
			}
		}
	}
	
	public void removeEventBy(File removeFloFile, int levelIndex) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(removeFloFile))) {
			for (String line = reader.readLine();
				 line != null;
				 line = reader.readLine()) {
				if (floStructs[levelIndex] == null) {
					initFlo(levelIndex);
				}
				floStructs[levelIndex].removeEvent(line);
			}
		}
	}
	
	private void initFsb(int i) throws IOException {
		fsb5Files[i] = buny.getFile(FSB_NAMES[i]);
		fsb5Builders[i] = new Fsb5Builder(cast(fsb5Files[i]));
		initialSizes[i] = fsb5Builders[i].getSoundCount();
	}
	
	private void initFlo(int i) throws IOException {
		floFiles[i] = buny.getFile(FLO_NAMES[i]);
		floFiles[i].extractTo(tempFile);
		floStructs[i] = new FloStruct(tempFile);
		clearFile(tempFile);
	}
	
	private static int replaceByIndex(Fsb5Builder fsb, int maxIndex, int index, Sound sound) {
		if (sound == null) {
			return 2;	// error (total sound count < 1)
		}
		if (index < 0 || index >= maxIndex) {
			return 3;	// Index overflow
		}
		fsb.setSound(index, sound);
		return 0;	// replaced
	}
	
	private static int replaceByEvent(Fsb5Builder fsb, FloStruct flo, String eventName, Sound... sounds) {
		if (sounds.length < 1) {
			return 2;	// error (total sound count < 1)
		}
		if (!flo.containsEvent(eventName)) {
			return 1;	// skipped (event not found)
		}
		
		/*
		 * Even if the original eventName maps to a RandomEvent group bound to multiple SimpleEvents,
		 * only the first SimpleEvent and its corresponding Sound are used as a reference
		 * for the pattern configuration.
		 * (If they all belong to the same RandomEvent group, the pattern configurations of those Sounds
		 * are probably not too different, right?)
		 */
		int originalEventIndex = flo.getEventIndex(eventName)[0];
		int[] originalSimpleEvent = flo.getSimpleEvent(originalEventIndex);
		String unknownOfSound = flo.getSoundDataFile(originalSimpleEvent[0])[0];	
		
		/*
		 * Append the new Sound to the .fsb file, 
		 * then add a reference to this Sound in the .flo file, 
		 * along with a new SimpleEvent pointing to the Sound.
		 */
		int[] eventIndices = new int[sounds.length];
		for (int i = 0; i < sounds.length; i++) {
			fsb.addSound(sounds[i]);
			
			// Duplicate Sound names do not affect functionality.
			String soundName = eventName + "_" + i + ".wav";
			
			int soundIndex = flo.addSound(unknownOfSound, soundName);
			eventIndices[i] = flo.addSimpleEvent(soundIndex, originalSimpleEvent[1], originalSimpleEvent[2]);
		}
		
		if (sounds.length > 1) {
			/*
			 * If there is more than one Sound corresponding to an event,
			 * create a RandomEvent group so that when the event is triggered,
			 * one of these Sounds is played at random.
			 */
			int randomEventIndex = flo.addRandomEventGroup(eventIndices);
			flo.redirectEvent(eventName, true, randomEventIndex);
		} else {
			int simpleEventIndex = eventIndices[0];
			flo.redirectEvent(eventName, false, simpleEventIndex);
		}
		
		return 0;	// replaced
	}
	
	private static String combine(int[] levelIndices, int[] results) {
	    String[] statusNames = {"replaced", "skipped", "error", "index overflow"};

	    Map<Integer, List<String>> statusMap = new LinkedHashMap<>();

	    for (int i = 0; i < levelIndices.length; i++) {
	        int result = results[i];
	        int levelIndex = levelIndices[i];

	        String levelName = (levelIndex >= 0 && levelIndex < LEVEL_NAMES.length) 
	                ? LEVEL_NAMES[levelIndex] 
	                : "UNKNOWN";

	        statusMap.computeIfAbsent(result, k -> new ArrayList<>()).add(levelName);
	    }

	    StringBuilder sb = new StringBuilder();
	    for (int status = 0; status < statusNames.length; status++) {
	        List<String> levels = statusMap.get(status);
	        if (levels != null && !levels.isEmpty()) {
	            if (sb.length() > 0) sb.append(";  ");
	            sb.append(statusNames[status])
	              .append("(")
	              .append(String.join(", ", levels))
	              .append(")");
	        }
	    }

	    return sb.toString();
	}


	
	private static File createTempFile() throws IOException {
		File tempFile = File.createTempFile("modLoader-", ".tmp");
		tempFile.deleteOnExit();
		return tempFile;
	}
	
	private static void clearFile(File file) throws IOException {
        try (LERandomAccessFile raf = new LERandomAccessFile(file, "rw")) {
            raf.setLength(0);
        }
    }
	
	private static Fsb5Reader cast(FileInside file) throws IOException {
	    /*
	     * .fsb files within .buny archives are stored uncompressed (not zstd-compressed), 
	     * allowing direct creation of a file slice/window from the .buny container. 
	     * This slice can be parsed as a standalone .fsb file while sharing the same underlying 
	     * file channel. This approach provides efficient zero-copy access without requiring 
	     * explicit resource management (closure is handled by the parent container).
	     */
	    return new Fsb5Reader(file.getSlice(), false);
	}
}
