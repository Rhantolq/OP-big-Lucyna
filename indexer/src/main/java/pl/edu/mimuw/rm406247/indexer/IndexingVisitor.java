/** Copyright (c) Robert Michna
 * rm406247@students.mimuw.edu.pl
 */
package pl.edu.mimuw.rm406247.indexer;


import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;

public class IndexingVisitor extends SimpleFileVisitor<Path> {

    private Consumer<Path> fileAction;

    public IndexingVisitor(Consumer<Path> fileAction) {
        this.fileAction = fileAction;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        fileAction.accept(file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException io) {
        System.err.println("Couldn't visit " + file.toAbsolutePath().toString());
        System.err.println(io.getMessage());
        return FileVisitResult.CONTINUE;
    }
}