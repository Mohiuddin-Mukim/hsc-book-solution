package bd.hscsolution.student.ui;

import static org.junit.Assert.*;
import org.junit.Test;

public class ReaderHtmlTest {
    @Test public void escapesRawHtml() {
        String out = ReaderHtml.escapeAndFormat("<script>alert(1)</script>");
        assertFalse(out.contains("<script>"));
        assertTrue(out.contains("&lt;script&gt;"));
    }
    @Test public void detectsMath() {
        assertTrue(ReaderHtml.containsMath("x^2 + \\(y\\)"));
        assertTrue(ReaderHtml.containsMath("$$x^2$$"));
        assertFalse(ReaderHtml.containsMath("Bangla text"));
    }
}
