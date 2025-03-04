package org.opentripplanner.utils.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.utils.text.Table.Align.Center;
import static org.opentripplanner.utils.text.Table.Align.Left;
import static org.opentripplanner.utils.text.Table.Align.Right;

import org.junit.jupiter.api.Test;

public class TableTest {

  @Test
  public void buildAndPrintTable() {
    // Test various normalization cases
    var builder = Table.of()
      .withAlights(Left, Center, Right)
      .withHeaders("LEFT", "CENTER", "RIGHT")
      .addRow(" SPACE ", " Long-value\t", 2)
      .addRow("\nNL Before", "NL\nin middle", "NL after\n")
      .addRow(null, "Short", 123);

    var expected =
      """
      LEFT      |    CENTER    |    RIGHT
      SPACE     |  Long-value  |        2
      NL Before | NL in middle | NL after
                |     Short    |      123
      """;

    // Both the builder and the Table returns the same table as toString()
    assertEquals(expected, builder.toString());
    assertEquals(expected, builder.build().toString());
  }

  @Test
  public void printTableWhileGoing() {
    Table table = Table.of()
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
    var table = Table.of()
      .withHeaders("A", "B", "Total")
      .withAlights(Center, Center, Center)
      .addRow(100, 2, 102)
      .addRow("One", null)
      .addRow("(A|B)", "|")
      .build();

    var result = table.toMarkdownTable();

    assertEquals(
      """
      |   A   | B | Total |
      |:-----:|:-:|:-----:|
      |  100  | 2 |  102  |
      |  One  |   |       |
      | (A¦B) | ¦ |       |
      """,
      result
    );
  }

  @Test
  public void tableWithTooFewAlignsFails() {
    assertThrows(IllegalStateException.class, () ->
      Table.of().withHeaders("A", "B").withAlights(Center).build()
    );
  }

  @Test
  public void tableWithTooFewMinWidths() {
    assertThrows(IllegalStateException.class, () ->
      Table.of().withHeaders("A", "B").withMinWidths(20).build()
    );
  }
}
