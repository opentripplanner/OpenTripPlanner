package org.opentripplanner.generate.doc.framework;

import static org.opentripplanner.framework.text.MarkdownFormatter.normalizeAnchor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import javax.annotation.Nullable;
import org.opentripplanner.framework.text.Table;
import org.opentripplanner.util.lang.StringUtils;

/**
 * This class is responsible for producing the markdown to format a document properly.
 * This of cause make the rest of the code cleaner, but also make it easier to support other
 * fomats later, not just markdown.
 */
@SuppressWarnings("UnusedReturnValue")
public class MarkDownDocWriter {

  private static final char NBSP = '\u00A0';

  private final PrintStream out;

  public MarkDownDocWriter(PrintStream out) {
    this.out = out;
  }

  public static MarkDownDocWriter create(File outputFilename) {
    try {
      return new MarkDownDocWriter(new PrintStream(new FileOutputStream(outputFilename)));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  static String contextIndented(String contextPath) {
    if (contextPath == null) {
      return "";
    }
    int count = (int) contextPath.chars().filter(c -> c == '.').count() + 1;
    return StringUtils.fill(NBSP, 3 * count);
  }

  public void printNewLine() {
    out.println();
  }

  public void printSection(String section) {
    if (section == null) {
      return;
    }
    out.println(section.trim());
    printNewLine();
  }

  public void printDocTitle(String title) {
    header(1, title, null);
    printNewLine();
  }

  public void printHeader1(String header) {
    header(2, header, header);
  }

  public void printHeader2(String header, @Nullable String anchor) {
    header(3, header, anchor);
  }

  public void printHeader3(String header, String anchor) {
    header(4, header, anchor);
  }

  public void printTable(Table table) {
    for (String row : table.toMarkdownRows()) {
      out.println(row);
    }
    printNewLine();
  }

  private void header(int level, String header, @Nullable String anchor) {
    printNewLine();
    if (anchor != null && !anchor.isBlank()) {
      out.printf("<h%d id=\"%s\">%s</h%d>%n", level, normalizeAnchor(anchor), header, level);
    } else {
      out.printf("<h%d>%s</h%d>%n", level, header, level);
    }
    printNewLine();
  }
}
