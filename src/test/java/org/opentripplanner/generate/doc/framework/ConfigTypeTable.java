package org.opentripplanner.generate.doc.framework;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.text.MarkdownFormatter;
import org.opentripplanner.framework.text.Table;
import org.opentripplanner.standalone.config.framework.json.ConfigType;

@SuppressWarnings("NewClassNamingConvention")
public class ConfigTypeTable {

  private static final String NEW_LINE = "\n";

  public static String configTypeTable() {
    var tbl = Table
      .of()
      .withHeaders("Type", "Description", "Examples")
      .withAlights(Table.Align.Left, Table.Align.Left, Table.Align.Left);

    for (var it : ConfigType.values()) {
      tbl.addRow(MarkdownFormatter.code(it.docName()), it.description(), it.examplesToMarkdown());
    }
    var rows = tbl.build().toMarkdownRows();
    return String.join(NEW_LINE, rows) + NEW_LINE;
  }

  @Test
  void configTypeTableTest() {
    var table = configTypeTable();
    for (ConfigType it : ConfigType.values()) {
      assertTrue(table.contains(it.docName()), table);
    }
  }
}
