package pl.edu.mimuw.rm406247.searcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.FSDirectory;
import pl.edu.mimuw.rm406247.IndexerUtils;


public class Searcher {

    private Searcher() {}

    private enum QueryType {
        TERM ("Term"),
        PHRASE ("Phrase"),
        FUZZY ("Fuzzy"),
        PREFIX ("Prefix");

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
        Path indexPath = IndexerUtils.indexPath();

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
        Analyzer analyzer;
        try {
             analyzer = IndexerUtils.defaultAnalyzer();
        }
        catch (IOException e) {
            System.out.println("Fatal error creating analyzer.");
            System.out.println(e.getMessage());
            System.exit(1);
            return; // to ignore uninitialized warnings.
        }
        IndexSearcher indexSearcher;
        Scanner scanner = new Scanner(System.in);

        System.out.print(">");
        while (scanner.hasNext()) {
            String command = scanner.nextLine();
            if (command.length() == 0) {
                continue;
            }
            if (command.charAt(0) == '%') {
                String[] commandArgs =  command.split(" ");
                boolean queryChange = false;
                for (QueryType type : QueryType.values()) {
                    if (commandArgs[0].equals("%" + type.toString().toLowerCase())) {
                        if (commandArgs.length > 1) {
                            System.out.println("Too many arguments for %term command.");
                            break;
                        }
                        queryType = type;
                        queryChange = true;
                    }
                }
                if (queryChange) {
                    continue;
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
                if (queryType != QueryType.PHRASE && command.contains(" ")) {
                    System.out.println(queryType.toString() + " requires");
                }
                try (FSDirectory indexDir = FSDirectory.open(indexPath)) {
                    try (IndexReader indexReader = DirectoryReader.open(indexDir)){
                        indexSearcher = new IndexSearcher(indexReader);
                        Query query;
                        Query query1 = null, query2 = null;
                        ArrayList<String> bodyTerms = analyze("body-" + lang, command, analyzer);
                        ArrayList<String> titleTerms = analyze("title-" + lang, command, analyzer);
                        String body = (bodyTerms.size() > 0) ? bodyTerms.get(0) : command.toLowerCase();
                        String title = (titleTerms.size() > 0) ? titleTerms.get(0) : command.toLowerCase();
                        System.out.println("Bodyterms " + body);
                        for (String s : bodyTerms) {
                            System.out.print("'" + s + "' ");
                        }
                        System.out.println("\nTitleterms " + title);
                        for (String s : titleTerms) {
                            System.out.print("'" + s + "' ");
                        }
                        System.out.println("\nText = " + command + ":");
                        if (queryType == QueryType.TERM) {
                            query1 = new TermQuery(new Term("body-" + lang, body));
                            query2 = new TermQuery(new Term("title-" + lang, title));
                        }
                        else if (queryType == QueryType.PHRASE) {
                            query1 = new PhraseQuery("body-" + lang, bodyTerms.toArray(new String[bodyTerms.size()]));
                            query2 = new PhraseQuery("title-" + lang, titleTerms.toArray(new String[titleTerms.size()]));
                        }
                        else if (queryType == QueryType.FUZZY) {
                            query1 = new FuzzyQuery(new Term("body-" + lang, body));
                            query2 = new FuzzyQuery(new Term("title-" + lang, title));
                        }
                        if (queryType == QueryType.PREFIX) { // additional functionality
                            query = new PrefixQuery(new Term("path", command));
                        }
                        else {
                             query = new BooleanQuery.Builder()
                                    .add(query1, BooleanClause.Occur.SHOULD)
                                    .add(query2, BooleanClause.Occur.SHOULD)
                                    .build();
                        }
                        TopDocs topDocs = indexSearcher.search(query, limit);
                        Formatter formatter;
                        if (showColors) {
                            formatter = new ConsoleFormatter("***", "***");
                        }
                        else {
                            formatter = new ConsoleFormatter("", "");
                        }
                        QueryScorer scorer = new QueryScorer(query);
                        Highlighter highlighter = new Highlighter(formatter, scorer);
                        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 70);
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
            System.out.print(">");
        }
        System.exit(0);
    }

    public static ArrayList<String> analyze(String fieldName, String text, Analyzer analyzer) throws IOException{
        ArrayList<String> result = new ArrayList<String>();
        TokenStream tokenStream = analyzer.tokenStream(fieldName, text);
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while(tokenStream.incrementToken()) {
            result.add(attr.toString());
        }
        tokenStream.close();
        return result;
    }

}
