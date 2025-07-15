package zzx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FloStruct {
	
	private class EventMap {
		String line;
		
		final int index;
		boolean isRandom;
		int eventIndex;
		final String eventName;
		
		EventMap(String line) {
			this.line = line;
			
			line = line.trim();
		    
		    String[] parts = line.split(",\\s*");
		    
		    if (parts.length != 4) {
		        throw new IllegalArgumentException("Invalid line: " + line);
		    }

		    this.index = Integer.parseInt(parts[0]);
		    this.isRandom = Integer.parseInt(parts[1]) == 1;
		    this.eventIndex = Integer.parseInt(parts[2]);
		    this.eventName = parts[3];
		}
		
		void update(boolean isRandom, int eventIndex) {
			this.isRandom = isRandom;
			this.eventIndex = eventIndex;
			
			line = index + ",\t" +
				   (isRandom ? 1 : 0) + ", " +
				   eventIndex + ", " +
				   eventName;
		}
		
		@Override
		public String toString() {
		    return line;
		}
		
		@Override
		public boolean equals(Object obj) {
		    if (this == obj) return true;
		    if (obj == null || getClass() != obj.getClass()) return false;
		    EventMap other = (EventMap) obj;
		    return line.equals(other.line);
		}

		@Override
		public int hashCode() {
		    return line.hashCode();
		}
	}
	
	private List<String> startInfo = new ArrayList<>();
	private List<String> soundDataFiles = new ArrayList<>();
	private List<String> soundParameterSets = new ArrayList<>();
	private List<String> simpleEvents = new ArrayList<>();
	private List<int[]> randomEvents = new ArrayList<>();
	private List<EventMap> eventMapsPre = new ArrayList<>();
	// private List<EventMap> eventMapsPan = new ArrayList<>();
	private List<EventMap> eventMapsPos = new ArrayList<>();
	
	private Map<String, EventMap> eventMaps = new HashMap<>();
	
	public FloStruct(File floFile) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(floFile))) {
			
			// start info (Unimportant part)
	        for (String line = reader.readLine();
	        	 !"SoundDataFiles".equals(line);
	        	 line = reader.readLine()) {
	        	startInfo.add(line);
	        }
	        
	        // SoundDataFiles
	        int count = Integer.parseInt(reader.readLine());
	        for (int i = 0; i < count; i++) {
	        	soundDataFiles.add(reader.readLine());
	        }
	        
	        // SoundParameterSets (Unimportant part)
	        for (String line = readLineSkipEmpty(reader);
				 !"SimpleEvents".equals(line);
				 line = reader.readLine()) {
				soundParameterSets.add(line);
			}
	        
	        // SimpleEvents
	        count = Integer.parseInt(reader.readLine());
	        for (int i = 0; i < count; i++) {
	        	simpleEvents.add(reader.readLine());
	        }
	        
	        // RandomEvents title
	        readLineSkipEmpty(reader);
	        
	        // RandomEvents group count
	        count = Integer.parseInt(reader.readLine());
	        
	        // RandomEvents group
	        for (int i = 0; i < count; i++) {
	        	String header = reader.readLine().trim();
	            String[] headerParts = header.split(",\\s*");
	            int eventCount = Integer.parseInt(headerParts[1]);
	            int[] eventIndices = new int[eventCount];

	            for (int j = 0; j < eventCount; j++) {
	                String line = reader.readLine().trim();
	                String[] parts = line.split(",\\s*");
	                eventIndices[j] = Integer.parseInt(parts[1]);
	            }

	            randomEvents.add(eventIndices);
	        }
	        
	        // EventMaps title
	        readLineSkipEmpty(reader);
	        
	        // EventMaps Pre
	        readLineSkipEmpty(reader);
	        count = Integer.parseInt(reader.readLine());
	        for (int i = 0; i < count; i++) {
	        	EventMap eventMap = new EventMap(reader.readLine());
	        	eventMapsPre.add(eventMap);
	        	eventMaps.put(eventMap.eventName, eventMap);
	        }
	        
	        /*
	         * In pc_ui.flo, the actual number of lines here will be 1 more than indicated by count.
	         * This extra line should be discarded.
	         */
	        reader.readLine();
	        
	        // EventMaps Pan (always empty)
	        readLineSkipEmpty(reader);
	        count = Integer.parseInt(reader.readLine());
	        if (count != 0) {
	        	throw new IllegalArgumentException("Invalid EventMaps Pan count: " + count);
	        }
	        
	        // EventMaps Pos
	        readLineSkipEmpty(reader);
	        count = Integer.parseInt(reader.readLine());
	        for (int i = 0; i < count; i++) {
	        	EventMap eventMap = new EventMap(reader.readLine());
	        	eventMapsPos.add(eventMap);
	        	eventMaps.put(eventMap.eventName, eventMap);
	        }
	    }
	}
	
	public void writeTo(File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // startInfo
            for (String line : startInfo) {
                writeLine(writer, line);
            }
            
            // SoundDataFiles
            writeLine(writer, "SoundDataFiles");
            writeIntLine(writer, soundDataFiles.size());
            for (String line : soundDataFiles) {
                writeLine(writer, line);
            }
            writeEmptyLine(writer);
            
            // SoundParameterSets
            for (String line : soundParameterSets) {
                writeLine(writer, line);
            }
            
            // SimpleEvents
            writeLine(writer, "SimpleEvents");
            writeIntLine(writer, simpleEvents.size());
            for (String line : simpleEvents) {
                writeLine(writer, line);
            }
            writeEmptyLine(writer);
            
            // RandomEvents
            writeLine(writer, "RandomEvents");
            writeIntLine(writer, randomEvents.size());
            for (int i = 0; i < randomEvents.size(); i++) {
                int[] group = randomEvents.get(i);
                writeLine(writer, i + ",\t" + group.length);
                for (int event : group) {
                    writeLine(writer, "\t0, " + event);
                }
            }
            writeEmptyLine(writer);
            
            // EventMaps title
            writeLine(writer, "EventMaps");
            writeEmptyLine(writer);
            
            // EventMaps Pre
            writeLine(writer, "Pre");
            writeIntLine(writer, eventMapsPre.size());
            for (EventMap eventMap : eventMapsPre) {
                writeLine(writer, eventMap.toString());
            }
            writeEmptyLine(writer);
            
            // EventMaps Pan
            writeLine(writer, "Pan");
            writeIntLine(writer, 0);
            writeEmptyLine(writer);
            
            // EventMaps Pos
            writeLine(writer, "Pos");
            writeIntLine(writer, eventMapsPos.size());
            for (EventMap eventMap : eventMapsPos) {
                writeLine(writer, eventMap.toString());
            }
            writeEmptyLine(writer);
        }
    }
	
	public int addSound(String unknown, String name) {
	    int index = soundDataFiles.size();
	    String line = index + ",\t" + unknown + ",\t" + name;
	    soundDataFiles.add(line);
	    return index;
	}

	public int addSimpleEvent(int soundIndex, int unknown, int playMode) {
	    int index = simpleEvents.size();
	    String line = index + ",\t" + soundIndex + ", " + unknown + ", " + playMode;
	    simpleEvents.add(line);
	    return index;
	}
	
	public int addRandomEventGroup(int... eventIndices) {
	    int index = randomEvents.size();
	    randomEvents.add(eventIndices);
	    return index;
	}
	
	public boolean containsEvent(String eventName) {
		return eventMaps.containsKey(eventName);
	}
	
	public int[] getEventIndex(String eventName) {
		EventMap eventMap = eventMaps.get(eventName);
		if (eventMap.isRandom) {
			return randomEvents.get(eventMap.eventIndex);
		} else {
			return new int[] {eventMap.eventIndex};
		}
	}
	
	public int[] getSimpleEvent(int eventIndex) {
	    String line = simpleEvents.get(eventIndex).trim();
	    String[] parts = line.split(",\\s*");

	    if (parts.length < 4) {
	        throw new IllegalArgumentException("Invalid simple event line: " + line);
	    }

	    return new int[] {
	        Integer.parseInt(parts[1]),
	        Integer.parseInt(parts[2]),
	        Integer.parseInt(parts[3])
	    };
	}

	public String[] getSoundDataFile(int soundIndex) {
	    String line = soundDataFiles.get(soundIndex).trim();
	    String[] parts = line.split(",\\s*");

	    if (parts.length < 3) {
	        throw new IllegalArgumentException("Invalid sound data line: " + line);
	    }

	    return new String[] {
	        parts[1], // unknown (Possibly related to the type/management/storage method of the Sound?)
	        parts[2]  // filename
	    };
	}
	
	public void redirectEvent(String eventName, boolean isRandom, int eventIndex) {
		EventMap eventMap = eventMaps.get(eventName);
		eventMap.update(isRandom, eventIndex);
	}
	
	public void removeEvent(String line) {
		if (line == null) {
			return;
		}
		
		EventMap eventMap = null;
		try {
			eventMap = new EventMap(line);
		} catch (IllegalArgumentException e) {
			return;
		}
		
		if (eventMaps.containsKey(eventMap.eventName)) {
			eventMaps.remove(eventMap.eventName);
			eventMapsPre.remove(eventMap);
			eventMapsPos.remove(eventMap);
		}
	}
	
	private static String readLineSkipEmpty(BufferedReader reader) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			if (!line.isEmpty()) {
				return line;
			}
		}
		return null;
	}
	
    private static void writeLine(BufferedWriter writer, String content) throws IOException {
        writer.write(content);
        writer.write("\r\n");
    }
    
    private static void writeEmptyLine(BufferedWriter writer) throws IOException {
        writer.write("\r\n");
    }
    
    private static void writeIntLine(BufferedWriter writer, int value) throws IOException {
        writer.write(Integer.toString(value));
        writer.write("\r\n");
    }
}