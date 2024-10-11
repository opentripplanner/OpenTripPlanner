package org.opentripplanner.osm.wayproperty.specifier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OsmSpecifierTest extends SpecifierTest {

  Condition[] wildcardSpec = OsmSpecifier.parseConditions("highway=*", ";");

  @Test
  public void parseWildcardAsPresent() {
    var expected = new Condition[] { new Condition.Present("highway") };
    Assertions.assertArrayEquals(expected, wildcardSpec);
  }
}
