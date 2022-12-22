package org.opentripplanner.openstreetmap.wayproperty.specifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class OsmSpecifierTest extends SpecifierTest {
  Condition[] wildcardSpec = OsmSpecifier.parseEqualsTests("highway=*", ";");
  @Test
  public void parseWildcardAsPresent() {
    var expected = new Condition[]{new Condition.Present("highway")};
    Assertions.assertArrayEquals(expected, wildcardSpec);
  }
}
