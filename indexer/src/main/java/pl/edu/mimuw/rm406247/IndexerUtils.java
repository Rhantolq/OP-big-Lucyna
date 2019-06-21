/** Copyright (c) Robert Michna
 * rm406247@students.mimuw.edu.pl
 */
package pl.edu.mimuw.rm406247;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

public class IndexerUtils {

    private static Path indexPath = null;
    private static PerFieldAnalyzerWrapper defaultAnalyzer = null;
    public static final String[] languages = {"pl", "en"};

    public static Path indexPath() {
        if (indexPath == null) {
            indexPath = Paths.get(System.getProperty("user.home")
                    + File.separator
                    + ".index"
                    + File.separator);
        }
        return indexPath;
    }

    public static Analyzer defaultAnalyzer() throws IOException{
        if (defaultAnalyzer == null) {
            Analyzer titleAnalyzer;
            titleAnalyzer = CustomAnalyzer.builder()
                    .withTokenizer(StandardTokenizerFactory.class)
                    .addTokenFilter(LowerCaseFilterFactory.class)
                    .build();

            Map<String, Analyzer> analyzerMap = new TreeMap<>();
            analyzerMap.put("body-en", new EnglishAnalyzer());
            analyzerMap.put("body-pl", new PolishAnalyzer());
            for (String lang : languages) {
                analyzerMap.put("title-" + lang, titleAnalyzer);
            }
            defaultAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), analyzerMap);
        }
        return defaultAnalyzer;
    }

    public static boolean argCheck(String[] args, int pos, String expected, int total_args) {
        if (total_args == args.length) {
            return args[pos].equals(expected);
        }
        else {
            return false;
        }
    }
}
