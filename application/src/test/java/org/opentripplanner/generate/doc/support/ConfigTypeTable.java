package org.opentripplanner.generate.doc.support;

import org.opentripplanner.standalone.config.framework.json.ConfigType;
import org.opentripplanner.utils.text.MarkdownFormatter;
import org.opentripplanner.utils.text.Table;

@SuppressWarnings("NewClassNamingConvention")
public class ConfigTypeTable {

  private static final String NEW_LINE = "\n";

  public static String configTypeTable() {
    var tbl = Table.of()
      .withHeaders("Type", "Description", "Examples")
      .withAlights(Table.Align.Left, Table.Align.Left, Table.Align.Left);

    for (var it : ConfigType.values()) {
      tbl.addRow(MarkdownFormatter.code(it.docName()), it.description(), it.examplesToMarkdown());
    }
    var rows = tbl.build().toMarkdownRows();
    return String.join(NEW_LINE, rows) + NEW_LINE;
  }
}
