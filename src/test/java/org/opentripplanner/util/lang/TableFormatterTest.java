package org.opentripplanner.util.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.util.lang.TableFormatter.Align.Center;
import static org.opentripplanner.util.lang.TableFormatter.Align.Left;
import static org.opentripplanner.util.lang.TableFormatter.Align.Right;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TableFormatterTest {

  @Test
  public void buildAndPrintTable() {
    String expect =
      """
      LEFT |   CENTER   | RIGHT
      AAA  | Long-value |     2
      BB   |    Short   |    12
      """;

    TableFormatter table = new TableFormatter(
      List.of(Left, Center, Right),
      List.of("LEFT", "CENTER", "RIGHT")
    );
    table.addRow("AAA", "Long-value", 2);
    table.addRow("BB", "Short", 12);
    assertEquals(expect, table.toString());
  }

  @Test
  public void printTableWhileGoing() {
    TableFormatter table = new TableFormatter(
      List.of(Left, Center, Right),
      List.of("LEFT", "CENTER", "RIGHT"),
      5,
      10,
      0
    );
    assertEquals("LEFT  |   CENTER   | RIGHT", table.printHeader());
    assertEquals("AAA   | Long-value |     2", table.printRow("AAA", "Long-value", 2));
    assertEquals("BB    |    Short   |    12", table.printRow("BB", "Short", 12));
  }

  @Test
  public void simpleTableFormatted() {
    List<List<?>> input = List.of(List.of("A", "B", "Total"), List.of(100, 2, 102));

    var table = TableFormatter.formatTableAsTextLines(input, " | ", false);
    assertEquals("  A | B | Total", table.get(0));
    assertEquals("100 | 2 |   102", table.get(1));
    assertEquals(2, table.size());
  }

  @Test
  public void tableAsMarkdown() {
    // Use a regular ArrayList to be able to insert null
    var lastRow = new ArrayList<String>();
    lastRow.add("(A|B)");
    lastRow.add(null);
    lastRow.add("X");

    List<List<?>> input = List.of(List.of("A", "B", "Total"), List.of(100, 2, 102), lastRow);

    var table = TableFormatter.asMarkdownTable(input);
    assertEquals("| A      | B | Total |", table.get(0));
    assertEquals("|--------|---|-------|", table.get(1));
    assertEquals("| 100    | 2 | 102   |", table.get(2));
    assertEquals("| (A\\|B) |   | X     |", table.get(3));
    assertEquals(4, table.size());
  }
}
