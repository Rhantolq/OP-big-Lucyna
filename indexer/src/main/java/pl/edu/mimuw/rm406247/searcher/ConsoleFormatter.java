package pl.edu.mimuw.rm406247.searcher;

import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.TokenGroup;

public class ConsoleFormatter implements Formatter {

    final String prefix, suffix;

    public ConsoleFormatter(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    @Override
    public String highlightTerm(String originalText, TokenGroup tokenGroup) {
        if (tokenGroup.getTotalScore() <= 0) {
            return originalText;
        }
        return prefix + originalText + suffix;
    }
}
