package org.opentripplanner.street.model.vertex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TransitModelForTest.FLEX_ZONE;

import java.util.List;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.site.AreaStop;

class OsmVertexTest {

  private static final AreaStop AREA_STOP1 = TransitModelForTest.areaStopForTest(
    "flex-zone-1",
    FLEX_ZONE
  );
  private static final AreaStop AREA_STOP2 = TransitModelForTest.areaStopForTest(
    "flex-zone-2",
    FLEX_ZONE
  );

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

  @Nonnull
  private static OsmVertex vertex() {
    return new OsmVertex("test", 1, 2, 1);
  }
}
