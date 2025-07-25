package org.opentripplanner.osm.wayproperty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.wayproperty.specifier.BestMatchSpecifier;

class MixinPropertiesBuilderTest {

  @Test
  void walkSafety() {
    var b = MixinPropertiesBuilder.ofWalkSafety(5).build(new BestMatchSpecifier("foo=bar"));

    assertEquals(5, b.walkSafety());
    assertEquals(1, b.bicycleSafety());
  }

  @Test
  void bikeSafety() {
    var b = MixinPropertiesBuilder.ofBicycleSafety(5).build(new BestMatchSpecifier("foo=bar"));

    assertEquals(5, b.bicycleSafety());
    assertEquals(1, b.walkSafety());
  }
}
