package org.opentripplanner.street.linking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Area;

class AreaEdgePropertiesTest {

  // A flat, pedestrian-and-bicycle square and a steep, pedestrian-only "steps" tile.
  private static final Area FRIENDLY = area(
    "square",
    StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
    1.0f,
    1.0f
  );
  private static final Area STEPS = area("steps", StreetTraversalPermission.PEDESTRIAN, 8.0f, 8.0f);

  @Test
  void singleAreaKeepsItsProperties() {
    var props = AreaEdgeProperties.merge(List.of(FRIENDLY));
    assertEquals(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, props.permission());
    assertEquals(1.0f, props.walkSafety());
    assertEquals(1.0f, props.bicycleSafety());
    assertEquals("square", props.name().toString());
  }

  @Test
  void crossingTwoAreasMergesWorstCase() {
    // permission AND-ed (bike dropped), safety MAX-ed.
    var props = AreaEdgeProperties.merge(List.of(FRIENDLY, STEPS));
    assertEquals(StreetTraversalPermission.PEDESTRIAN, props.permission());
    assertEquals(8.0f, props.walkSafety());
    assertEquals(8.0f, props.bicycleSafety());
    // name comes from the first area in list order (FRIENDLY).
    assertEquals("square", props.name().toString());
  }

  private static Area area(
    String name,
    StreetTraversalPermission permission,
    float walk,
    float bike
  ) {
    var a = new Area();
    a.setName(I18NString.of(name));
    a.setPermission(permission);
    a.setWalkSafety(walk);
    a.setBicycleSafety(bike);
    return a;
  }
}
