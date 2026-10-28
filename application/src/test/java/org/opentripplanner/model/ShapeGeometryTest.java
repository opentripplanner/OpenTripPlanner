package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model.network.TripPattern;

public class ShapeGeometryTest {

  @Test
  public void useShapeGeometries() {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.SHAPE_DIST_GTFS);
    TimetableDataImport timetableDataImport = model.timetableDataImport();

    // data includes 2 trips, of which one has incorrect shape_dist_traveled parametrization in stop_times
    for (TripPattern pattern : timetableDataImport.getAllTripPatterns()) {
      // examine if shape extraction worked at first stop intervals, where dist parameter is wrong
      var tripPatternGeometry = model.shapeDataImport().getTripPatternGeometry(pattern.getId());
      assertNotNull(tripPatternGeometry);
      assertTrue(tripPatternGeometry.get(0).getCoordinates().length > 2);
      assertTrue(tripPatternGeometry.get(1).getCoordinates().length > 2);
    }
  }
}
