package zzx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import zzx.buny.BunyStruct;
import zzx.buny.FileInside;
import zzx.buny.TocBackup;
import zzx.utils.FileTree;
import zzx.utils.IllegalUsageException;

public class Config {
	
	public static final String[] LEVEL_NAMES = {
	        "ui","a1","a2","a3",
	        "b1","b2","b3",
	        "c1","c2","c3",
	        "d1","d2","d3",
	        "e1","e2","e3",
	        "f1","f2","f3"
	    };
	
	public static final int[] LEVEL_INDICES = {
		    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
		    10, 11, 12, 13, 14, 15, 16, 17, 18
	};
	
	public static final Map<String,Integer> LEVEL_TO_INDEX = new HashMap<>();
    static {
    	for (int i = 0; i < LEVEL_NAMES.length; i++) {
            LEVEL_TO_INDEX.put(LEVEL_NAMES[i], i);
        }
    }
    
    public static int LEVEL_COUNT = LEVEL_NAMES.length;
    
    public static String[] FSB_NAMES = new String[LEVEL_COUNT];
    public static String[] FLO_NAMES = new String[LEVEL_COUNT];
    static {
    	for (int i = 0; i < LEVEL_COUNT; i++) {
			FSB_NAMES[i] = "data/sound/" + LEVEL_NAMES[i] + ".fsb";
			FLO_NAMES[i] = "data/sound/pc_" + LEVEL_NAMES[i] + ".flo";
		}
    }
    
    public static boolean isLevelName(String s) {
    	return LEVEL_TO_INDEX.containsKey(s);
    }
	
	public static final String dataTocBackupFileName = "dataTocBackup.dat";
	public static final String data1TocBackupFileName = "data1TocBackup.dat";
	
	public static boolean canReset(BunyStruct buny) throws IOException {
    	return isDataBuny(buny) || isData1Buny(buny);
    }
    
	public static final long dataBunyOriginalLength = 6980514369L;
	public static boolean isDataBuny(BunyStruct buny) throws IOException {
    	return  buny.getTocOffset() == 80L			&&
    			buny.getTocSize()   == 712680L		&&
    			buny.getLength() 	>= dataBunyOriginalLength;	
    }
    
	public static final long data1BunyOriginalLength = 86528129L;
	public static boolean isData1Buny(BunyStruct buny) throws IOException {
    	return  buny.getTocOffset() == 80L			&&
    			buny.getTocSize()   == 9800L		&&
    			buny.getLength() 	>= data1BunyOriginalLength;	
    }
    
	public static FileTree<FileInside> searchAllModifiedFile(BunyStruct buny, boolean isData1) throws IOException {
    	FileTree<FileInside> result = new FileTree<>(isData1 ? "data_1.buny" : "data.buny");
    	for (FileInside file : buny.getAllFiles()) {
			if (isModified(file, isData1)) {
				result.put(file.getName(), file);
			}
		}
    	return result;
    }
	
	public static boolean isModified(FileInside file, boolean isData1) {
    	return file.getOffset() >= (isData1 ? data1BunyOriginalLength : dataBunyOriginalLength);
    }
    
	public static TocBackup getTocBackup(boolean isData1) {
    	String resourcesName = isData1 ? data1TocBackupFileName : dataTocBackupFileName;
		InputStream in = Start.class.getClassLoader().getResourceAsStream(resourcesName);
		if (in == null) {
			throw new RuntimeException("Resource file (" + dataTocBackupFileName + 
				") could not be found");
		}
		return new TocBackup(in);
    }
	
	public static final String USAGE =
		    "Usage: BunyUtil.exe [option] <arguments>\n\n" +
		    "Options:\n" +
		    "  -e, --extract <bunyFilePath> <outputPath> [prefix]\n" +
		    "      Extract files from the specified .buny archive to the output directory.\n" +
		    "      If [prefix] is provided, only files whose paths start with the given prefix will be extracted.\n\n" +
		    
		    "  -m, --modify [modPath]\n" +
		    "      Apply a mod to the .buny archive by replacing existing resources.\n" +
		    "      If [modPath] is specified, only that mod will be used. Otherwise, all mods in the 'mods' directory\n" +
		    "      will be automatically applied.\n\n" +
		    
		    "  -r, --reset [bunyFilePath]\n" +
		    "      Revert all changes previously made by the --modify operation to the specified .buny archive.\n" +
		    "      If [bunyFilePath] is omitted, both data.buny and data_1.buny will be reset.\n\n" +
		    
		    "  -s, --split <audioFile> <outputPath>\n" +
		    "      Split a multi-audio .fsb/.bank file into individual .fsb files (each containing one audio).\n" +
		    "      Output files will be saved to the specified directory with their original names or indices.\n" +
		    "      Accepts both .fsb and .bank files as input.\n\n" +
		    
		    "  -c, --combine <outputAudioFile> <inputAudioFile...>\n" +
		    "      Combine multiple .fsb/.bank files into a single output file containing all audios.\n" +
		    "      Requires at least one input file and an output file path.\n" +
		    "      Input files can be any combination of .fsb and .bank formats.\n\n" +
		    
		    "  -h, --help\n" +
		    "      Show this help message and exit.\n\n" +
		    
		    "Examples:\n" +
		    "  BunyUtil.exe --extract .\\data_1.buny .\\extracted\\\n" +
		    "  BunyUtil.exe --extract .\\data.buny .\\extracted\\ data/actors/ats\n" +
		    "  BunyUtil.exe --modify .\\mods\\my_mod\\\n" +
		    "  BunyUtil.exe --modify\n" +
		    "  BunyUtil.exe --reset .\\data.buny\n" +
		    "  BunyUtil.exe --reset\n" +
		    "  BunyUtil.exe --split .\\audio.bank .\\split_audios\\\n" +
		    "  BunyUtil.exe --combine .\\combined.fsb .\\audio1.fsb .\\audio2.bank .\\audio3.fsb\n";
	
	private static final String dataBunyPath = "data.buny";
	private static final String data1BunyPath = "data_1.buny";
	private static final String modsPath = "mods";

    public static String getDefaultDataBunyPath() {
        return resolvePath(dataBunyPath);
    }

    public static String getDefaultData1BunyPath() {
        return resolvePath(data1BunyPath);
    }

    public static String getDefaultModsPath() {
        return resolvePath(modsPath);
    }

    private static String resolvePath(String primaryPath) {
        File primary = new File(primaryPath);
        if (primary.exists()) {
            return primary.getPath();
        }

        File fallback = new File(".." + File.separator + primaryPath);
        if (fallback.exists()) {
            return fallback.getPath();
        }

        throw new IllegalUsageException("Missing file: " + primaryPath + 
        		 	". Please run the program from the game root or a subdirectory.");
    }
}
