package zzx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import zzx.BunyStruct.FileInside;
import zzx.utils.IllegalUsageException;
import zzx.utils.TocBackup;
import zzx.utils.TocBackup.Item;

public class BunyDriver {
	
	public static final String dataTocBackupFileName = "dataTocBackup.dat";
	public static final String data1TocBackupFileName = "data1TocBackup.dat";
	
	private final Consumer<String> infoPrinter;
	
	public BunyDriver(Consumer<String> infoPrinter) {
		this.infoPrinter = infoPrinter;
	}
	
	public void extract(String bunyFilePath, String outputPath) throws IOException {
		extract(bunyFilePath, outputPath, "");
    }
	
	public void extract(String bunyFilePath, String outputPath, String prefix) throws IOException {
    	try (BunyStruct buny = new BunyStruct(bunyFilePath)) {
            extract(buny, outputPath, prefix);
        }
    }
	
	public void extract(BunyStruct buny, String outputPath) throws IOException {
		extract(buny, outputPath, "");
	}
	
	public void extract(BunyStruct buny, String outputPath, String prefix) throws IOException {
		// Print header info
        println("=== BunyStruct Header Info ===");
        println("Magic ID String : " + buny.getIdString());
        println("TOC Offset      : " + buny.getTocOffset());
        println("TOC Size        : " + buny.getTocSize() + " bytes");
        println("Names Offset    : " + buny.getNamesOffset());
        println("Names Size      : " + buny.getNamesSize() + " bytes");
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
        	FileInside file = buny.readFile(i);
        	if (file.getName().startsWith(prefix)) {
				println(String.format(
						"Extracting(progress: %d/%d, size: %s): %s ",
						file.getIndex(), fileCount,
						humanReadableByteCount(file.getSize()), file.getName()
					));
				file.extractTo(outputPath);
				extractedCount++;
        	}
        }
        println("\nSuccessfully extracted " + extractedCount + " files");
        println("All done!");
	}
	
	public void modifyAll(String dataBunyPath, String data1BunyPath, String modsPath) throws IOException {
		try (BunyStruct dataBuny = new BunyStruct(dataBunyPath);
			 BunyStruct data1Buny = new BunyStruct(data1BunyPath)) {
			modifyAll(dataBuny, data1Buny, modsPath);
		}
	}
	
	public void modifyAll(BunyStruct dataBuny, BunyStruct data1Buny, String modsPath) throws IOException {
	    File modsDir = new File(modsPath);
	    if (!modsDir.exists() || !modsDir.isDirectory()) {
	        throw new IOException("Invalid mods directory: " + modsPath);
	    }

	    File[] modDirs = modsDir.listFiles(File::isDirectory);
	    if (modDirs == null || modDirs.length == 0) {
	        println("No mods found in directory: " + modsPath);
	        return;
	    }

	    println("==> Starting batch mod loading from: " + modsPath);
	    println();
	    
	    int appliedMods = 0;
	    for (File modDir : modDirs) {
	        try {
	            modify(dataBuny, data1Buny, modDir.getAbsolutePath());
	            appliedMods++;
	        } catch (Exception e) {
	            // Print error but continue with the next mod
	            println("!! Failed to load mod: " + modDir.getName() + " (" + e.getMessage() + ")");
	        }
	    }

	    println();
	    println("==> Successfully loaded " + appliedMods + " mod(s).");
	}
	
	public void modify(String dataBunyPath, String data1BunyPath, String modPath) throws IOException {
		try (BunyStruct dataBuny = new BunyStruct(dataBunyPath);
			 BunyStruct data1Buny = new BunyStruct(data1BunyPath)) {
			modify(dataBuny, data1Buny, modPath);
		}
	}
    
	public void modify(BunyStruct dataBuny, BunyStruct data1Buny, String modPath) throws IOException {
	    File modDir = new File(modPath);
	    if (!modDir.exists() || !modDir.isDirectory()) {
	        throw new IOException("Invalid mod directory: " + modPath);
	    }

	    String modName = modDir.getName();
	    println("==> Loading mod: " + modName);

	    boolean modifiedAnything = false;

	    File dataDir = new File(modDir, "data");
	    if (dataDir.exists() && dataDir.isDirectory()) {
	        println("Modifying data.buny from 'data' directory...");
	        modify(dataBuny, dataDir.getAbsolutePath());
	        println("Finished modifying data.buny.");
	        modifiedAnything = true;
	    }

	    File data1Dir = new File(modDir, "data1");
	    if (data1Dir.exists() && data1Dir.isDirectory()) {
	        println("Modifying data_1.buny from 'data1' directory...");
	        modify(data1Buny, data1Dir.getAbsolutePath());
	        println("Finished modifying data_1.buny.");
	        modifiedAnything = true;
	    }

	    if (!modifiedAnything) {
	        println("No valid 'data' or 'data1' directories found in mod. Nothing was modified.");
	    }

	    println("==> Finished loading mod: " + modName);
	}

    private void modify(BunyStruct buny, String dirPath) throws IOException {
        buny.readFilesIfNotExist();
        AtomicInteger replaceFileCount = new AtomicInteger();
        AtomicInteger skipFileCount = new AtomicInteger();
        buny.update(dirPath,
        		fileName -> {
        			replaceFileCount.incrementAndGet();
        			println("Replaced: " + fileName);
        			}, 
        		fileName -> {
        			skipFileCount.incrementAndGet();
        			println("Skiped: " + fileName);
        			});
        println(replaceFileCount + " files were Replaced.");
        println(skipFileCount + " files were skipped.");
    }
    
    public void reset(String bunyFilePath) throws IOException {
        try (BunyStruct buny = new BunyStruct(bunyFilePath)) {
            reset(buny);
        }
    }
    
    public void reset(BunyStruct buny) throws IOException {
        if (!canReset(buny)) {
            throw new IllegalUsageException(
                "Unable to reset the specified buny file. " +
                "Please ensure it's either the main 'data.buny' or 'data_1.buny' from the game directory, " +
                "and that the file is intact and not corrupted."
            );
        }

        boolean isData1 = isData1Buny(buny);
        String targetName = isData1 ? "data_1.buny" : "data.buny";

        println("==> Starting reset for " + targetName + "...");

        List<FileInside> modifiedFiles = searchAllModifiedFile(buny, isData1);
        println("Located " + modifiedFiles.size() + " modified files to reset.");

        TocBackup tocBackup = getTocBackup(isData1);

        int prevFileIndex = -1;
        int i = 0;
        for (FileInside file : modifiedFiles) {
            println("Resetting: " + file.getName());
            tocBackup.skip(file.getIndex() - prevFileIndex - 1);
            Item item = tocBackup.get();
            file.redirect(item.offset, item.zsize, item.size);
            prevFileIndex = file.getIndex();
            i++;
        }

        buny.setNewLength(isData1 ? data1BunyOriginalLength : dataBunyOriginalLength);

        println("Reset completed. " + i + " files were reset.");
        println("==> Finished reset for " + targetName + ".");
    }

    
    private static boolean canReset(BunyStruct buny) throws IOException {
    	return isDataBuny(buny) || isData1Buny(buny);
    }
    
    private static final long dataBunyOriginalLength = 6980514369L;
    private static boolean isDataBuny(BunyStruct buny) throws IOException {
    	return  buny.getTocOffset() == 80L			&&
    			buny.getTocSize()   == 712680L		&&
    			buny.getLength() 	>= dataBunyOriginalLength;	
    }
    
    private static final long data1BunyOriginalLength = 86528129L;
    private static boolean isData1Buny(BunyStruct buny) throws IOException {
    	return  buny.getTocOffset() == 80L			&&
    			buny.getTocSize()   == 9800L		&&
    			buny.getLength() 	>= data1BunyOriginalLength;	
    }
    
    private static List<FileInside> searchAllModifiedFile(BunyStruct buny, boolean isData1) throws IOException {
    	List<FileInside> result = new ArrayList<>();
    	buny.readFilesIfNotExist();
    	for (FileInside file : buny.getFiles()) {
			if (isModified(file, isData1)) {
				result.add(file);
			}
		}
    	return result;
    }
    
    private static boolean isModified(FileInside file, boolean isData1) {
    	return file.getOffset() >= (isData1 ? data1BunyOriginalLength : dataBunyOriginalLength);
    }
    
    private static TocBackup getTocBackup(boolean isData1) {
    	String resourcesName = isData1 ? data1TocBackupFileName : dataTocBackupFileName;
		InputStream in = Start.class.getClassLoader().getResourceAsStream(resourcesName);
		if (in == null) {
			throw new RuntimeException("Resource file (" + dataTocBackupFileName + 
				") could not be found");
		}
		return new TocBackup(in);
    }
    
    private void println() {
        infoPrinter.accept("\n");
    }

    private void println(String string) {
        infoPrinter.accept(string);
        infoPrinter.accept("\n");
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
