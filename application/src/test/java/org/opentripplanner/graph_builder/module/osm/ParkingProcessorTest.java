package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.geometry.Coordinates;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.osm.wayproperty.specifier.WayTestData;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

class ParkingProcessorTest {

  private static final IntersectionVertex INTERSECTION_VERTEX =
    StreetModelForTest.intersectionVertex(1, 1);
  private static final ParkingProcessor PROCESSOR = new ParkingProcessor(
    new Graph(),
    DataImportIssueStore.NOOP,
    (n, w) -> INTERSECTION_VERTEX
  );

  @Test
  void noWheelchairParking() {
    var entity = WayTestData.parkAndRide();
    var parking = PROCESSOR.createVehicleParkingObjectFromOsmEntity(
      true,
      Coordinates.BERLIN,
      entity,
      I18NString.of("parking"),
      List.of()
    );

    assertFalse(parking.hasWheelchairAccessibleCarPlaces());
    assertNull(parking.getCapacity().getWheelchairAccessibleCarSpaces());
  }

  @Test
  void wheelchairParking() {
    var entity = WayTestData.parkAndRide();
    entity.addTag("capacity:disabled", "yes");
    var parking = PROCESSOR.createVehicleParkingObjectFromOsmEntity(
      true,
      Coordinates.BERLIN,
      entity,
      I18NString.of("parking"),
      List.of()
    );

    assertTrue(parking.hasWheelchairAccessibleCarPlaces());
    assertEquals(1, parking.getCapacity().getWheelchairAccessibleCarSpaces());
  }
}
