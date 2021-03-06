/** Copyright (c) Robert Michna
 * rm406247@students.mimuw.edu.pl
 */
package pl.edu.mimuw.rm406247.indexer;

import pl.edu.mimuw.rm406247.IndexerUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;


public class IndexerMain {

    private IndexerMain() {}

    public static void main(String[] args) {

        Path indexPath = IndexerUtils.indexPath();

        if (!Files.isReadable(indexPath) || !Files.isWritable(indexPath)) {
            System.err.println("Directory "
                    + indexPath
                    + " does not exist or the application does not have valid permissions.");
            System.exit(1);
        }

        try (Indexer indexer = new Indexer(indexPath)) {
            if (args.length != 0) {
                if (args[0].equals("--purge")) {
                    if (args.length != 1) {
                        System.out.println("Invalid number of arguments.");
                    }
                    else {
                        indexer.purge();
                    }
                }

                else if (args[0].equals("--add") || args[0].equals("--rm")) {
                    if (args.length != 2) {
                        System.out.println("Invalid number of arguments.");
                    }
                    else {
                        Path docsPath = Paths.get(args[1]);
                        if (!Files.isReadable(docsPath)) {
                            System.err.println("Directory "
                                    + docsPath
                                    + " does not exist or is not readable");
                            indexer.close();
                            System.exit(1);
                        }
                        if (args[0].equals("--add")) {
                            indexer.addToIndexedPaths(docsPath);
                        }
                        else {
                            indexer.removeFromIndexedPaths(docsPath);
                        }
                    }
                }

                else if (args[0].equals("--reindex")) {
                    if (args.length != 1) {
                        System.out.println("Invalid number of arguments.");
                    }
                    else {
                        indexer.reindex();
                    }
                }

                else if (args[0].equals("--list")) {
                    if (args.length != 1) {
                        System.out.println("Invalid number of arguments.");
                    }
                    else {
                        File indexedDirs = new File(indexPath + File.separator + "indexed_dirs.txt");
                        try (Scanner scanner = new Scanner(indexedDirs)) {
                            while (scanner.hasNextLine()) {
                                System.out.println(scanner.nextLine());
                            }
                        }
                        catch (FileNotFoundException e) {
                            System.err.println("No file containing indexed directories.");
                        }
                    }
                }

                else {
                    System.out.println("Invalid parameter.");
                }
            }
            else {
                File indexedDirs = new File(indexPath + File.separator + "indexed_dirs.txt");
                ArrayList<Path> paths = new ArrayList<>();
                try (Scanner scanner = new Scanner(indexedDirs)) {
                    while (scanner.hasNextLine()) {
                        String pathString = scanner.nextLine().trim();
                        paths.add(Paths.get(pathString));
                    }
                }
                catch (IOException e) {
                    System.err.println("Could not open file containing paths.");
                    System.err.println(e.getMessage());
                }
                try {
                    new Watcher(indexer, paths).processEvents();
                }
                catch (IOException e) {
                    System.err.println("Could not initialize watchservice.");
                    System.err.println(e.getMessage());
                }
            }
        }
        catch (IOException e) {
            System.err.println("Error opening index.");
        }
        System.exit(0);
    }
}
