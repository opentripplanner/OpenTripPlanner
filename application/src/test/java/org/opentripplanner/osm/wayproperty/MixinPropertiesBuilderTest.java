package org.opentripplanner.osm.wayproperty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.wayproperty.specifier.BestMatchSpecifier;

class MixinPropertiesBuilderTest {

  @Test
  void walkSafety() {
    var b = MixinPropertiesBuilder.ofWalkSafety(5).build(new BestMatchSpecifier("foo=bar"));

    assertEquals(5, b.forwardProperties().walkSafety());
    assertEquals(5, b.backwardProperties().walkSafety());
    assertEquals(1, b.forwardProperties().bicycleSafety());
    assertEquals(1, b.backwardProperties().bicycleSafety());
  }

  @Test
  void bikeSafety() {
    var b = MixinPropertiesBuilder.ofBicycleSafety(5).build(new BestMatchSpecifier("foo=bar"));

    assertEquals(5, b.forwardProperties().bicycleSafety());
    assertEquals(5, b.backwardProperties().bicycleSafety());
    assertEquals(1, b.forwardProperties().walkSafety());
    assertEquals(1, b.backwardProperties().walkSafety());
  }
}
