/** Copyright (c) Robert Michna
 * rm406247@students.mimuw.edu.pl
 */
package pl.edu.mimuw.rm406247.indexer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import pl.edu.mimuw.rm406247.IndexerUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.Scanner;
import java.util.function.Consumer;

public class Indexer implements AutoCloseable {

    private IndexWriter indexWriter;
    private FSDirectory indexDir;
    private Path indexPath;

    /** Creates new indexer.
     *
     * @param indexPath - path to the index directory ending with \ or /
     * @throws IOException
     */
    public Indexer(Path indexPath) throws IOException{
        this.indexPath = indexPath;
        Analyzer analyzer;
        try {
            analyzer = IndexerUtils.defaultAnalyzer();
        }
        catch (IOException e) {
            System.err.println("Fatal error creating analyzer.");
            System.err.println(e.getMessage());
            throw e;
        }
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

        try {
            indexDir = FSDirectory.open(indexPath);
            if (DirectoryReader.indexExists(indexDir)) {
                iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
            }
            else {
                iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            }
        }
        catch (IOException e) {
            System.err.println("Got IOException while opening index directory.");
            System.err.println(e.getMessage());
            throw e;
        }

        try {
            indexWriter = new IndexWriter(indexDir, iwc);
        }
        catch (IOException e) {
            System.err.println("Got IOException while opening index.");
            System.err.println(e.getMessage());
            throw e;
        }
    }

