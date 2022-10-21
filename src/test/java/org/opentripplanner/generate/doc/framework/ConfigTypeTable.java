package org.opentripplanner.generate.doc.framework;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.config.framework.json.ConfigType;
import org.opentripplanner.util.lang.TableFormatter;

@SuppressWarnings("NewClassNamingConvention")
public class ConfigTypeTable {

  private static final String NEW_LINE = "\n";

  public static String configTypeTable() {
    List<List<?>> list = new ArrayList<>();
    list.add(List.of("Type", "Description", "Examples"));

    for (var it : ConfigType.values()) {
      list.add(List.of(DocFormatter.code(it.docName()), it.description(), it.examplesToMarkdown()));
    }
    var rows = TableFormatter.asMarkdownTable(list);
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
