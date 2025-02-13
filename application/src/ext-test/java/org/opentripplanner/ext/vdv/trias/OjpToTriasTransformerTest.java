package org.opentripplanner.ext.vdv.trias;

import org.junit.jupiter.api.Test;
import org.opentripplanner.test.support.ResourceLoader;

class OjpToTriasTransformerTest {

  @Test
  void transform() {
    var xmlString = ResourceLoader.of(this).fileToString("stop-event-request.xml");
    OjpToTriasTransformer.readTrias(xmlString);
  }
}
