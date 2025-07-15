package zzx;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import zzx.fsb5.Fsb5Reader;
import zzx.fsb5.SoundFromFsb;
import zzx.utils.FileTree;

import static zzx.Config.*;

public class Mod implements Closeable {
	
	public class SoundFile implements Closeable {
		private final int[] levelIndex;
		
		private final Integer soundIndex;
		private final String eventName;
		
		private File[] files;
		private Fsb5Reader[] soundContainers;
		
		private SoundFile(int levelIndex, Integer soundIndex, File file) {
			this.levelIndex = new int[] {levelIndex};
			this.soundIndex = soundIndex;
			this.eventName = null;
			this.files = new File[]{file};
		}
		
		private SoundFile(int levelIndex, String eventName, File... file) {
			this(new int[] {levelIndex}, eventName, file);
		}
		
		private SoundFile(int[] levelIndex, String eventName, File... file) {
			this.levelIndex = levelIndex;
			this.soundIndex = null;
			this.eventName = eventName;
			this.files = file;
		}
		
		public boolean allowsMultipleSounds() {
		    return isEventBased();
		}
		
		public boolean isEventBased() {
		    return eventName != null;
		}

		public boolean isIndexBased() {
		    return soundIndex != null;
		}

		public int[] getlevelIndex() {
			return levelIndex;
		}
		
		public int getSoundIndex() {
			return soundIndex;
		}
		
		public String getEventName() {
			return eventName;
		}

		public SoundFromFsb[] getSounds() throws IOException {
			if (soundContainers == null) {
				soundContainers = new Fsb5Reader[files.length];
				for (int i = 0; i < files.length; i++) {
					soundContainers[i] = new Fsb5Reader(files[i]);
				}
			}
			
			List<SoundFromFsb> sounds = new ArrayList<>();
			
			for (Fsb5Reader container : soundContainers) {
				for (SoundFromFsb sound : container.getAllSounds()) {
					sounds.add(sound);
				}
			}
			
			if (allowsMultipleSounds()) {
				return sounds.toArray(new SoundFromFsb[0]);
			} else {
				return new SoundFromFsb[] {sounds.size() < 1 ? null : sounds.getFirst()};
			}
		}

		@Override
		public void close() throws IOException {
			if (soundContainers != null) {
				for (Fsb5Reader container : soundContainers) {
					if (container != null) {
						container.close();
					}
				}
				soundContainers = null;
			}
		}
	}

    private File modFolder;
    private FileTree<File> dataFiles;
    private FileTree<File> data1Files;
    
    private List<SoundFile> soundFiles;
    private File[] removeFloFile;
    
    public Mod(File modFolder) throws IOException {
    	this.modFolder = modFolder;
    	
    	File dataFolder = new File(modFolder, "data");
    	File data1Folder = new File(modFolder, "data1");
    	
        // Collect files separately from "modFolder/data" and "modFolder/data1"
        this.dataFiles = new FileTree<File>(dataFolder, f -> f);
        this.data1Files = new FileTree<File>(data1Folder, f -> f);;

        filterOutSoundFiles();
    }
    
    public String getName() {
    	return modFolder.getName();
    }
    
    public FileTree<File> getDataFiles() {
    	return dataFiles;
    }
    
    public FileTree<File> getData1Files() {
    	return data1Files;
    }
    
    public int getSoundFileCount() {
    	return soundFiles.size();
    }
    
    public SoundFile getSoundFile(int i) {
    	return soundFiles.get(i);
    }
    
    public List<SoundFile> getAllSoundFiles() {
    	return soundFiles;
    }
    
    public File getRemoveFloFile(int levelIndex) {
    	return removeFloFile[levelIndex];
    }
    
