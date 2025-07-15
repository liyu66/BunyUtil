package zzx;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import zzx.Mod.SoundFile;
import zzx.buny.BunyStruct;
import zzx.buny.FileInside;
import zzx.buny.TocBackup;
import zzx.buny.TocBackup.Item;
import zzx.fsb5.Chunk;
import zzx.fsb5.Fsb5Builder;
import zzx.fsb5.Fsb5Reader;
import zzx.fsb5.Sound;
import zzx.fsb5.SoundFromFsb;
import zzx.utils.FileTree;
import zzx.utils.IllegalUsageException;
import zzx.utils.LERandomAccessFile;
import zzx.utils.ProgressBar;

import static zzx.Config.*;

public class Driver {
	
	public static void loadInfo(BunyStruct buny) throws IOException {
	    final int totalFiles = (int) buny.getFileCount();
	    ProgressBar progressBar = new ProgressBar(totalFiles);
	    
	    for (int i = 0; i < totalFiles; i++) {
	        buny.readFile(i);
	        progressBar.update(i);
	    }
	    
	    progressBar.complete();
	}
	
	public static void extract(BunyStruct buny, String outputPath, String prefix) throws IOException {
		// Print header info
        println("=== BunyStruct Header Info ===");
        println("Magic ID String : " + buny.getIdString());
        println("TOC Offset      : " + buny.getTocOffset());
        println("TOC Size        : " + buny.getTocSize() + " bytes");
        println("Names Offset    : " + buny.getNameTableOffset());
        println("Names Size      : " + buny.getNameTableSize() + " bytes");
        println("TOC2 Offset     : " + buny.getToc2Offset());
        println("TOC2 Size       : " + buny.getToc2Size() + " bytes");
        println();
        long fileCount = buny.getFileCount();
        println("File Count      : " + fileCount);
        println("========  End ========\n");
        
        prefix = prefix.replace("\\", "/");

        // Extract files
        int extractedCount = 0;
        for (int i = 0; i < buny.getFileCount(); i++) {
        	FileInside file = buny.getFile(i);
        	if (file.getName().startsWith(prefix)) {
				println(String.format(
						"Extracting(progress: %d/%d, size: %s): %s ",
						file.getIndex() + 1, fileCount,
						humanReadableByteCount(file.getSize()), file.getName()
					));
				file.extractTo(outputPath);
				extractedCount++;
        	}
        }
        println("\nSuccessfully extracted " + extractedCount + " files");
	}
	
    public static void reset(BunyStruct buny) throws IOException {
        if (!canReset(buny)) {
            throw new IllegalUsageException(
                "Unable to reset the specified buny file. " +
                "Please ensure it's either the main 'data.buny' or 'data_1.buny' from the game directory, " +
                "and that the file is intact and not corrupted."
            );
        }

        boolean isData1 = isData1Buny(buny);
        String targetName = isData1 ? "data_1.buny" : "data.buny";

        println("Starting reset for \'" + targetName + "\'...");

        FileTree<FileInside> modifiedFiles = searchAllModifiedFile(buny, isData1);
        if (modifiedFiles.size() == 0) {
            println("No modified files found - nothing to reset.");
            return;
        }
        println("Located " + modifiedFiles.size() + " modified files to reset.");

        TocBackup tocBackup = getTocBackup(isData1);

        Iterator<Entry<String, FileInside>> iterator = modifiedFiles.printingIterator();
        int i = 0;
        while (iterator.hasNext()) {
        	FileInside file = iterator.next().getValue();
        	
            Item item = tocBackup.get(file.getIndex());
            file.redirectTo(item.offset, item.zsize, item.size);
            
            print(" -> resetted");
            i++;
        }

        buny.setNewLength(isData1 ? data1BunyOriginalLength : dataBunyOriginalLength);

        println("\nReset completed. " + i + " files were reset.");
    }
	
	public static void modify(BunyStruct dataBuny, BunyStruct data1Buny, String modsPath) throws IOException {
		File modsDir = new File(modsPath);
	    if (!modsDir.exists() || !modsDir.isDirectory()) {
	        throw new IOException("Invalid mods directory: " + modsPath);
	    }

	    File[] modDirs = modsDir.listFiles(File::isDirectory);
	    if (modDirs == null || modDirs.length == 0) {
	        println("No mods found in directory: " + modsPath);
	        return;
	    }
	    
	    Arrays.sort(modDirs, Comparator.comparing(File::getName));
	    
	    modify(dataBuny, data1Buny, modDirs);
	}
	
	public static void modify(BunyStruct dataBuny, BunyStruct data1Buny, File... modDirs) throws IOException {
		
	    List<Mod> mods = new ArrayList<>();
	    for (File modDir : modDirs) {
		    mods.add(new Mod(modDir));
		}
	    
	    SoundModifier soundModifier = new SoundModifier(dataBuny);
	    
		int modCount = 0;
		for (Mod mod : mods) {
			println();
			println("[" + (modCount++) + "]Start loading mod: " + mod.getName());
			
			replaceNormalFiles(dataBuny, data1Buny, mod);
			
			removeSoundEvent(soundModifier, mod);
			replaceSoundFiles(soundModifier, mod);
			
			println("\nSuccessfully loaded mod: " + mod.getName());
		}
		
		if (soundModifier.needWriteBack()) {
			println();
			soundModifier.writeBackWithPrint();
		}
		
		for (Mod mod : mods) {
			mod.close();
		}
	    
		println("\nSuccessfully loaded " + modCount + " mod(s).");
	}

