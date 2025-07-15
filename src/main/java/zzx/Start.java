package zzx;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import zzx.buny.BunyStruct;
import zzx.utils.IllegalUsageException;

import static zzx.Config.*;

public class Start {
	
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
        
        System.out.println("\nAll Done!");
        System.exit(0);
    }
    
    public static void utilMode(String option, String[] args) throws IOException {
    	
		switch (option.toLowerCase()) {
			case "-e":
			case "--extract":
				if (args.length == 2) {
					extract(args[0], args[1]);
				} else if (args.length == 3) {
					extract(args[0], args[1], args[2]);
				} else {
					throw new IllegalUsageException("Wrong number of parameters");
				}
				break;
			case "-m":
			case "--modify":
				if (args.length == 0) {
					modify();
				} else if (args.length == 1) {
					modify(args[0]);
				} else {
					throw new IllegalUsageException("Wrong number of parameters");
				}
				break;
			case "-r":
			case "--reset":
				if (args.length == 0) {
					reset();
				} else if (args.length == 1) {
					reset(args[0]);
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
			case "-s":
			case "--split":
				if (args.length == 2) {
					split(args[0], args[1]);
				} else {
					throw new IllegalUsageException("Wrong number of parameters");
				}
				break;
			case "-c":
			case "--combine":
				if (args.length >= 2) {
					combine(args[0], Arrays.copyOfRange(args, 1, args.length));
				} else {
					throw new IllegalUsageException("Wrong number of parameters");
				}
				break;
			default:
				throw new IllegalUsageException("Unknown option: " + option);
		}
    }
    
    public static void extract(String bunyFilePath, String outputPath) throws IOException {
    	extract(bunyFilePath, outputPath, "");
    }
    
    public static void extract(String bunyFilePath, String outputPath, String prefix) throws IOException {
    	try (BunyStruct buny = new BunyStruct(bunyFilePath)) {
			Driver.loadInfo(buny);
			Driver.extract(buny, outputPath, prefix);
		}
    }
    
    // Load all mods in the default mods folder
    public static void modify() throws IOException {
    	try (BunyStruct dataBuny = new BunyStruct(getDefaultDataBunyPath());
    		 BunyStruct data1Buny = new BunyStruct(getDefaultData1BunyPath())) {
    		Driver.loadInfo(dataBuny);
    		Driver.loadInfo(data1Buny);
    		Driver.modify(dataBuny, data1Buny, getDefaultModsPath());
    	}
    }
    
    // Only load the specified mod
    public static void modify(String modPath) throws IOException {
    	try (BunyStruct dataBuny = new BunyStruct(getDefaultDataBunyPath());
       		 BunyStruct data1Buny = new BunyStruct(getDefaultData1BunyPath())) {
    		Driver.loadInfo(dataBuny);
    		Driver.loadInfo(data1Buny);
       		Driver.modify(dataBuny, data1Buny, new File(modPath));
       	}
    }
    
    public static void reset() throws IOException {
    	reset(getDefaultDataBunyPath());
    	reset(getDefaultData1BunyPath());
    }

	public static void reset(String bunyFilePath) throws IOException {
		try (BunyStruct buny = new BunyStruct(bunyFilePath)) {
			Driver.loadInfo(buny);
			Driver.reset(buny);
		}
	}

	public static void resetAndModify() throws IOException {
		try (BunyStruct dataBuny = new BunyStruct(getDefaultDataBunyPath());
			 BunyStruct data1Buny = new BunyStruct(getDefaultData1BunyPath())) {

			Driver.loadInfo(dataBuny);
			Driver.loadInfo(data1Buny);

			Driver.reset(dataBuny);
			Driver.reset(data1Buny);

			Driver.modify(dataBuny, data1Buny, getDefaultModsPath());
		}
	}
    
    public static void split(String fsbFile, String outputPath) throws IOException {
    	Driver.split(fsbFile, outputPath);
    }
    
    public static void combine(String outputFsbFile, String... inputFsbFiles) throws IOException {
    	Driver.combine(outputFsbFile, inputFsbFiles);
    }
}
