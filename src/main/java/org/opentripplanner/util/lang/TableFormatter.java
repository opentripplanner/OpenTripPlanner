package org.opentripplanner.util.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is responsible for creating a pretty table that can be printed to a terminal window.
 * Like this:
 * <pre>
 * Description  | Duration | Walk | Start time |  End  | Modes
 * Case one     |    34:13 | 1532 |      08:07 | 08:41 |  BUS
 * Case another |    26:00 |  453 |      08:29 | 08:55 |  BUS
 * </pre>
 */
public class TableFormatter {

  private static final char SPACE = ' ';

  private final List<Align> aligns = new ArrayList<>();
  private final List<String> headers = new ArrayList<>();
  private final List<Integer> widths = new ArrayList<>();
  private final List<List<String>> rows = new ArrayList<>();

  /**
   * Use this constructor to create a table with headers and the use the {@link #addRow(Object...)}
   * to add rows to the table. The column widths will be calculated based on the data in the table.
   */
  public TableFormatter(Collection<Align> aligns, Collection<String> headers) {
    if (aligns.size() != headers.size()) {
      throw new IllegalArgumentException();
    }
    this.aligns.addAll(aligns);
    this.headers.addAll(headers);
    this.headers.forEach(it -> widths.add(it.length()));
  }

  /**
   * Use this constructor to create a table with headers and fixed width columns. This is useful if
   * you want to print header and row during computation and can not hold the entire table in memory
   * until all values are added.
   * <p>
   * Use the {@code print} methods to return each line.
   */
  public TableFormatter(Collection<Align> aligns, Collection<String> headers, int... widths) {
    if (aligns.size() != headers.size() || aligns.size() != widths.length) {
      throw new IllegalArgumentException();
    }
    this.aligns.addAll(aligns);
    this.headers.addAll(headers);
    for (int i = 0; i < widths.length; i++) {
      int width = Math.max(this.headers.get(i).length(), widths[i]);
      this.widths.add(width);
    }
  }

  /**
   * Static method witch format a given table as valid Markdown table like:
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
  public static List<String> asMarkdownTable(List<List<?>> table) {
    final int nColumns = table.get(0).size();
    // Ad an extra row to make room for the horizontal divider: | -- | -- |
    final int nRows = table.size() + 1;
    var buf = new ArrayList<StringBuilder>();
    for (int i = 0; i < nRows; ++i) {
      buf.add(new StringBuilder("|"));
    }

    for (int colIndex = 0; colIndex < nColumns; ++colIndex) {
      // Use Array list to allow for 'null' values
      var col = extractColumn(table, colIndex)
        .map(TableFormatter::escapeMarkdownTableCell)
        .toList();

      var width = findMaxWidth(col);

      for (int rowIndex = 0; rowIndex < nRows; ++rowIndex) {
        var row = buf.get(rowIndex);
        // Allow for an extra space on each side; Hence: 'width + 2'
        int newLength = row.length() + width + 2;

        if (rowIndex == 1) {
          // Build horizontal ruler like '| --- | --- | ------ |'
          StringUtils.pad(row, '-', newLength);
        } else {
          // Get row 0, skip horizontal ruler for rowIndex > 1
          int i = rowIndex - (rowIndex == 0 ? 0 : 1);
          var text = col.get(i);
          StringUtils.pad(row.append(SPACE).append(text), SPACE, newLength);
        }
        row.append("|");
      }
    }
    return buf.stream().map(Objects::toString).toList();
  }

  /**
   * This static method take a table as input and format each row in equally wide columns. It is a
   * simpler alternative to the {@link TableFormatter} class.
   * <p>
   * It formats each row with identical width using the maximum width of all elements in that
   * column:
   * <pre>
   * Input:
   * [
   *   ["A", "B", "Total"],
   *   ["100", "2", "102"]
   * ]
   * Result (leftJustify=false, colSep=" | "):
   * [
   *   "  A | B | Total",
   *   "100 | 2 |   102"
   * ]
   * </pre>
   * All columns are left or right justified, depending on the given {@code leftJustify} input
   * variable.
   */
  public static List<String> formatTableAsTextLines(
    List<List<?>> table,
    String colSep,
    boolean leftJustify
  ) {
    final int nColumns = table.get(0).size();
    var buf = table.stream().map(i -> new StringBuilder()).toList();

    for (int colIndex = 0; colIndex < nColumns; ++colIndex) {
      var col = extractColumn(table, colIndex).toList();
      int width = findMaxWidth(col);
      var f = (leftJustify ? "%-" : "%") + width + "s";

      for (int rowIndex = 0; rowIndex < buf.size(); ++rowIndex) {
        if (colIndex > 0) {
          buf.get(rowIndex).append(colSep);
        }
        buf.get(rowIndex).append(f.formatted(col.get(rowIndex)));
      }
    }
    return buf.stream().map(Objects::toString).toList();
  }