	public static void split(String fsbFile, String outputPath) throws IOException {
		try (Fsb5Reader fsb = new Fsb5Reader(fsbFile)) {
			println("=== Fsb5 Header Info ===");
			println("Magic ID String  : " + fsb.getIdString());
			println("version          : " + fsb.getVersion());
			println("Sound File Count : " + fsb.getSoundCount());
			println("Table Size       : " + fsb.getTableSize() + " bytes");
			println("Name Table Size  : " + fsb.getNameTableSize() + " bytes");
			println("Data Size        : " + fsb.getDataSize() + " bytes");
			println("Codec            : " + fsb.getCodec());
			println();

			int count = 0;
			for (SoundFromFsb sound : fsb.getAllSounds()) {
				println(sound.toString());
				for (Chunk chunk : sound.getAllChunks()) {
					println(chunk.toString());
				}
				println();

				String name = sound.getName();
				if (name == null) {
					name = Integer.toString(sound.getIndex());
				}

				String outputFsbPath = Paths.get(outputPath, name + ".fsb").toString();

				Fsb5Builder builder = new Fsb5Builder();
				builder.addSound(sound);
				builder.buildTo(outputFsbPath);

				count++;
			}

			println("Successfully split " + count + " sound(s) from " + fsbFile);
		}
	}
	
	public static void combine(String outputFsbFile, String... inputFsbFiles) throws IOException {
		Fsb5Builder builder = new Fsb5Builder();
		int count = 0;

		for (String fsbFile : inputFsbFiles) {
			try (Fsb5Reader fsb = new Fsb5Reader(fsbFile)) {
				println("=== " + fsbFile + " ===\n");
				for (Sound sound : fsb.getAllSounds()) {
					println(sound.toString());
					for (Chunk chunk : sound.getAllChunks()) {
						println(chunk.toString());
					}
					println();

					builder.addSound(sound);
					count++;
				}
			}
		}

		builder.buildTo(outputFsbFile);
		println("Successfully combined " + count + " sound(s) into " + outputFsbFile);
	}
    
	private static void replaceNormalFiles(BunyStruct dataBuny, BunyStruct data1Buny, Mod mod) throws IOException {
		// data.buny
		if (mod.getDataFiles().size() > 0) {
			replaceNormalFiles(dataBuny, mod, false);
		}
		
		// data_1.buny
		if (mod.getData1Files().size() > 0) {
			if (mod.getDataFiles().size() > 0) {
				println();
			}
			replaceNormalFiles(data1Buny, mod, true);
		}
		
		if (mod.getDataFiles().size() <= 0 && mod.getData1Files().size() <= 0) {
			println("  This mod doesn't appear to contain any sound-unrelated files");
		}
	}
	
	private static void replaceNormalFiles(BunyStruct buny, Mod mod, boolean isData1) throws IOException {
		Iterator<Entry<String, File>> newFiles;
		if (isData1) {
			newFiles = mod.getData1Files().printingIterator();
		} else {
			newFiles = mod.getDataFiles().printingIterator();
		}
		
		int replaced = 0;
		int skipped = 0;
		while (newFiles.hasNext()) {
			Entry<String, File> entry = newFiles.next();
			String name = entry.getKey();
			File file = entry.getValue();
			name = name.substring(isData1 ? "data1/".length() : "data/".length());
			
			if (buny.containFile(name)) {
				buny.getFile(name).redirectTo(file);
				print(" -> replaced");
				replaced++;
			} else {
				print(" -> skipped");
				skipped++;
			}
		}
		print("\n  Result: " + replaced + " files replaced");
		println(skipped <= 0 ? "." : ";  " + skipped + " files skipped.");
	}
	
	private static void removeSoundEvent(SoundModifier soundModifier, Mod mod) throws IOException {
		for (int i = 0; i < LEVEL_COUNT ; i++) {
			File removeFloFile = mod.getRemoveFloFile(i);
			if (removeFloFile != null) {
				soundModifier.removeEventBy(removeFloFile, i);
			}
		}
	}
	
	private static void replaceSoundFiles(SoundModifier soundModifier, Mod mod) throws IOException {
		int soundFileCount = mod.getSoundFileCount();
		if (soundFileCount > 0) {
			println();
			for (int i = 0; i < soundFileCount; i++) {
				SoundFile soundFile = mod.getSoundFile(i);
				int soundCount = soundFile.getSounds().length;
				
				if (soundFile.isIndexBased()) {
					print("  [" + i + "] " + soundFile.getSoundIndex() + 
						  " (total " + soundCount + " sounds): ");
				} else {
					print("  [" + i + "] " + soundFile.getEventName() +
						  " (total " + soundCount + " sounds): ");
				}
				
				String result = soundModifier.modifyBy(soundFile);
				println(result);
			}
		} else {
			println("  This mod doesn't appear to contain any sound-related files");
		}
	}
	
	private static void print(String string) {
        System.out.print(string);
    }
    
    private static void println() {
        System.out.println();
    }

    private static void println(String string) {
    	System.out.println(string);
    }
    
    private static String humanReadableByteCount(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int unit = 1024;
        String[] units = { "KB", "MB", "GB", "TB", "PB", "EB" };
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String unitName = units[exp - 1];
        double result = bytes / Math.pow(unit, exp);
        return String.format("%.1f %s", result, unitName);
    }

}
