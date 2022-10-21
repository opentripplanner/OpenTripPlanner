package org.opentripplanner.generate.doc.framework;

public class DocFormatter {

  private static final char NBSP = '\u00A0';
  private static final String INDENT_NBSP = "" + NBSP + NBSP + NBSP + NBSP;

  /** Return the given input as emphasise text. */
  public static String em(String text) {
    return "<em>" + text + "</em>";
  }

  /** Return the given input as bold text. */
  public static String bold(Object text) {
    return text == null ? "" : "<b>" + text + "</b>";
  }

  /** Return the given input formatted as an inline code fragment. */
  public static String code(Object text) {
    return text == null ? "" : "`" + text + "`";
  }

  /**
   * Link to a header in the same document. The "other" element need to be tagged with an
   * "id" attribute equals to the given anchor.
   */
  public static String linkToAnchor(String text, String anchor) {
    return "[%s](#%s)".formatted(text, anchor);
  }

  /**
   * Link to a header in the same document. The "other" element need to be tagged with an
   * "id" attribute equals to the given anchor.
   */
  public static String linkToDoc(String text, String url) {
    return "[%s](%s)".formatted(text, url);
  }

  /** Return a check mark if true, or unchecked id false. */
  public static String checkMark(boolean enable) {
    return enable ? "✓️" : "";
  }

  /** Return the given text escaped, so it is safe to put it in a table cell. */
  public static String escapeInTable(String text) {
    return text == null ? null : text.replace("|", "\\|");
  }

  /** Return whitespace witch can be used to indent inside a table cell. */
  public static String indentInTable() {
    return INDENT_NBSP;
  }
}
