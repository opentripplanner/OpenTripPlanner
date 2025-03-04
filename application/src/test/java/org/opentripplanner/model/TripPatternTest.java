package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;

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
    var testModel = TimetableRepositoryForTest.of();
    var stationOrigin = testModel.station("S1").withCoordinate(0.0, 0.0).build();
    var stationDestination = testModel.station("S2").withCoordinate(1.0, 1.0).build();
    var stopOrigin = testModel
      .stop("A1")
      .withCoordinate(0.1, 0.1)
      .withParentStation(stationOrigin)
      .build();
    var stopNewOrigin = testModel
      .stop("A2")
      .withCoordinate(0.2, 0.2)
      .withParentStation(stationOrigin)
      .build();
    var stopDestination = testModel
      .stop("C")
      .withCoordinate(0.9, 0.9)
      .withParentStation(stationDestination)
      .build();
    var coordinate = new Coordinate(0.5, 0.5);

    // Add coordinate between stops on original trip pattern
    var originalTripPattern = setupTripPattern(
      stopOrigin,
      stopDestination,
      null,
      getLineStrings(stopOrigin, stopDestination, coordinate)
    );

    // Test without setting original trip pattern
    var newTripPattern = setupTripPattern(stopNewOrigin, stopDestination, null, null);

    var originalCoordinates = originalTripPattern.getHopGeometry(0).getCoordinates().length;
    var coordinates = newTripPattern.getHopGeometry(0).getCoordinates().length;

    assertEquals(
      3,
      originalCoordinates,
      "The coordinates for originalTripPattern on first hop should be 3"
    );
    assertEquals(2, coordinates, "The coordinates for tripPattern on first hop should be 2");

    // Test with setting original trip pattern
    newTripPattern = setupTripPattern(stopNewOrigin, stopDestination, originalTripPattern, null);

    var finalCoordinates = newTripPattern.getHopGeometry(0).getCoordinates().length;

    assertEquals(
      originalCoordinates,
      finalCoordinates,
      "The hop on tripPattern should contain as many coordinates as the originalTripPattern"
    );
  }

  /**
   * Create TripPattern between to stops
   *
   * @param origin      Start stop
   * @param destination End stop
   * @return TripPattern with stopPattern
   */
  public TripPattern setupTripPattern(
    RegularStop origin,
    RegularStop destination,
    TripPattern originalTripPattern,
    List<LineString> geometry
  ) {
    var builder = StopPattern.create(2);
    builder.stops.with(0, origin);
    builder.stops.with(1, destination);
    for (int i = 0; i < 2; i++) {
      builder.pickups.with(i, PickDrop.SCHEDULED);
      builder.dropoffs.with(i, PickDrop.SCHEDULED);
    }

    var stopPattern = builder.build();
    var route = TimetableRepositoryForTest.route("R1").build();

    return TripPattern.of(new FeedScopedId("Test", "T1"))
      .withRoute(route)
      .withStopPattern(stopPattern)
      .withOriginalTripPattern(originalTripPattern)
      .withHopGeometries(geometry)
      .build();
  }

  /**
   * Create LineString of coordinates and stops.
   * @param origin First stop will become the first coordinate
   * @param destination Last stop will become the last coordinate
   * @param coordinate Coordinate to inject between stops
   * @return LineString with all coordinates
   */
  private List<LineString> getLineStrings(
    RegularStop origin,
    RegularStop destination,
    Coordinate coordinate
  ) {
    var coordinates = new ArrayList<Coordinate>();
    // Add start and stop first and last
    coordinates.add(new Coordinate(origin.getLon(), origin.getLat()));
    coordinates.add(coordinate);
    coordinates.add(new Coordinate(destination.getLon(), destination.getLat()));

    var l1 = GeometryUtils.getGeometryFactory()
      .createLineString(coordinates.toArray(Coordinate[]::new));

    return List.of(l1);
  }
}
