package pl.edu.mimuw.rm406247.indexer;

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;

/**
 * Example to watch a directory (or tree) for changes to files.
 */

public class Watcher {

    private final Indexer indexer;
    private final WatchService watcher;
    private final Map<WatchKey,Path> keys;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        /*
        Path prev = keys.get(key);
        if (prev == null) {
            System.out.format("register: %s\n", dir);
        }
        else {
            if (!dir.equals(prev)) {
                System.out.format("update: %s -> %s\n", prev, dir);
            }
        }
        */
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException
            {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    Watcher(Indexer indexer, ArrayList<Path> dirs) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.indexer = indexer;
        this.keys = new HashMap<WatchKey,Path>();
        for (Path dir : dirs) {
            System.out.println("Observing: " + dir.toAbsolutePath().toString());
            registerAll(dir);
        }
    }

    /**
     * Process all events for keys queued to the watcher
     */
    void processEvents() {
        for(;;) {
            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            }
            catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            // trying to defend against ctrl + x'd folders.
            if (!Files.isDirectory(dir)) {
                keys.remove(key);
                key.cancel();
                continue;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                if (kind == ENTRY_DELETE) {
                    System.out.println("Removing " + child.toAbsolutePath().toString());
                    indexer.remove(child);
                }

                if (kind == ENTRY_MODIFY && !(Files.isDirectory(child, NOFOLLOW_LINKS))) {
                    System.out.println("Removing index " + child.toAbsolutePath().toString());
                    indexer.remove(child);
                    System.out.println("Adding index " + child.toAbsolutePath().toString());
                    indexer.add(child);
                }

                // if directory is created, and watching recursively, then
                // register it and its sub-directories
                if (kind == ENTRY_CREATE) {
                    System.out.println("Adding index " + child.toAbsolutePath().toString());
                    indexer.add(child);
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    }
                    catch (IOException x) {
                        // ignore to keep sample readbale
                    }
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }
}