  public String printHeader() {
    StringBuilder buf = new StringBuilder();
    appendRow(buf, headers, this::padHeader);
    return buf.toString();
  }

  public String printRow(Object... row) {
    StringBuilder buf = new StringBuilder();
    appendRow(buf, toStrings(row), this::pad);
    return buf.toString();
  }

  public void addRow(Object... row) {
    addRow(Arrays.stream(row).collect(Collectors.toList()));
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();

    appendRowLn(buf, headers, this::padHeader);

    for (List<String> row : rows) {
      appendRowLn(buf, row, this::pad);
    }
    return buf.toString();
  }

  private static List<String> toStrings(Collection<?> row) {
    return row.stream().map(it -> it == null ? "" : it.toString()).collect(Collectors.toList());
  }

  /* private methods */

  private static List<String> toStrings(Object... row) {
    return Arrays
      .stream(row)
      .map(it -> it == null ? "" : it.toString())
      .collect(Collectors.toList());
  }

  private void addRow(Collection<?> row) {
    assertRowIsLessThanOrSameSizeAsHeader(row);
    List<String> aRow = toStrings(row);
    setColumnWidths(aRow);
    rows.add(aRow);
  }

  private void appendRow(
    StringBuilder buf,
    List<String> row,
    BiFunction<List<String>, Integer, String> pad
  ) {
    for (int i = 0; i < row.size(); ++i) {
      if (i != 0) {
        buf.append(" | ");
      }
      buf.append(pad.apply(row, i));
    }
  }

  private void appendRowLn(
    StringBuilder buf,
    List<String> row,
    BiFunction<List<String>, Integer, String> pad
  ) {
    appendRow(buf, row, pad);
    buf.append('\n');
  }

  private String pad(List<String> row, int i) {
    return aligns.get(i).pad(row.get(i), widths.get(i));
  }

  private String padHeader(List<String> row, int i) {
    return headerAlignment(i).pad(row.get(i), widths.get(i));
  }

  private Align headerAlignment(int i) {
    return aligns.get(i) == Align.Left ? Align.Left : Align.Center;
  }

  private void setColumnWidths(List<String> row) {
    for (int i = 0; i < row.size(); ++i) {
      widths.set(i, Math.max(widths.get(i), row.get(i).length()));
    }
  }

  /**
   * Extract a column from a table of rows. This method will also transform each cell to
   * a string using {@code toString}, any 'null' values are converted to an empty string.
   */
  private static Stream<String> extractColumn(List<List<?>> table, final int columnIndex) {
    return table.stream().map(r -> r.get(columnIndex)).map(o -> o == null ? "" : o.toString());
  }

  /**
   * Find the maximum width for the given set of cells. Returns zero(0) if the given list of cells
   * are empty.
   */
  private static int findMaxWidth(List<String> cells) {
    return cells.stream().mapToInt(String::length).max().orElse(0);
  }

  /**
   * Pipes '|' in the text cell in a Markdown table will cause the table to be rendered wrong,
   * so we must escape '|' in the text.
   */
  private static String escapeMarkdownTableCell(String text) {
    return text == null ? null : text.replace("|", "\\|");
  }

  private void assertRowIsLessThanOrSameSizeAsHeader(Collection<?> row) {
    if (row.size() > headers.size()) {
      throw new IllegalArgumentException(
        "Can not add row with more columns than the header. " +
        "Row size: " +
        row.size() +
        ", Header size: " +
        headers.size()
      );
    }
  }

  public enum Align {
    Left {
      String pad(String value, char ch, int width) {
        return StringUtils.pad(new StringBuilder(value), ch, width).toString();
      }
    },
    Center {
      String pad(String value, char ch, int width) {
        if (value.length() >= width) {
          return value;
        }
        var buf = new StringBuilder();
        StringUtils.pad(buf, ch, (width + 1 - value.length()) / 2);
        buf.append(value);
        StringUtils.pad(buf, ch, width);
        return buf.toString();
      }
    },
    Right {
      String pad(String value, char ch, int width) {
        return StringUtils
          .pad(new StringBuilder(), ch, width - value.length())
          .append(value)
          .toString();
      }
    };

    abstract String pad(String value, char ch, int width);

    String pad(String value, int width) {
      return pad(value, SPACE, width);
    }
  }
}
