package pl.edu.mimuw.rm406247.indexer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class IndexerMain {

    private IndexerMain() {}

    public static void main(String[] args) {

        Path indexPath = Paths.get(System.getProperty("user.home")
                + File.separator
                + ".index"
                + File.separator);

        if (!Files.isReadable(indexPath) || !Files.isWritable(indexPath)) {
            System.out.println("Directory "
                    + indexPath
                    + " does not exist or the application does not have valid permissions.");
            System.exit(1);
        }

        try (Indexer indexer = new Indexer(indexPath)) {

            boolean watchMode = true;
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("--purge")) {
                    indexer.purge();
                    watchMode = false;
                    break;
                }

                if (args[i].equals("--add") || args[i].equals("--rm")) {
                    Path docsPath = Paths.get(args[i + 1]);
                    if (!Files.isReadable(docsPath)) {
                        System.out.println("Directory "
                                + indexPath
                                + " does not exist or is not readable");
                        indexer.close();
                        System.exit(1);
                    }
                    if (args[i].equals("--add")) {
                        indexer.add(docsPath);
                    }
                    else {
                        indexer.remove(docsPath);
                    }
                    watchMode = false;
                    break;
                }

                if (args[i].equals("--reindex")) {

                    watchMode = false;
                    break;
                }

                if (args[i].equals("--list")) {

                    watchMode = false;
                    break;
                }
            }

            while (watchMode) {
                break; // todo
            }
        }
        catch (IOException e) {
            System.out.println("Error opening index.");
        }
        System.exit(0);
    }
}
