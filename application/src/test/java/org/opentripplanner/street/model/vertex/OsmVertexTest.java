package org.opentripplanner.street.model.vertex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.AreaStop;

class OsmVertexTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final AreaStop AREA_STOP1 = TEST_MODEL.areaStop("flex-zone-1").build();
  private static final AreaStop AREA_STOP2 = TEST_MODEL.areaStop("flex-zone-2").build();

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
