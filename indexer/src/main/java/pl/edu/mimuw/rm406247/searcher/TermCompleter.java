package pl.edu.mimuw.rm406247.searcher;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.FSDirectory;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class TermCompleter implements Completer {

    private final Analyzer analyzer;
    private final Path indexPath;

    public TermCompleter(Path indexPath, Analyzer analyzer) {
        this.analyzer = analyzer;
        this.indexPath = indexPath;
    }

    /**
     * Populates <i>candidates</i> with a list of possible completions for the <i>command line</i>.
     *
     * The list of candidates will be sorted and filtered by the LineReader, so that
     * the list of candidates displayed to the user will usually be smaller than
     * the list given by the completer.  Thus it is not necessary for the completer
     * to do any matching based on the current buffer.  On the contrary, in order
     * for the typo matcher to work, all possible candidates for the word being
     * completed should be returned.
     *
     * @param reader        The line reader
     * @param line          The parsed command line
     * @param candidates    The {@link List} of candidates to populate
     */
    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        /*
        try (FSDirectory indexDir = FSDirectory.open(indexPath)) {
            try (IndexReader indexReader = DirectoryReader.open(indexDir)) {
                String term = line.word();
                AnalyzingInfixSuggester suggester;
                List<Lookup.LookupResult> results;
                try {
                    suggester = new AnalyzingInfixSuggester(indexDir, analyzer);
                    suggester.build(new LuceneDictionary(indexReader, "body-pl"));
                    results = suggester.lookup(term, 30, true, false);
                }
                catch (IOException e) {
                    System.err.println(e);
                    return;
                }
                for (Lookup.LookupResult result : results) {
                    System.err.println(result.key.toString());
                    candidates.add(new Candidate(result.key.toString()));
                }
            }
            catch (IOException e) {
                // ignore
                System.err.println(e);
            }
        }
        catch (IOException e) {
            // ignore
            System.err.println(e);
        }*/
    }
}