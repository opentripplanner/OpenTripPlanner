package org.opentripplanner.generate.doc.support;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.generate.doc.support.ConfigTypeTable.configTypeTable;

import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.config.framework.json.ConfigType;

public class ConfigTypeTableTest {

  @Test
  void configTypeTableTest() {
    var table = configTypeTable();
    for (ConfigType it : ConfigType.values()) {
      assertTrue(table.contains(it.docName()), table);
    }
  }
}
