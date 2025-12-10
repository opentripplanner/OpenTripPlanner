package org.opentripplanner.street.model.vertex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;

class OsmVertexTest {

  private static final FeedScopedId AREA_STOP1 = id("flex-zone-1");
  private static final FeedScopedId AREA_STOP2 = id("flex-zone-2");

  @Test
  void areaStops() {
    var vertex = vertex();
    assertNotNull(vertex.areaStops());
    assertTrue(vertex.areaStops().isEmpty());
  }

  @Test
  void addAreaStop() {
    var vertex = vertex();
    vertex.addAreaStops(List.of(AREA_STOP1));
    assertNotNull(vertex.areaStops());
    assertEquals(1, vertex.areaStops().size());

    vertex.addAreaStops(List.of(AREA_STOP1));
    assertEquals(1, vertex.areaStops().size());

    vertex.addAreaStops(List.of(AREA_STOP2));
    assertEquals(2, vertex.areaStops().size());
  }

  private static OsmVertex vertex() {
    return new OsmVertex(1, 2, 1);
  }
}
