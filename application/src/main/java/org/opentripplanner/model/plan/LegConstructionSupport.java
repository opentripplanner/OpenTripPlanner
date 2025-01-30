package org.opentripplanner.model.plan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * Utility methods for constructing legs.
 */
public class LegConstructionSupport {

  /**
   * Given a pattern, board and alight stop index compute the distance in meters.
   */
  public static double computeDistanceMeters(
    TripPattern tripPattern,
    int boardStopIndexInPattern,
    int alightStopIndexInPattern
  ) {
    List<Coordinate> transitLegCoordinates = extractTransitLegCoordinates(
      tripPattern,
      boardStopIndexInPattern,
      alightStopIndexInPattern
    );
    return GeometryUtils.sumDistances(transitLegCoordinates);
  }

  /**
   * Given a pattern, board and alight stop index compute the list of coordinates that this
   * segment of the pattern visits.
   */
  public static List<Coordinate> extractTransitLegCoordinates(
    TripPattern tripPattern,
    int boardStopIndexInPattern,
    int alightStopIndexInPattern
  ) {
    List<Coordinate> transitLegCoordinates = new ArrayList<>();

    for (int i = boardStopIndexInPattern + 1; i <= alightStopIndexInPattern; i++) {
      transitLegCoordinates.addAll(
        Arrays.asList(tripPattern.getHopGeometry(i - 1).getCoordinates())
      );
    }

    return transitLegCoordinates;
  }
}
