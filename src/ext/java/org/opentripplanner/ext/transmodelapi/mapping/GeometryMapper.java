package org.opentripplanner.ext.transmodelapi.mapping;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.ext.transmodelapi.model.util.EncodedPolylineBeanWithStops;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.util.PolylineEncoder;

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
      var geometry = PolylineEncoder.encodeGeometry(tripPattern.getHopGeometry(i));

      var stopToStopGeometry = new EncodedPolylineBeanWithStops();
      stopToStopGeometry.setFromQuay(startLocation);
      stopToStopGeometry.setToQuay(endLocation);
      stopToStopGeometry.setEncodedPolylineBean(geometry);

      stopToStopGeometries.add(stopToStopGeometry);
    }

    return stopToStopGeometries;
  }
}
