package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;

public class TripPatternTest {

  /**
   * The originalTripPattern is to consider a planned trip with 2 stops and one coordinate between.
   *
   * The tripPattern is an updated trip with just two stops, belonging to the same stations as for
   * originalTripPattern but first stop is an alternative stop.
   *
   * The scope is to create hop geometries from original trip pattern since the stops belong to the
   * same stations. The end result should be that the new tripPattern contains the altered stop but
   * all the rest of the coordinates are as from originalTripPattern
   */
  @Test
  public void testSetHopGeometriesFromPattern() {
    var stationOrigin = Station.stationForTest("S1", 0.0, 0.0);
    var stationDestination = Station.stationForTest("S2", 1.0, 1.0);
    var stopOrigin = Stop.stopForTest("A1", 0.1, 0.1);
    var stopNewOrigin = Stop.stopForTest("A2", 0.2, 0.2);
    var stopDestination = Stop.stopForTest("C", 0.9, 0.9);
    var coordinate = new Coordinate(0.5, 0.5);

    stopNewOrigin.setParentStation(stationOrigin);
    stopOrigin.setParentStation(stationOrigin);
    stopDestination.setParentStation(stationDestination);

    var originalTripPattern = setupTripPattern(stopOrigin, stopDestination);
    var newTripPattern = setupTripPattern(stopNewOrigin, stopDestination);

    // Add coordinate between stops on original trip pattern
    originalTripPattern.setHopGeometries(getLineStrings(stopOrigin, stopDestination, coordinate));

    var originalCoordinates = originalTripPattern.getHopGeometry(0).getCoordinates().length;
    var coordinates = newTripPattern.getHopGeometry(0).getCoordinates().length;

    assertEquals(
      3,
      originalCoordinates,
      "The coordinates for originalTripPattern on first hop should be 3"
    );
    assertEquals(2, coordinates, "The coordinates for tripPattern on first hop should be 2");

    // Add geometry from planned data
    newTripPattern.setHopGeometriesFromPattern(originalTripPattern);

    var finalCoordinates = newTripPattern.getHopGeometry(0).getCoordinates().length;

    assertEquals(
      originalCoordinates,
      finalCoordinates,
      "The hop on tripPattern should contain as many coordinates as the originalTripPattern"
    );
  }

  /**
   * Create TripPattern between to stops
   * @param origin Start stop
   * @param destination End stop
   * @return TripPattern with stopPattern
   */
  public TripPattern setupTripPattern(Stop origin, Stop destination) {
    var builder = StopPattern.create(2);
    builder.stops[0] = origin;
    builder.stops[1] = destination;

    var stopPattern = builder.build();
    var route = new Route(new FeedScopedId("Test", "R1"));

    return new TripPattern(new FeedScopedId("Test", "T1"), route, stopPattern);
  }

  /**
   * Create LineString of coordinates and stops.
   * @param origin First stop will become the first coordinate
   * @param destination Last stop will become the last coordinate
   * @param coordinate Coordinate to inject between stops
   * @return LineString with all coordinates
   */
  private LineString[] getLineStrings(Stop origin, Stop destination, Coordinate coordinate) {
    var coordinates = new ArrayList<Coordinate>();
    // Add start and stop first and last
    coordinates.add(new Coordinate(origin.getLon(), origin.getLat()));
    coordinates.add(coordinate);
    coordinates.add(new Coordinate(destination.getLon(), destination.getLat()));

    var l1 = GeometryUtils
      .getGeometryFactory()
      .createLineString(coordinates.toArray(Coordinate[]::new));

    return List.of(l1).toArray(LineString[]::new);
  }
}
