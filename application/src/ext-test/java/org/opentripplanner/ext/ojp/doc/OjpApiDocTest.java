package org.opentripplanner.ext.ojp.doc;

import org.junit.jupiter.api.Test;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;

@GeneratesDocumentation
public class OjpApiDocTest {

  @Test
  public void update() {
    new DocTest("OjpApi.md", "ojp-config.json", "ojpApi").build();
  }
}
