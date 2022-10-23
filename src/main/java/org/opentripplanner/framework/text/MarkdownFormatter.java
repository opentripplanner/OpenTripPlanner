package org.opentripplanner.framework.text;

import javax.annotation.Nullable;

/**
 * This utility can be used to format Markdown text. This is useful when generating documentation.
 */
public class MarkdownFormatter {

  private static final char NBSP = '\u00A0';
  private static final String INDENT_NBSP = "" + NBSP + NBSP + NBSP + NBSP;

  /** Return the given input as emphasise text. */
  public static String em(String text) {
    return "*" + text + "*";
  }

  /** Return the given input as bold text. */
  public static String bold(Object text) {
    return text == null ? "" : "**" + text + "**";
  }

  /** Return the given input formatted as an inline code fragment. */
  public static String code(@Nullable Object text) {
    return text == null ? "" : "`" + text + "`";
  }

  /** Return the given input formatted as an inline code fragment. */
  public static String quote(@Nullable Object text) {
    return text == null ? "" : "\"" + text + "\"";
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

  /**
   * Pipes '|' in the text in a Markdown table cell will cause the table to be rendered wrong,
   * so we must escape '|' in the text.
   */
  public static String escapeInTable(String text) {
    return text == null ? null : text.replace("|", "\\|");
  }

  /** Return whitespace witch can be used to indent inside a table cell. */
  public static String indentInTable() {
    return INDENT_NBSP;
  }
}