    private void filterOutSoundFiles() {
    	this.soundFiles = new ArrayList<>();
    	this.removeFloFile = new File[LEVEL_COUNT];
    	
    	String soundDir = "data/sound/";
    	
    	for (int levelIndex = 0; levelIndex < LEVEL_COUNT; levelIndex++) {
    		
    		// data/sound/[level]/
    		String levelDir = soundDir + LEVEL_NAMES[levelIndex] + "/";
        	for (Entry<String, File> entry : dataFiles.getAllFile(levelDir)) {
        		String name = entry.getKey();
        		File file = entry.getValue();
        		
        		if (!isSoundFile(name)) {
        			if (isRemoveFloFile(name)) {
        				// data/sound/[level]/remove.flo
        				removeFloFile[levelIndex] = file;
        			}
        			
        			continue;
        		}
        		
        		Integer soundIndex = getSoundIndex(name);
        		
        		if (soundIndex != null) {
        			// data/sound/[level]/[index].fsb
        			soundFiles.add(new SoundFile(levelIndex, soundIndex, file));
        		} else {
        			// data/sound/[level]/[event].fsb
        			soundFiles.add(new SoundFile(levelIndex, getEventName(name, false), file));
        		}
        		
        		dataFiles.remove(levelDir + name);
            }
        	
        	// data/sound/[level]/[event]/*.fsb
        	for (String eventDir : dataFiles.getAllSubDir(levelDir)) {
        		List<File> files = new ArrayList<>();
        		String eventName = getEventName(eventDir, true);
        		eventDir = levelDir + eventDir + "/";
        		for (Entry<String, File> entry : dataFiles.getAllFile(eventDir)) {
        			if (isSoundFile(entry.getKey())) {
						files.add(entry.getValue());
						dataFiles.remove(eventDir + entry.getKey());
        			}
        		}
        		if (files.size() > 0) {
        			soundFiles.add(new SoundFile(levelIndex, eventName, files.toArray(new File[0])));
        		}
        	}
        }
    	
    	// data/sound/[event].fsb
    	for (Entry<String, File> entry : dataFiles.getAllFile(soundDir)) {
    		String name = entry.getKey();
    		File file = entry.getValue();
    		
    		if (!isSoundFile(name)) {
    			continue;
    		}
    		
    		soundFiles.add(new SoundFile(LEVEL_INDICES, getEventName(name, false), file));
    		
    		dataFiles.remove(soundDir + name);
    	}
    	
    	// data/sound/[event]/*.fsb
    	for (String eventDir : dataFiles.getAllSubDir(soundDir)) {
    		if (isLevelName(eventDir)) {
    			continue;
    		}
    		
    		List<File> files = new ArrayList<>();
    		String eventName = getEventName(eventDir, true);
    		eventDir = soundDir + eventDir + "/";
    		for (Entry<String, File> entry : dataFiles.getAllFile(eventDir)) {
    			if (isSoundFile(entry.getKey())) {
					files.add(entry.getValue());
					dataFiles.remove(eventDir + entry.getKey());
    			};
    		}
    		if (files.size() > 0) {
    			soundFiles.add(new SoundFile(LEVEL_INDICES, eventName, files.toArray(new File[0])));
    		}
    	}
    }

    private boolean isSoundFile(String fileName) {
    	return fileName.endsWith(".fsb") || fileName.endsWith(".bank");
    }
    
    private boolean isRemoveFloFile(String fileName) {
    	return "remove.flo".equals(fileName);
    }
    
    private Integer getSoundIndex(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');

        // example: 475.fsb / 475.bank
        String idPart = fileName.substring(0, dotIndex);
        
        try {
            return Integer.parseInt(idPart);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private String getEventName(String name, boolean isDir) {
    	if (!isDir) {
    		// example: Jetpack_WeaponFlightLoop.fsb / Jetpack_WeaponFlightLoop.bank
    		int dotIndex = name.lastIndexOf('.');
    		name = name.substring(0, dotIndex);
    	}
    	
    	return name.replaceFirst("_", "*");
    }

	@Override
	public void close() throws IOException {
		for (Closeable c : soundFiles) {
			if (c != null) {
				c.close();
			}
		}
	}

}
