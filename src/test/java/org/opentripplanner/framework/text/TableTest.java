package org.opentripplanner.framework.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.framework.text.Table.Align.Center;
import static org.opentripplanner.framework.text.Table.Align.Left;
import static org.opentripplanner.framework.text.Table.Align.Right;

import org.junit.jupiter.api.Test;

public class TableTest {

  @Test
  public void buildAndPrintTable() {
    String expect =
      """
      LEFT |   CENTER   | RIGHT
      AAA  | Long-value |     2
           |    Short   |   123
      """;

    var table = Table.of().withAlights(Left, Center, Right).withHeaders("LEFT", "CENTER", "RIGHT");
    table.addRow("AAA", "Long-value", 2);
    table.addRow(null, "Short", 123);
    assertEquals(expect, table.toString());
  }

  @Test
  public void printTableWhileGoing() {
    Table table = Table
      .of()
      .withAlights(Left, Center, Right)
      .withHeaders("LEFT", "CENTER", "RIGHT")
      .withMinWidths(5, 8, 0)
      .build();

    assertEquals("LEFT  |  CENTER  | RIGHT", table.headerRow());
    assertEquals("AAA   | Long-value |     2", table.rowAsText("AAA", "Long-value", 2));
    assertEquals("BB    |   Short  |    12", table.rowAsText("BB", "Short", 12));
    // Add row with just one element, column 2 and 3 is added
    assertEquals("One   |          |      ", table.rowAsText("One"));
  }

  @Test
  public void tableAsMarkdown() {
    var table = Table
      .of()
      .withHeaders("A", "B", "Total")
      .withAlights(Center, Center, Center)
      .addRow(100, 2, 102)
      .addRow("One", null)
      .addRow("(A|B)", "|")
      .build();

    var result = table.toMarkdownRows();
    assertEquals("|   A   | B | Total |", result.get(0));
    assertEquals("|:-----:|:-:|:-----:|", result.get(1));
    assertEquals("|  100  | 2 |  102  |", result.get(2));
    assertEquals("|  One  |   |       |", result.get(3));
    assertEquals("| (A\\|B) | \\| |       |", result.get(4));
    assertEquals(5, result.size());
  }

  @Test
  public void tableWithTooFewAlignsFails() {
    assertThrows(
      IllegalStateException.class,
      () -> Table.of().withHeaders("A", "B").withAlights(Center).build()
    );
  }

  @Test
  public void tableWithTooFewMinWidths() {
    assertThrows(
      IllegalStateException.class,
      () -> Table.of().withHeaders("A", "B").withMinWidths(20).build()
    );
  }
}
