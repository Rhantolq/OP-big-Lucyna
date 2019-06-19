package pl.edu.mimuw.rm406247.searcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.FSDirectory;


public class Searcher {

    private Searcher() {}

    private enum QueryType {
        TERM ("term"),
        PHRASE ("phrase"),
        FUZZY ("fuzzy");

        private final String name;

        QueryType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public static void main(String[] args) {
        Path indexPath = Paths.get(System.getProperty("user.home")
                + File.separator
                + ".index"
                + File.separator);

        if (!Files.isReadable(indexPath) || !Files.isWritable(indexPath)) {
            System.out.println("Directory "
                    + indexPath
                    + " does not exist or the application does not have valid permissions.");
        }

        String lang = "en";
        int limit = Integer.MAX_VALUE;
        boolean showDetails = false;
        boolean showColors = false;
        QueryType queryType = QueryType.TERM;

        IndexSearcher indexSearcher;
        Map<String, Analyzer> analyzerMap = new TreeMap<>();
        analyzerMap.put("body-en", new EnglishAnalyzer());
        analyzerMap.put("body-pl", new PolishAnalyzer());
        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), analyzerMap);

        try (FSDirectory indexDir = FSDirectory.open(indexPath)) {
            try (IndexReader indexReader = DirectoryReader.open(indexDir)){
                indexSearcher = new IndexSearcher(indexReader);
                Scanner scanner = new Scanner(System.in);

                System.out.print(">");
                while (scanner.hasNext()) {
                    String command = scanner.nextLine();
                    if (command.length() == 0) {
                        continue;
                    }
                    if (command.charAt(0) == '%') {
                        String[] commandArgs =  command.split(" ");
                        if (commandArgs[0].equals("%term")) {
                            if (commandArgs.length > 1) {
                                System.out.println("Too many arguments for %term command.");
                                continue;
                            }
                            queryType = QueryType.TERM;
                            System.out.println("Query type changed to " + queryType);
                        }
                        else if (command.equals("%phrase")) {
                            if (commandArgs.length > 1) {
                                System.out.println("Too many arguments for %phrase command.");
                                continue;
                            }
                            queryType = QueryType.PHRASE;
                            System.out.println("Query type changed to " + queryType);
                        }
                        else if (command.equals("%fuzzy")) {
                            if (commandArgs.length > 1) {
                                System.out.println("Too many arguments for %fuzzy command.");
                                continue;
                            }
                            queryType = QueryType.FUZZY;
                            System.out.println("Query type changed to " + queryType);
                        }
                        else if (commandArgs[0].equals("%lang")) {
                            if (commandArgs.length != 2) {
                                System.out.println("Invalid number of arguments.");
                                continue;
                            }
                            if (!(commandArgs[1].equals("pl") || commandArgs[1].equals("en"))) {
                                System.out.println("Invalid language. (en/pl expected)");
                                continue;
                            }
                            lang = commandArgs[1];
                            System.out.println("Language set to " + lang);
                        }
                        else if (commandArgs[0].equals("%color") || commandArgs[0].equals("%details")) {
                            if (commandArgs.length != 2) {
                                System.out.println("Invalid number of arguments.");
                                continue;
                            }
                            if (!(commandArgs[1].equals("on") || commandArgs[1].equals("off"))) {
                                System.out.println("Invalid color option. (on/off expected)");
                                continue;
                            }
                            if (commandArgs[0].equals("%details")) {
                                showDetails = commandArgs[ 1].equals("on");
                            }
                            else if (commandArgs[0].equals("%color")) {
                                showColors = commandArgs[1].equals("on");
                            }
                        }
                        else {
                            System.out.println("Invalid command.");
                        }

                    }
                    else {
                        Query query = null;
                        if (queryType == QueryType.TERM) {
                            query = new TermQuery(new Term("body-" + lang, command.toLowerCase()));
                        }
                        else if (queryType == QueryType.PHRASE) {
                            query = new PhraseQuery("body-" + lang, command.toLowerCase().split(" "));
                        }
                        else if (queryType == QueryType.FUZZY) {
                            query = new FuzzyQuery(new Term("body-" + lang, command.toLowerCase()));
                        }
                        TopDocs topDocs = indexSearcher.search(query, limit);
                        Formatter formatter = new ConsoleFormatter("***", "***");
                        QueryScorer scorer = new QueryScorer(query);
                        Highlighter highlighter = new Highlighter(formatter, scorer);
                        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 30);
                        highlighter.setTextFragmenter(fragmenter);

                        System.out.println("File count: " + topDocs.scoreDocs.length);
                        for (int i = 0; i < Math.min(limit, topDocs.scoreDocs.length); i++) {
                            int docId = topDocs.scoreDocs[i].doc;
                            Document doc = indexSearcher.doc(docId);
                            System.out.println(doc.getField("path").stringValue() + ":");
                            if (showDetails) {
                                String text = doc.get("body-" + lang);
                                try {
                                    String[] fragments = highlighter.getBestFragments(analyzer,
                                            "body-" + lang,
                                            text,
                                            10);
                                    for (String fragment : fragments) {
                                        System.out.print(fragment);
                                        System.out.println(" ...");
                                    }
                                }
                                catch (Exception e) {
                                    System.out.println("Error fetching context.");
                                    System.out.println(e.getMessage());
                                }
                            }
                        }
                    }
                    System.out.print(">");
                }
                System.exit(0);
            }
            catch (IOException e) {
                System.out.println("Index does not exist. Exiting.");
                System.exit(1);
            }
        }
        catch (IOException e) {
            System.out.println("Got IOException while opening index directory.");
            System.out.println(e.getMessage());
        }
    }

}
