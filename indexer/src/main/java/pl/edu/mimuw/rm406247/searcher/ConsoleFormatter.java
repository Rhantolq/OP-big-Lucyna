/** Copyright (c) Robert Michna
 * rm406247@students.mimuw.edu.pl
 */
package pl.edu.mimuw.rm406247.searcher;

import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.TokenGroup;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class ConsoleFormatter implements Formatter {

    final int color;

    public ConsoleFormatter(int color) {
        this.color = color;
    }

    @Override
    public String highlightTerm(String originalText, TokenGroup tokenGroup) {
        if (tokenGroup.getTotalScore() <= 0) {
            return originalText;
        }
        return new AttributedStringBuilder()
                .append("")
                .style(AttributedStyle.DEFAULT.foreground(color))
                .append(originalText)
                .toAnsi();
    }
}