    /** Performs action on every file in the directory pointed by @p path.
     * If path points to a file only this file is indexed.
     *
     * @param path - Path to file / directory
     * @param fileAction - Action to be performed on the files.
     * @throws IOException
     */
    private void indexDocs(Path path, Consumer<Path> fileAction) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            FileVisitor<Path> fileVisitor = new IndexingVisitor(fileAction);
            Files.walkFileTree(path, fileVisitor);
        }
        else if (Files.isRegularFile(path)){
            fileAction.accept(path);
        }
        else {
            throw new IOException("Invalid path.");
        }
    }

    /** Saves the specified file to the index.
     * Since "Można założyć, że parsowane dokumenty napisane są w języku
     * polskim albo angielskim", we treat every file as if it was written in
     * english with the exception for files that were written in polish and
     * return @p true in @p LanguageResult.isReasonablyCertain()
     * Only files of the following formats are indexed:
     * TXT, PDF, RTF, OpenXML, Open Document Format (text).
     *
     *
     * @param path - Path to the indexed file.
     */
    private void addDocFunction(Path path) {
        if (Files.isRegularFile(path)) {
            try {
                Tika tika = new Tika();
                Document document = new Document();
                String extension = tika.detect(path);

                if (!(extension.contains("opendocument.text")
                        || extension.contains("rtf")
                        || extension.contains("openxmlformats")
                        || extension.contains("pdf")
                        || extension.contains("text/plain"))) {
                    return; // invalid file type
                }
                String content = tika.parseToString(path);
                LanguageResult language_result = detectLanguage(content);
                String lang_short = "en";
                if (language_result.isReasonablyCertain() && language_result.getLanguage().equals("pl")) {
                    lang_short = "pl";
                }

                StringField fieldPath =
                        new StringField("path", path.toAbsolutePath().toString(), Field.Store.YES);
                TextField fieldContent =
                        new TextField("body-" + lang_short, content, Field.Store.YES);
                String titleText = path.toFile().getName().replace(".", " ");
                TextField fieldTitle =
                        new TextField("title-" + lang_short, titleText, Field.Store.YES);
                document.add(fieldPath);
                document.add(fieldContent);
                document.add(fieldTitle);

                if (indexWriter.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                    System.out.println("Adding " + path.getFileName());
                    indexWriter.updateDocument(new Term("path", path.toAbsolutePath().toString()), document);
                    //indexWriter.addDocument(document);
                }
                else {
                    System.out.println("Updating " + path.getFileName());
                    indexWriter.updateDocument(new Term("path", path.toAbsolutePath().toString()), document);
                }
            }
            catch (TikaException te) {
                System.err.println("Tika exception reading file " + path.toString());
                System.err.println(te.getMessage());
            }
            catch (IOException e){
                System.err.println("IOException reading file " + path.toString());
                System.err.println(e.getMessage());
            }
        }
        else {
            // ignore
        }
    }

    /** Removes file from index.
     * @param path - Path to the removed file.
     */
    @Deprecated
    private void removeDocFunction(Path path) {
        if (Files.isRegularFile(path)) {
            try {
                Tika tika = new Tika();
                String extension = tika.detect(path);

                if (!(extension.contains("opendocument.text")
                        || extension.contains("rtf")
                        || extension.contains("openxmlformats")
                        || extension.contains("pdf")
                        || extension.contains("text/plain"))) {
                    return; // invalid file type
                }
                System.out.println("Removing " + path.getFileName());
                indexWriter.deleteDocuments(new Term("path", path.toAbsolutePath().toString()));
                indexWriter.commit();
            }
            catch (IOException e) {
                System.err.println("IOException reading file " + path.toString());
                System.err.println(e.getMessage());
            }
        }
        else {
            // ignore
        }
    }

    /** Removes all documents which have @p path as their prefix.
     * @param path - Folder / file to delete.
     */
    private void removeDoc(Path path) {
        Query query;
        try (IndexReader indexReader = DirectoryReader.open(indexDir)) {
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            query = new PrefixQuery(new Term("path", path.toAbsolutePath().toString()));
        }
        catch (IOException e) {
            System.err.println("Failed to open searcher in removeDoc.");
            System.err.println(e.getMessage());
            return;
        }
        try {
            indexWriter.deleteDocuments(query);
        }
        catch (IOException e) {
            System.err.println("Could not delete documents.");
            System.err.println(e.getMessage());
        }
    }

    /// Adds documents.
    public void add(Path docsPath) {
        try {
            indexDocs(docsPath, this::addDocFunction);
        }
        catch (IOException e) {
            System.err.println("IOException in walkFileTree:");
            System.err.println(e.getMessage());
        }
        try {
            indexWriter.commit();
        }
        catch (IOException e) {
            System.err.println("Could not save changes to index.");
            System.err.println(e.getMessage());
        }
    }

    /** Adds directory / file to the index and saves the path to watched paths.
     * @param docsPath - Path to the file / directory.
     */
    public void addToIndexedPaths(Path docsPath) {
        File indexedDirs = new File(indexPath.toString() + File.separator + "indexed_dirs.txt");
        try (FileWriter fileWriter = new FileWriter(indexedDirs, true)) {
            add(docsPath);
            fileWriter.write(docsPath.toAbsolutePath().toString() + "\n");
        }
        catch (IOException e) {
            System.err.println("IOException raised by add:");
            System.err.println(e.getMessage());
        }
    }

    public void remove(Path docsPath) {
        removeDoc(docsPath);
        try {
            indexWriter.commit();
        }
        catch (IOException e) {
            System.err.println("Could not save changes to index.");
            System.err.println(e.getMessage());
        }
    }

    /** Removes directory / file from the index and removes the path to watched paths.
     * @param docsPath - Path to the file / directory.
     */
    public void removeFromIndexedPaths(Path docsPath) {
        boolean happened = false;
        StringBuilder paths = new StringBuilder();
        File indexedDirs = new File(indexPath + File.separator + "indexed_dirs.txt");
        try (Scanner scanner = new Scanner(indexedDirs)){
            while (scanner.hasNextLine()) {
                String path = scanner.nextLine();
                if (!path.equals(docsPath.toAbsolutePath().toString())) {
                    paths.append(path);
                }
                else {
                    happened = true;
                    remove(docsPath);
                }
            }
        }
        catch (IOException e) {
            System.err.println("IOException raised by remove:");
            System.err.println(e.getMessage());
        }

        if (!happened) {
            System.out.println("Specified path was not previously added by --add command.");
            return;
        }

        try (FileWriter fileWriter = new FileWriter(indexedDirs, false)) {
            fileWriter.write(paths.toString());
        }
        catch (IOException e) {
            System.err.println("Failed to save directories:");
            System.err.println(e.getMessage());
        }
    }

    public void reindex() {
        File indexedDirs = new File(indexPath + File.separator + "indexed_dirs.txt");
        try (Scanner scanner = new Scanner(indexedDirs)) {
            while (scanner.hasNextLine()) {
                Path docsPath = Paths.get(scanner.nextLine());
                if (!Files.isReadable(docsPath)) {
                    System.err.println("Directory "
                            + docsPath
                            + " does not exist or is not readable");
                    close();
                    return;
                }
                remove(docsPath);
                add(docsPath);
            }
        }
        catch(FileNotFoundException e) {
            System.err.println("No file containing indexed directories.");
            return;
        }
        try {
            indexWriter.commit();
        }
        catch (IOException e) {
            System.err.println("Could not save changes to index.");
            System.err.println(e.getMessage());
        }
    }

    private static LanguageResult detectLanguage(String text) throws IOException {
        LanguageDetector detector = new OptimaizeLangDetector().loadModels();
        return detector.detect(text);
    }

    public void purge() throws IOException{
        indexWriter.deleteAll();
        File indexedDirs = new File(indexPath + File.separator + "indexed_dirs.txt");
        indexedDirs.delete();
        try {
            indexWriter.commit();
        }
        catch (IOException e) {
            System.err.println("Could not save changes to index.");
            System.err.println(e.getMessage());
        }
    }

    public void close() {
        try {
            indexWriter.close();
            indexDir.close();
        }
        catch (IOException e) {
            System.err.println("Error closing indexer.");
        }
    }
}
