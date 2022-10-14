package org.opentripplanner.standalone.config.framework.doc;

import java.io.PrintStream;
import java.util.List;
import org.opentripplanner.util.lang.StringUtils;
import org.opentripplanner.util.lang.TableFormatter;

/**
 * This class is responsible for producing the markdown to format a document properly.
 * This of cause make the rest of the code cleaner, but also make it easier to support other
 * fomats later, not just markdown.
 */
@SuppressWarnings("UnusedReturnValue")
public class MarkDownDocWriter {

  private static final char NBSP = '\u00A0';
  private static final String INDENT_NBSP = "\u00A0\u00A0\u00A0\u00A0";

  private final PrintStream out;

  public MarkDownDocWriter(PrintStream out) {
    this.out = out;
  }

  /** Return the given input as emphasise text. */
  public String em(String text) {
    return "*" + text + "*";
  }

  /** Return the given input formatted as an inline code fragment. */
  public String code(String text) {
    return text == null ? "" : '`' + text + '`';
  }

  /** Return a check mark if true, or unchecked id false. */
  public String checkMark(boolean enable) {
    return enable ? "✓️" : "";
  }

  /** Return the given text escaped, so it is safe to put it in a table cell. */
  public String escapeInTable(String text) {
    return text == null ? null : text.replace("|", "\\|");
  }

  /** Return whitespace witch can be used to indent inside a table cell. */
  public String indentInTable() {
    return INDENT_NBSP;
  }

  static String contextIndented(String contextPath) {
    if (contextPath == null) {
      return "";
    }
    int count = contextPath.chars().filter(c -> c == '.').sum() + 1;
    return StringUtils.pad(new StringBuilder(), NBSP, 3 * count).toString();
  }

  public void printNewLine() {
    out.println();
  }

  public void printSection(String section) {
    out.println(section.trim());
    printNewLine();
  }

  public void printDocTitle(String title) {
    out.println("# " + title);
    printNewLine();
  }

  public void printHeader1(String header) {
    printNewLine();
    out.println("## " + header);
    printNewLine();
  }

  public void printHeader2(String header) {
    printNewLine();
    out.println("### " + header);
    printNewLine();
  }

  public void printHeader3(String header) {
    printNewLine();
    out.println("#### " + header);
    printNewLine();
  }

  public void printHeader4(String header) {
    printNewLine();
    out.println("##### " + header);
    printNewLine();
  }

  public void printTable(List<List<?>> table) {
    for (String row : TableFormatter.asMarkdownTable(table)) {
      out.println(row);
    }
    printNewLine();
  }

  public String linkHeader(String text, String linkToHeader) {
    return "[%s](#%s)".formatted(text, linkToHeader);
  }
}
