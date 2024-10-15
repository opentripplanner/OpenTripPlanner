package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.TransitModel;

public class ShapeGeometryTest {

  @Test
  public void useShapeGeometries() {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.SHAPE_DIST_GTFS);
    TransitModel transitModel = model.transitModel();

    // data includes 2 trips, of which one has incorrect shape_dist_traveled parametrization in stop_times
    for (TripPattern pattern : transitModel.getAllTripPatterns()) {
      // examine if shape extraction worked at first stop intervals, where dist parameter is wrong
      assertTrue(pattern.getHopGeometry(0).getCoordinates().length > 2);
      assertTrue(pattern.getHopGeometry(1).getCoordinates().length > 2);
    }
  }
}
