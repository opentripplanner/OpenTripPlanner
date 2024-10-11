package org.opentripplanner.framework.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Use the builder to create a Table.
 */
public class TableBuilder {

  private List<Table.Align> aligns = null;
  private List<String> headers = null;
  private List<Integer> minWidths = null;
  private final List<List<String>> rows = new ArrayList<>();

  List<String> headers() {
    return headers;
  }

  /**
   * Set the header row for the table.
   */
  public TableBuilder withHeaders(String... headers) {
    return withHeaders(Arrays.asList(headers));
  }

  public TableBuilder withHeaders(Collection<String> headers) {
    this.headers = new ArrayList<>(headers);
    return this;
  }

  List<Table.Align> aligns() {
    // if no alightment is set, default to left aligned
    return aligns != null ? aligns : headers.stream().map(it -> Table.Align.Left).toList();
  }

  /**
   * Set alignments for each column.
   */
  public TableBuilder withAlights(Table.Align... aligns) {
    return withAlights(Arrays.asList(aligns));
  }

  public TableBuilder withAlights(Collection<Table.Align> aligns) {
    this.aligns = List.copyOf(aligns);
    return this;
  }

  /**
   * Return the width needed for each column. The which is calculated by taking
   * the maximum of the {@code minWidth}, header width and the maximum width for all
   * cells in the column.
   */
  List<Integer> calculateWidths() {
    var widths = new ArrayList<Integer>(numberOfColumns());
    for (int i = 0; i < numberOfColumns(); i++) {
      widths.add(findMaxWidth(i));
    }
    return widths;
  }

  /**
   * Set minimum width for each column. This is not necessary if all values are added to the
   * table before printing it. But, if the table is used to format e.g. log lines and created
   * before the logging start, then you can set the minimum column widths. If the header is wider
   * the width of the header is used. If a cell is wider than the width used, then the cell is
   * expanded to fit the content - the row will not match the header.
   */
  public TableBuilder withMinWidths(int... widths) {
    return withMinWidths(IntStream.of(widths).boxed().toList());
  }

  public TableBuilder withMinWidths(Collection<Integer> widths) {
    this.minWidths = List.copyOf(widths);
    return this;
  }

  public List<List<String>> rows() {
    return rows;
  }

  public TableBuilder addRow(Object... cells) {
    return addRow(Arrays.asList(cells));
  }

  public TableBuilder addRow(Collection<?> row) {
    this.rows.add(Table.normalizeRow(row, numberOfColumns()));
    return this;
  }

  public Table build() {
    return new Table(this);
  }

  @Override
  public String toString() {
    return build().toString();
  }

  /**
   * Find the maximum width for the given column.
   */
  private int findMaxWidth(int column) {
    int width0 = headers.get(column).length();
    if (minWidths != null) {
      if (minWidths.size() != numberOfColumns()) {
        throw new IllegalStateException(
          "Number minWidths does not match number of columns. MinWidths=" +
          minWidths.size() +
          ", columns=" +
          numberOfColumns()
        );
      }
      width0 = Math.max(width0, minWidths.get(column));
    }

    return rows
      .stream()
      .map(it -> it.get(column))
      .mapToInt(it -> it == null ? 0 : it.length())
      .reduce(width0, Math::max);
  }

  private int numberOfColumns() {
    return headers.size();
  }
}
