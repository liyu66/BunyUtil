package zzx;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import zzx.utils.IllegalUsageException;

public class Start {
	
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
		    "  -h, --help\n" +
		    "      Show this help message and exit.\n\n" +
		    "Examples:\n" +
		    "  BunyUtil.exe --extract .\\data_1.buny .\\extracted\\\n" +
		    "  BunyUtil.exe --extract .\\data.buny .\\extracted\\ data/actors/ats\n" +
		    "  BunyUtil.exe --modify .\\mods\\my_mod\\\n" +
		    "  BunyUtil.exe --modify\n" +
		    "  BunyUtil.exe --reset .\\data.buny\n" +
		    "  BunyUtil.exe --reset\n";
	
	public static final String dataBunyPath = "data.buny";
	public static final String data1BunyPath = "data_1.buny";
	public static final String modsPath = "mods";
	
    public static void main(String[] args) {
        try {
        	if (args.length < 1) {
        		throw new IllegalUsageException("Wrong number of parameters");
			} else {
				utilMode(args[0], Arrays.copyOfRange(args, 1, args.length));
			}
        } catch (IllegalUsageException e) {
        	System.err.println(e.getMessage() + "\n");
        	System.err.print(USAGE);
        	System.exit(1);
		} catch (Exception e) {
			System.err.println("An unexpected error has occurred.\n"
			        + "If you need help, please provide the operation you were performing "
			        + "along with the following error details:");
            e.printStackTrace();
            System.exit(1);
        }
        
        System.exit(0);
    }
    
    public static void utilMode(String option, String[] args) throws IOException {
    	BunyDriver driver = new BunyDriver(Start::printToScreen);
		switch (option.toLowerCase()) {
			case "-e":
			case "--extract":
				if (args.length == 2) {
					driver.extract(args[0], args[1]);
				} else if (args.length == 3) {
					driver.extract(args[0], args[1], args[2]);
				} else {
					throw new IllegalUsageException("Wrong number of parameters");
				}
				break;
			case "-m":
			case "--modify":
				if (args.length == 0) {
					// Load all mods in the default mods folder
					driver.modifyAll(getDataBunyPath(), getData1BunyPath(), getModsPath());
				} else if (args.length == 1) {
					// Only load the specified mod
					driver.modify(getDataBunyPath(), getData1BunyPath(), args[0]);
				} else {
					throw new IllegalUsageException("Wrong number of parameters");
				}
				break;
			case "-r":
			case "--reset":
				if (args.length == 0) {
					driver.reset(getDataBunyPath());
					driver.reset(getData1BunyPath());
				} else if (args.length == 1) {
					driver.reset(args[0]);
				} else {
					throw new IllegalUsageException("Wrong number of parameters");
				}
				break;
			case "-rm":
				if (args.length == 0) {
					resetAndModify();
				} else {
					throw new IllegalUsageException("Wrong number of parameters");
				}
				break;
			default:
				throw new IllegalUsageException("Unknown option: " + option);
		}
    }
    
    public static void resetAndModify() throws IOException {
    	BunyDriver driver = new BunyDriver(Start::printToScreen);
    	
    	try (BunyStruct dataBuny = new BunyStruct(getDataBunyPath());
    		 BunyStruct data1Buny = new BunyStruct(getData1BunyPath())) {
    		
    		driver.reset(dataBuny);
        	driver.reset(data1Buny);
        	
        	driver.modifyAll(dataBuny, data1Buny, getModsPath());
    	}
    }
    
    public static String getDataBunyPath() {
        return resolvePath(dataBunyPath);
    }

    public static String getData1BunyPath() {
        return resolvePath(data1BunyPath);
    }

    public static String getModsPath() {
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
    
    public static void writeToLogFile(String info) {
    	// ...To be implemented, uhhhh...
    }
    
    public static void printToScreen(String info) {
    	System.out.print(info);
    }
    
    public static void printNothing(String info) {
    	return;
    }
}
