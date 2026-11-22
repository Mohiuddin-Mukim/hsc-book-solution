package bd.hscsolution.student.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ReaderHtml {
    private ReaderHtml() {}

    static String escapeAndFormat(String raw) {
        if (raw == null) return "";
        String s = raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
        // Only a small Markdown policy is supported. Raw HTML and links remain inert.
        s = s.replaceAll("(?m)^###\\s+(.+)$", "<b>$1</b>")
             .replaceAll("(?m)^##\\s+(.+)$", "<b>$1</b>")
             .replaceAll("(?m)^#\\s+(.+)$", "<b>$1</b>")
             .replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>")
             .replaceAll("(?m)^[-*]\\s+(.+)$", "• $1");
        s = s.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "<br>");
        return s;
    }

    static boolean containsMath(String text) {
        if (text == null) return false;
        return text.contains("$$") || text.contains("\\(") || text.contains("\\)")
                || text.contains("\\[") || text.contains("\\]");
    }
}
