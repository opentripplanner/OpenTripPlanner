package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.opentripplanner.standalone.config.JsonSupport.jsonNodeForTest;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.impl.DefaultFareServiceImpl;

public class BuildConfigTest {

  @Test
  public void boardingLocationRefs() {
    var node = jsonNodeForTest("{ 'boardingLocationTags' : ['a-ha', 'royksopp'] }");

    var subject = new BuildConfig(node, "Test", false);

    assertEquals(Set.of("a-ha", "royksopp"), subject.boardingLocationTags);
  }

  @Test
  public void fareService() {
    var node = jsonNodeForTest("{ 'fares' : \"highestFareInFreeTransferWindow\" }");
    var conf = new BuildConfig(node, "Test", false);
    assertInstanceOf(DefaultFareServiceImpl.class, conf.fareServiceFactory.makeFareService());
  }
}
