package pl.edu.mimuw.rm406247.indexer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
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

        Map<String, Analyzer> analyzerMap = new TreeMap<>();
        analyzerMap.put("body-en", new EnglishAnalyzer());
        analyzerMap.put("body-pl", new PolishAnalyzer());
        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), analyzerMap);
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

        try {
            indexDir = FSDirectory.open(indexPath);
            if (DirectoryReader.indexExists(indexDir)) {
                iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
            } else {
                iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            }
        }
        catch (IOException e) {
            System.out.println("Got IOException while opening index directory.");
            System.out.println(e.getMessage());
            throw e;
        }

        try {
            indexWriter = new IndexWriter(indexDir, iwc);
        }
        catch (IOException e) {
            System.out.println("Got IOException while opening index.");
            System.out.println(e.getMessage());
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
    public void indexDocs(Path path, Consumer<Path> fileAction) throws IOException {
        if (Files.isDirectory(path)) {
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
    private void addDoc(Path path) {
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

                StringField fieldPath = new StringField("path", path.toAbsolutePath().toString(), Field.Store.YES);
                TextField fieldContent = new TextField("body-" + lang_short, content, Field.Store.YES);
                document.add(fieldPath);
                document.add(fieldContent);

                if (indexWriter.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                    System.out.println("Adding " + path.getFileName());
                    System.out.println(path.toString());
                    indexWriter.addDocument(document);
                }
                else {
                    System.out.println("Updating " + path.getFileName());
                    System.out.println(path.toString());
                    System.out.println(path.toAbsolutePath().toString());
                    indexWriter.updateDocument(new Term("path", path.toAbsolutePath().toString()), document);
                }
            }
            catch (TikaException te) {
                System.out.println("Tika exception reading file " + path.toString());
                System.out.println(te.getMessage());
            }
            catch (IOException e){
                System.out.println("IOException reading file " + path.toString());
                System.out.println(e.getMessage());
            }
        }
        else {
            // ignore
        }
    }

    /** Removes file from index.
     * @param path - Path to the removed file.
     */
    private void removeDoc(Path path) {
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
                System.out.println("IOException reading file " + path.toString());
                System.out.println(e.getMessage());
            }
        }
        else {
            // ignore
        }
    }

    /** Adds directory / file to the index.
     * @param docsPath - Path to the file / directory.
     */
    public void add(Path docsPath) {
        File indexedDirs = new File(indexPath.toString() + File.separator + "indexed_dirs.txt");
        try (FileWriter fileWriter = new FileWriter(indexedDirs, true)) {
            try {
                indexDocs(docsPath, this::addDoc);
                fileWriter.write(docsPath.toAbsolutePath().toString());
            }
            catch (IOException e) {
                System.out.println("IOException in walkFileTree:");
                System.out.println(e.getMessage());
            }
        }
        catch (IOException e) {
            System.out.println("IOException raised by add:");
            System.out.println(e.getMessage());
        }
    }

    /** Removes directory / file from the index.
     * @param docsPath - Path to the file / directory.
     */
    public void remove(Path docsPath) {
        StringBuilder paths = new StringBuilder();
        File indexedDirs = new File(indexPath + File.separator + "indexed_dirs.txt");
        try (Scanner scanner = new Scanner(indexedDirs)){
            indexDocs(docsPath, this::removeDoc);
            while (scanner.hasNext()) {
                String path = scanner.nextLine();
                if (!path.equals(docsPath.toAbsolutePath().toString())) {
                    paths.append(path);
                }
            }
        }
        catch (IOException e) {
            System.out.println("IOException raised by remove:");
            System.out.println(e.getMessage());
        }

        try (FileWriter fileWriter = new FileWriter(indexedDirs, false)) {
            fileWriter.write(paths.toString());
        }
        catch (IOException e) {
            System.out.println("Failed to save directories:");
            System.out.println(e.getMessage());
        }
    }

    private static LanguageResult detectLanguage(String text) throws IOException {
        LanguageDetector detector = new OptimaizeLangDetector().loadModels();
        return detector.detect(text);
    }

    public void purge() throws IOException{
        indexWriter.deleteAll();
    }

    public void close() {
        try {
            indexWriter.close();
            indexDir.close();
        }
        catch (IOException e) {
            System.out.println("Error closing indexer.");
        }
    }
}
