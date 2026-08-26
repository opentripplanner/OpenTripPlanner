package org.opentripplanner.ext.taxizone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.ext.taxizone.model.TaxiZone;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.Trip;

class TaxiZoneIndexTest {

  private static final Polygon SQUARE_1 = Polygons.square(
    new Coordinate(0, 0),
    new Coordinate(10, 10)
  );
  private static final Polygon SQUARE_2 = Polygons.square(
    new Coordinate(20, 20),
    new Coordinate(30, 30)
  );

  private static final Route ROUTE_1 = TransitRepositoryForTest.route("route-1").build();
  private static final Route ROUTE_2 = TransitRepositoryForTest.route("route-2").build();

  private static final Trip TRIP_1 = TransitRepositoryForTest.trip("trip-1")
    .withRoute(ROUTE_1)
    .build();
  private static final Trip TRIP_2 = TransitRepositoryForTest.trip("trip-2")
    .withRoute(ROUTE_2)
    .build();

  private static final WgsCoordinate INSIDE_SQUARE_1_A = new WgsCoordinate(2, 2);
  private static final WgsCoordinate INSIDE_SQUARE_1_B = new WgsCoordinate(8, 8);
  private static final WgsCoordinate INSIDE_SQUARE_2_A = new WgsCoordinate(25, 25);
  private static final WgsCoordinate INSIDE_SQUARE_2_B = new WgsCoordinate(28, 28);
  private static final WgsCoordinate OUTSIDE_ALL_ZONES = new WgsCoordinate(50, 50);

  private static TaxiZone zone(Polygon geometry, Trip trip) {
    return new TaxiZone(geometry, trip, null, null);
  }

  @Test
  void findsZoneCoveringBothPickupAndDropoff() {
    var index = new TaxiZoneIndex(List.of(zone(SQUARE_1, TRIP_1)));

    var result = index.findFirstZone(INSIDE_SQUARE_1_A, INSIDE_SQUARE_1_B);

    assertTrue(result.isPresent());
    assertEquals(ROUTE_1, result.get().trip().getRoute());
  }

  @Test
  void returnsEmptyWhenDropoffOutsideZone() {
    var index = new TaxiZoneIndex(List.of(zone(SQUARE_1, TRIP_1)));

    var result = index.findFirstZone(INSIDE_SQUARE_1_A, INSIDE_SQUARE_2_A);

    assertTrue(result.isEmpty());
  }

  @Test
  void returnsEmptyWhenNeitherPointInAnyZone() {
    var index = new TaxiZoneIndex(List.of(zone(SQUARE_1, TRIP_1), zone(SQUARE_2, TRIP_2)));

    var result = index.findFirstZone(OUTSIDE_ALL_ZONES, OUTSIDE_ALL_ZONES);

    assertTrue(result.isEmpty());
  }

  @Test
  void returnsEmptyForEmptyIndex() {
    var index = new TaxiZoneIndex(List.of());

    var result = index.findFirstZone(INSIDE_SQUARE_1_A, INSIDE_SQUARE_1_B);

    assertTrue(result.isEmpty());
  }

  @Test
  void findsCorrectZoneAmongMultipleCandidates() {
    var index = new TaxiZoneIndex(List.of(zone(SQUARE_1, TRIP_1), zone(SQUARE_2, TRIP_2)));

    var resultInSquare1 = index.findFirstZone(INSIDE_SQUARE_1_A, INSIDE_SQUARE_1_B);
    var resultInSquare2 = index.findFirstZone(INSIDE_SQUARE_2_A, INSIDE_SQUARE_2_B);

    assertTrue(resultInSquare1.isPresent());
    assertEquals(ROUTE_1, resultInSquare1.get().trip().getRoute());
    assertTrue(resultInSquare2.isPresent());
    assertEquals(ROUTE_2, resultInSquare2.get().trip().getRoute());
  }
}
