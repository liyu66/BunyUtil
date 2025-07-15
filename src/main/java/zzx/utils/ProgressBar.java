package zzx.utils;

/**
 * A utility class for displaying progress updates in the console. 
 * Dynamically shows a progress bar when processing large datasets (>1000 items),
 * including percentage completion, item counts, and time estimations.
 */
public class ProgressBar {
    /** Total number of items to process */
    private final int totalItems;
    /** Fixed width of the progress bar in characters */
    private final int progressBarWidth;
    /** Flag indicating whether to show progress visualization */
    private final boolean showProgress;
    /** Timestamp when progress tracking started (milliseconds) */
    private final long startTime;
    /** Last recorded percentage to prevent redundant updates */
    private int lastPercent = -1;
    /** Length of previously printed progress string for console cleanup */
    private int lastPrintedLength = 0;

    /**
     * Initializes a new progress tracker.
     * 
     * @param totalItems Total number of items to be processed.
     * Auto-enables progress display when item count exceeds 1000.
     */
    public ProgressBar(int totalItems) {
        this.totalItems = totalItems;
        this.showProgress = totalItems > 1000;
        this.progressBarWidth = 50;
        this.startTime = showProgress ? System.currentTimeMillis() : 0;
    }

    /**
     * Updates the progress display.
     * Only triggers visual updates when:
     *   - Progress percentage changes by ≥1% 
     *   - Processing the final item
     * 
     * @param currentItem Zero-based index of the currently processed item
     */
    public void update(int currentItem) {
        if (!showProgress || totalItems <= 0) return;
        
        // Calculate current completion percentage
        int percent = (int) ((currentItem + 1) * 100.0 / totalItems);
        
        // Update only on 1%+ change or for final item
        if (percent != lastPercent || currentItem == totalItems - 1) {
            lastPercent = percent;
            printProgress(currentItem + 1, percent);
        }
    }

    /**
     * Finalizes the progress display.
     * Ensures 100% completion is shown and prints processing summary.
     * Must be called after all items are processed.
     */
    public void complete() {
        if (!showProgress) return;
        
        // Force final display if not at 100%
        if (lastPercent < 100) {
            printProgress(totalItems, 100);
        }
        
        // Calculate and display total processing time
        long duration = System.currentTimeMillis() - startTime;
        System.out.println();  // Newline after progress bar
        System.out.printf("Loaded %d files in %.2f seconds%n", 
                          totalItems, duration / 1000.0);
    }

    /**
     * Renders the progress bar with dynamic information.
     * 
     * @param processed Number of processed items (1-based count)
     * @param percent Current completion percentage (0-100)
     */
    private void printProgress(int processed, int percent) {
        // Calculate filled portion of progress bar
        int filledWidth = (int) (progressBarWidth * (percent / 100.0));
        
        // Construct progress bar visualization
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < progressBarWidth; i++) {
            if (i < filledWidth) bar.append("=");          // Completed portion
            else if (i == filledWidth) bar.append(">");   // Progress head
            else bar.append(" ");                          // Unfilled portion
        }
        bar.append("] ");
        
        // Add numeric progress indicators
        bar.append(String.format("%d%% (%d/%d)", percent, processed, totalItems));
        
        // Add ETA estimation after initial 5% progress
        if (percent > 5 && processed < totalItems) {
            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = (elapsed * (totalItems - processed)) / processed;
            bar.append(String.format(" ETA: %.1fs", remaining / 1000.0));
        }
        
        // Clear previous output using last known length
        String clearLine = "\r" + " ".repeat(lastPrintedLength) + "\r";
        System.out.print(clearLine);
        
        // Print current progress and store display length
        String progressStr = bar.toString();
        System.out.print("\r" + progressStr);
        lastPrintedLength = progressStr.length();
    }
}