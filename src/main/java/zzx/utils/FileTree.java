package zzx.utils;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;

/**
 * A tree-based map implementation that simulates a file system structure.
 * Stores values using path-like keys (e.g., "dir1/dir2/file.txt").
 * Supports directory scanning, tree visualization, and path-based operations.
 *
 * @param <V> the type of values stored in file nodes
 */
public class FileTree<V> implements Map<String, V> {
    /**
     * Abstract base class for tree nodes (both directories and files).
     */
    private abstract class Node {
        public final String name;
        public final boolean isDir;
        
        public Node(String name, boolean isDir) {
            this.name = name;
            this.isDir = isDir;
        }
        
        /**
         * @return this node as a directory
         * @throws IllegalStateException if node is not a directory
         */
        public Dir asDir() {
            if (isDir) {
                return (Dir) this;
            } else {
                throw new IllegalStateException("This node is not a directory.");
            }
        }
        
        /**
         * @return this node as a file
         * @throws IllegalStateException if node is not a file
         */
        public FileNode asFile() {
            if (!isDir) {
                return (FileNode) this;
            } else {
                throw new IllegalStateException("This node is not a file.");
            }
        }
    }
    
    /**
     * Represents a directory node containing child nodes.
     */
    private class Dir extends Node {
        public Map<String, Node> children = new HashMap<>();
        
        public Dir(String name) {
            super(name, true);
        }
    }
    
    /**
     * Represents a file node storing a value.
     */
    private class FileNode extends Node {
        public V value;
        
        public FileNode (String name, V value) {
            super(name, false);
            this.value = value;
        }
        
