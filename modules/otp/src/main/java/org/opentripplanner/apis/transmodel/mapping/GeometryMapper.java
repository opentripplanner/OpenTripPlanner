package org.opentripplanner.apis.transmodel.mapping;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.apis.transmodel.model.util.EncodedPolylineBeanWithStops;
import org.opentripplanner.framework.geometry.EncodedPolyline;
import org.opentripplanner.transit.model.network.TripPattern;

public class GeometryMapper {

  /**
   * Based Trip Pattern, create a list of geometries for each stop-to-stop section in the pattern
   */
  public static List<EncodedPolylineBeanWithStops> mapStopToStopGeometries(
    TripPattern tripPattern
  ) {
    var stopToStopGeometries = new ArrayList<EncodedPolylineBeanWithStops>();

    for (int i = 0; i < tripPattern.numberOfStops() - 1; i++) {
      var startLocation = tripPattern.getStop(i);
      var endLocation = tripPattern.getStop(i + 1);
      var geometry = EncodedPolyline.encode(tripPattern.getHopGeometry(i));

      var stopToStopGeometry = new EncodedPolylineBeanWithStops(
        startLocation,
        endLocation,
        geometry
      );

      stopToStopGeometries.add(stopToStopGeometry);
    }

    return stopToStopGeometries;
  }
}
