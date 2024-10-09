package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.TransitEntranceVertex;

public class StreetTransitEntranceLinkTest {

  private static final TransitEntranceVertex ENTRANCE = StreetModelForTest.transitEntranceVertex(
    "entrance",
    1,
    2
  );
  private static final IntersectionVertex STREET_VERTEX = StreetModelForTest.intersectionVertex(
    1,
    1
  );

  @Test
  void isEntrance() {
    var edge = StreetTransitEntranceLink.createStreetTransitEntranceLink(STREET_VERTEX, ENTRANCE);

    assertTrue(edge.isEntrance());
    assertFalse(edge.isExit());
  }

  @Test
  void isExit() {
    var edge = StreetTransitEntranceLink.createStreetTransitEntranceLink(ENTRANCE, STREET_VERTEX);

    assertFalse(edge.isEntrance());
    assertTrue(edge.isExit());
  }
}
