package org.opentripplanner.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.util.TableFormatter.Align.Center;
import static org.opentripplanner.util.TableFormatter.Align.Left;
import static org.opentripplanner.util.TableFormatter.Align.Right;

public class TableFormatterTest {
  @Test
  public void buildAndPrintTable() {
    String expect = ""
        + "LEFT |   CENTER   | RIGHT\n"
        + "AAA  | Long-value |     2\n"
        + "BB   |    Short   |    12\n";

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
        5, 10, 0
    );
    assertEquals("LEFT  |   CENTER   | RIGHT", table.printHeader());
    assertEquals("AAA   | Long-value |     2", table.printRow("AAA", "Long-value", 2));
    assertEquals("BB    |    Short   |    12", table.printRow("BB", "Short", 12));
  }
}