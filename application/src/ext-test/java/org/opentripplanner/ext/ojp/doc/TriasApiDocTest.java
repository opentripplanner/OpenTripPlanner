package org.opentripplanner.ext.ojp.doc;

import org.junit.jupiter.api.Test;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;

@GeneratesDocumentation
public class TriasApiDocTest {

  @Test
  public void update() {
    new DocTest("TriasApi.md", "trias-config.json", "triasApi").build();
  }
}
