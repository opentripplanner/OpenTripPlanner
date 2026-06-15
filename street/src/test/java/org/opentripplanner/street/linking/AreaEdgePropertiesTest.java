package org.opentripplanner.street.linking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Area;

class AreaEdgePropertiesTest {

  // A flat, pedestrian-and-bicycle, wheelchair-accessible square and a steep, pedestrian-only "steps"
  // tile that is not wheelchair accessible.
  private static final Area FRIENDLY = area(
    "square",
    StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
    1.0f,
    1.0f,
    true
  );
  private static final Area STEPS = area(
    "steps",
    StreetTraversalPermission.PEDESTRIAN,
    8.0f,
    8.0f,
    false
  );

  @Test
  void singleAreaKeepsItsProperties() {
    var props = AreaEdgeProperties.merge(List.of(FRIENDLY));
    assertEquals(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, props.permission());
    assertEquals(1.0f, props.walkSafety());
    assertEquals(1.0f, props.bicycleSafety());
    assertTrue(props.wheelchairAccessible());
    assertEquals("square", props.name().toString());
  }

  @Test
  void crossingTwoAreasMergesWorstCase() {
    // permission AND-ed (bike dropped), safety MAX-ed, wheelchair AND-ed (STEPS drops it).
    var props = AreaEdgeProperties.merge(List.of(FRIENDLY, STEPS));
    assertEquals(StreetTraversalPermission.PEDESTRIAN, props.permission());
    assertEquals(8.0f, props.walkSafety());
    assertEquals(8.0f, props.bicycleSafety());
    assertFalse(props.wheelchairAccessible());
    // name comes from the first area in list order (FRIENDLY).
    assertEquals("square", props.name().toString());
  }

  private static Area area(
    String name,
    StreetTraversalPermission permission,
    float walk,
    float bike,
    boolean wheelchairAccessible
  ) {
    var a = new Area();
    a.setName(I18NString.of(name));
    a.setPermission(permission);
    a.setWalkSafety(walk);
    a.setBicycleSafety(bike);
    a.setWheelchairAccessible(wheelchairAccessible);
    return a;
  }
}
