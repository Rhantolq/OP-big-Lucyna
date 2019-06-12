package pl.edu.mimuw.rm406247.indexer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class Indexer {

    private Indexer() {}

    public static void main(String[] args) {
        Path path = Paths.get("C:\\Users\\rober\\Desktop\\STUDIA\\PO\\PROJEKT2\\ExampleToInddex");
        try {
            indexDocs(path);
        }
        catch (IOException e) {
            System.out.println("IOException in void main");
        }
    }

    private static void indexDocs(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    try {
                        indexDoc(file);
                    }
                    catch (IOException ignore) {
                        System.out.println("Caught IOException in path: " + path);
                        System.out.println(ignore.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        else {
            indexDoc(path);
        }
    }

     private static void indexDoc(Path path) throws IOException{
        System.out.println("Path: " +path);
     }

    private static class IndexingFileVisitor extends SimpleFileVisitor {

    }
}
