package org.opentripplanner.framework.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.framework.lang.ObjectUtils;
import org.opentripplanner.framework.lang.StringUtils;

/**
 * This class is responsible for creating a pretty table that can be printed to a terminal window.
 * Like this:
 * <pre>
 * Description  | Duration | Walk | Start time |  End  | Modes
 * Case one     |    34:13 | 1532 |      08:07 | 08:41 |  BUS
 * Case another |    26:00 |  453 |      08:29 | 08:55 |  BUS
 * </pre>
 */
public class Table {

  public static final String EMPTY_STRING = "";

  private static final char SPACE = ' ';

  private final List<Align> aligns;
  private final List<String> headers;
  private final List<Integer> widths;
  private final List<List<String>> rows = new ArrayList<>();

  Table(TableBuilder builder) {
    this.headers = List.copyOf(builder.headers());
    this.aligns = List.copyOf(builder.aligns());
    this.widths = builder.calculateWidths();
    this.rows.addAll(builder.rows());

    int nColumns = nColumns();
    assertSize(this.aligns, nColumns);
    assertSize(this.widths, nColumns);
    this.rows.forEach(row -> assertSize(row, nColumns));
  }

  /**
   * Use this factory method to create a table builder. Add headers, alignment and the use the
   * {@link TableBuilder#addRow(Object...)} to add rows.
   */
  public static TableBuilder of() {
    return new TableBuilder();
  }

  /**
   * Static method which format a given table as valid Markdown table like:
   * <pre>
   * | A | B |
   * |---|---|
   * | 1 | 2 |
   * </pre>
   * <ul>
   *   <li>Any 'null' values are converted to empty strings.</li>
   *   <li>Any '|' are escaped with '\'</li>
   * </ul>
   */
  public List<String> toMarkdownRows() {
    var list = new ArrayList<String>();
    var f = TableRowFormatter.markdownFormatter(aligns, widths);

    list.add(f.format(headers));
    list.add(f.markdownHeaderLine());

    for (var row : rows) {
      list.add(f.format(row));
    }
    return list;
  }

  /**
   * Join all rows into a table. See {@link #toMarkdownRows()}
   */
  public String toMarkdownTable() {
    return String.join("\n", toMarkdownRows()) + "\n";
  }

  /**
   * This method convert the table to a list of rows including a header row.
   * <p>
   * It formats each row using the maximum width of all elements in each column:
   * <pre>
   * Input:
   * [
   *   ["A", "B", "Total"],
   *   ["100", "2", "102"]
   * ]
   * Result:
   * [
   *   "  A | B | Total",
   *   "100 | 2 |   102"
   * ]
   * </pre>
   * All columns are justified according to the given alignment.
   */
  public List<String> toTextRows() {
    var list = new ArrayList<String>();
    var f = TableRowFormatter.logFormatter(aligns, widths);
    list.add(f.format(headers));
    for (var row : rows) {
      list.add(f.format(row));
    }
    return list;
  }

  public String headerRow() {
    return TableRowFormatter.logFormatter(aligns, widths).format(headers);
  }

  public String rowAsText(Object... row) {
    var list = normalizeRow(Arrays.asList(row), nColumns());
    return TableRowFormatter.logFormatter(aligns, widths).format(list);
  }

  /**
   * Return table as text. See {@link #toTextRows()}.
   */
  @Override
  public String toString() {
    return String.join("\n", toTextRows()) + "\n";
  }

  /* private methods */

  private void assertSize(List<?> list, int size) {
    if (list.size() != size) {
      throw new IllegalStateException(
        "The number of columns is for headers, aligns and width must be the same."
      );
    }
  }

  private int nColumns() {
    return headers.size();
  }

  /**
   * Convert a list of objects to a list of Strings with the same length as the header row.
   */
  static List<String> normalizeRow(Collection<?> row, int nColumns) {
    var list = new ArrayList<>(
      row
        .stream()
        .map(ObjectUtils::toString)
        .map(it -> it.replace('\n', ' '))
        .map(String::trim)
        .toList()
    );
    while (list.size() < nColumns) {
      list.add(EMPTY_STRING);
    }
    return list;
  }

  public enum Align {
    Left {
      String pad(String value, int width) {
        return StringUtils.padRight(value, SPACE, width);
      }
    },
    Center {
      String pad(String value, int width) {
        return StringUtils.padBoth(value, SPACE, width);
      }
    },
    Right {
      String pad(String value, int width) {
        return StringUtils.padLeft(value, SPACE, width);
      }
    };

    abstract String pad(String value, int width);
  }
}
