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
        System.out.println("Couldn't visit " + file.toString());
        System.out.println(io.getMessage());
        return FileVisitResult.CONTINUE;
    }
}