        /**
         * Updates the value in this file node.
         * 
         * @param newValue the value to set
         * @return the previous value
         */
        public V updateValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }
    }
    
    private Dir root;
    private int size = 0;
    
    /**
     * Creates an empty file tree with the specified root name.
     * 
     * @param rootDir the name of the root directory
     */
    public FileTree(String rootDir) {
        root = new Dir(rootDir);
    }
    
    /**
     * Creates a file tree by scanning a directory structure.
     * 
     * @param rootDir the root directory to scan
     * @param fileConverter function to convert files to values
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if rootDir is not a directory
     */
    public FileTree(File rootDir, Function<File, V> fileConverter) throws IOException {
        this(rootDir.getName());
        scanDirectory(rootDir, fileConverter);
    }
    
    /**
     * Recursively scans a directory and populates the tree.
     */
    private void scanDirectory(File rootDir, Function<File, V> fileConverter) throws IOException {
        if (!rootDir.exists()) {
            return;
        }
        
        if (!rootDir.isDirectory()) {
            throw new IllegalArgumentException("Path is not a directory: " + rootDir.getAbsolutePath());
        }

        scanDirectoryRecursive(rootDir, "", fileConverter);
    }
    
    /**
     * Recursive helper for directory scanning.
     */
    private void scanDirectoryRecursive(File currentDir, String currentPath, Function<File, V> fileConverter) {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            String relativePath = currentPath.isEmpty() ? 
                file.getName() : 
                currentPath + "/" + file.getName();
            
            if (file.isFile()) {
                V value = fileConverter.apply(file);
                put(relativePath, value);
            } else if (file.isDirectory()) {
                scanDirectoryRecursive(file, relativePath, fileConverter);
            }
        }
    }
    
    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        for (V v : values()) {
            if (v.equals(value)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public V get(Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        return get((String) key);
    }

    /**
     * Gets the value associated with a path.
     * 
     * @param key the path to look up
     * @return the associated value, or null if not found
     * @throws IllegalArgumentException for invalid paths
     */
    public V get(String key) {
        Dir dir = root;
        List<String> dirNames = split(key);
        
        if (dirNames.isEmpty()) {
            throw new IllegalArgumentException("Invalid path after splitting: " + key);
        }
        
        // Separate last component as filename
        String fileName = dirNames.remove(dirNames.size() - 1);
        
        // Traverse directories
        for (String dirName : dirNames) {
            Node node = dir.children.get(dirName);
            if (node == null || !node.isDir) {
                return null;
            }
            dir = node.asDir();
        }
        
        // Get file node
        Node node = dir.children.get(fileName);
        if (node == null || node.isDir) {
            return null;
        }
        return node.asFile().value;
    }

    @Override
    public V put(String key, V value) {
        // Handle null as removal
        if (value == null) {
            return remove(key);
        }
        
        Dir dir = root;
        List<String> dirNames = split(key);
        
        if (dirNames.isEmpty()) {
            throw new IllegalArgumentException("Invalid path after splitting: " + key);
        }
        
        // Separate last component as filename
        String fileName = dirNames.remove(dirNames.size() - 1);
        
        // Create missing directories
        for (String dirName : dirNames) {
            Node node = dir.children.get(dirName);
            
            if (node == null) {
                node = new Dir(dirName);
                dir.children.put(dirName, node);
            } else if (!node.isDir) {
                throw new IllegalStateException("Path component conflicts with existing file: " + dirName);
            }
            dir = node.asDir();
        }
        
        // Create or update file node
        Node node = dir.children.get(fileName);
        if (node != null) {
            if (node.isDir) {
                throw new IllegalStateException("Cannot put value to a directory path: " + key);
            } else {
                return node.asFile().updateValue(value);
            }
        } else {
            node = new FileNode(fileName, value);
            dir.children.put(fileName, node);
            size++;
            return null;
        }
    }
    
    /**
     * Gets all file entries in a directory.
     * 
     * @param targetDir the directory path
     * @return set of file entries in the directory
     */
    public Set<Entry<String, V>> getAllFile(String targetDir) {
        Dir dir = root;
        List<String> dirNames;
        
        if (targetDir.isEmpty()) {
            dirNames = List.of();
        } else {
            dirNames = split(targetDir);
        }
        
        // Navigate to target directory
        for (String dirName : dirNames) {
            Node node = dir.children.get(dirName);
            if (node == null || !node.isDir) {
                return Set.of();
            }
            dir = node.asDir();
        }
        
        // Collect file entries
        Set<Entry<String, V>> result = new HashSet<>();
        for (Node node : dir.children.values()) {
            if (!node.isDir) {
                result.add(new AbstractMap.SimpleEntry<>(node.name, node.asFile().value));
            }
        }
        return result;
    }
    
    public Set<String> getAllSubDir(String targetDir) {
        Dir dir = root;
        List<String> dirNames;
        
        if (targetDir.isEmpty()) {
            dirNames = List.of();
        } else {
            dirNames = split(targetDir);
        }
        
        // Navigate to target directory
        for (String dirName : dirNames) {
            Node node = dir.children.get(dirName);
            if (node == null || !node.isDir) {
                return Set.of();
            }
            dir = node.asDir();
        }
        
        // Collect file entries
        Set<String> result = new HashSet<>();
        for (Node node : dir.children.values()) {
            if (node.isDir) {
                result.add(node.name);
            }
        }
        return result;
    }
    
    @Override
    public V remove(Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        return remove((String) key);
    }
    
    /**
     * Removes a file from the tree.
     * 
     * @param key the path to remove
     * @return the removed value, or null if not found
     */
    public V remove(String key) {
        List<String> names = split(key);
        if (names.isEmpty()) {
            throw new IllegalArgumentException("Invalid path after splitting: " + key);
        }
        return remove(root, names.iterator());
    }
    
    /**
     * Recursive removal helper.
     */
    private V remove(Dir dir, Iterator<String> names) {
        String name = names.next();
        Node next = dir.children.get(name);
        
        if (next == null) {
            return null;
        }
        
        if (!names.hasNext()) {  // Final component
            if (next.isDir) {
                return null;  // Can't remove directory
            } else {
                dir.children.remove(name);
                size--;
                return next.asFile().value;
            }
        }
        
        if (next.isDir) {
            Dir nextDir = next.asDir();
            V v = remove(nextDir, names);
            // Remove empty directories
            if (nextDir.children.isEmpty()) {
                dir.children.remove(name);
            }
            return v;
        } else {
            return null;  // Path component is file but path continues
        }
    }
    
    /**
     * Splits a path into components.
     * 
     * @param key the path to split
     * @return list of path components
     * @throws NullPointerException for null input
     * @throws IllegalArgumentException for empty or absolute paths
     */
    private List<String> split(String key) {
        if (key == null) {
            throw new NullPointerException("Key cannot be null");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
        if (key.startsWith("/") || key.startsWith("\\")) {
            throw new IllegalArgumentException("Absolute paths are not allowed: " + key);
        }
        
        // Split using both Unix and Windows separators
        String[] parts = key.split("[\\\\/]");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty()) {  // Skip empty components
                result.add(part);
            }
        }
        return result;
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> m) {
        for (Entry<? extends String, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        root.children.clear();
        size = 0;
    }

    @Override
    public Set<String> keySet() {
        Set<String> keys = new HashSet<>();
        collectKeys(root, root.name, keys);
        return keys;
    }

    /**
     * Recursively collects all file paths.
     */
    private void collectKeys(Dir dir, String currentPath, Set<String> keys) {
        for (Node child : dir.children.values()) {
            String childPath = currentPath.isEmpty() ? 
                child.name : 
                currentPath + "/" + child.name;
            
            if (child.isDir) {
                collectKeys(child.asDir(), childPath, keys);
            } else {
                keys.add(childPath);
            }
        }
    }

    @Override
    public Collection<V> values() {
        List<V> values = new ArrayList<>();
        collectValues(root, values);
        return values;
    }

    /**
     * Recursively collects all values.
     */
    private void collectValues(Dir dir, List<V> values) {
        for (Node child : dir.children.values()) {
            if (child.isDir) {
                collectValues(child.asDir(), values);
            } else {
                values.add(child.asFile().value);
            }
        }
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
        Set<Entry<String, V>> result = new HashSet<>();
        collectEntries(root, root.name, result);
        return result;
    }
    
    /**
     * Recursively collects all file entries.
     */
    private void collectEntries(Dir dir, String currentPath, Set<Entry<String, V>> set) {
        for (Node child : dir.children.values()) {
            String childPath = currentPath.isEmpty() ? 
                child.name : 
                currentPath + "/" + child.name;
            
            if (child.isDir) {
                collectEntries(child.asDir(), childPath, set);
            } else {
                set.add(new AbstractMap.SimpleEntry<>(childPath, child.asFile().value));
            }
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(root.name).append("/\n");
        
        // Sort directories before files
        List<Node> children = new ArrayList<>(root.asDir().children.values());
        children.sort(Comparator.comparing(n -> n.isDir));
        
        int i = 0;
        for (Node child : children) {
            toString(sb, "", ++i == children.size(), child);
        }
        return sb.toString();
    }
    
    // Tree visualization constants
    private static final String VERTICAL    = "|   ";
    private static final String BRANCH      = "|-- ";
    private static final String LAST_BRANCH = "`-- ";
    
    /*
     * Classic Unix Tree style(requires Unicode):
     * private static String VERTICAL    = "│   ";
     * private static String BRANCH      = "├── ";
     * private static String LAST_BRANCH = "└── ";
     */
    
    /**
     * Recursive helper for tree visualization.
     */
    private void toString(StringBuilder sb, String prefix, boolean isLast, Node node) {
        // Draw current node
        sb.append(prefix)
            .append(isLast ? LAST_BRANCH : BRANCH)
            .append(node.name)
            .append(node.isDir ? "/" : "")
            .append('\n');
        
        // Prepare prefix for children
        String childPrefix = prefix + (isLast ? "    " : VERTICAL);
        
        if (node.isDir) {
            // Sort child nodes
            List<Node> children = new ArrayList<>(node.asDir().children.values());
            children.sort(Comparator.comparing(n -> n.isDir));
            
            int i = 0;
            for (Node child : children) {
                toString(sb, childPrefix, ++i == children.size(), child);
            }
        }
    }

    /**
     * Returns an iterator that visualizes the tree structure during traversal.
     * 
     * @return iterator that prints the tree structure
     */
    public Iterator<Entry<String, V>> printingIterator() {
        return new TreePrintingIterator();
    }

    /**
     * Iterator that prints the tree structure during traversal.
     */
    private class TreePrintingIterator implements Iterator<Entry<String, V>> {
        private final Stack<IteratorFrame> stack = new Stack<>();
        private Node nextFile = null;
        private String nextPath = "";
        private String nextPrefix = "";

        /**
         * Tracks iteration state for a directory.
         */
        private class IteratorFrame {
            final Dir dir;
            final String path;
            final String prefix;
            final Iterator<Node> childIter;
            boolean hasMoreChildren;

            IteratorFrame(Dir dir, String path, String prefix) {
                this.dir = dir;
                this.path = path;
                this.prefix = prefix;
                List<Node> children = new ArrayList<>(dir.children.values());
                children.sort(Comparator.comparing((Node n) -> n.name));
                this.childIter = children.iterator();
                this.hasMoreChildren = childIter.hasNext();
            }
        }

        public TreePrintingIterator() {
            System.out.print(root.name + "/");
            if (!root.children.isEmpty()) {
                stack.push(new IteratorFrame(root, root.name, ""));
                advanceToNextFile();
            }
        }

        /**
         * Advances to the next printable node (file or directory).
         */
        private void advanceToNextFile() {
            while (!stack.isEmpty()) {
                IteratorFrame frame = stack.peek();
                
                if (!frame.childIter.hasNext()) {
                    stack.pop();
                    continue;
                }

                Node node = frame.childIter.next();
                frame.hasMoreChildren = frame.childIter.hasNext();
                
                String childPath = frame.path + "/" + node.name;
                String childPrefix = frame.prefix + (frame.hasMoreChildren ? VERTICAL : "    ");

                if (node.isDir) {
                    // Store directory for printing and push to stack
                    nextFile = node;
                    nextPath = childPath;
                    nextPrefix = frame.prefix + (frame.hasMoreChildren ? BRANCH : LAST_BRANCH);
                    stack.push(new IteratorFrame(node.asDir(), childPath, childPrefix));
                    return;
                } else {
                    // Store file for return
                    nextFile = node;
                    nextPath = childPath;
                    nextPrefix = frame.prefix + (frame.hasMoreChildren ? BRANCH : LAST_BRANCH);
                    return;
                }
            }
            nextFile = null;  // End of iteration
        }

        @Override
        public boolean hasNext() {
            return nextFile != null;
        }

        @Override
        public Entry<String, V> next() {
            if (!hasNext()) throw new NoSuchElementException();
            
            // Print node visualization
            System.out.print("\n" + nextPrefix + nextFile.name + 
                           (nextFile.isDir ? "/" : ""));
            
            FileNode currentFile = nextFile.isDir ? null : nextFile.asFile();
            String currentPath = nextPath;
            
            advanceToNextFile();  // Prepare next node
            
            if (currentFile != null) {
                // Return file entry
                return new AbstractMap.SimpleEntry<>(currentPath, currentFile.value);
            } else {
                // Skip directories and return next file
                return next();
            }
        }
    }
}