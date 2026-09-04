package org.opentripplanner.ext.taxizone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.ext.taxizone.model.TaxiZone;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;

class TaxiZoneIndexTest {

  private static final LocalDate DATE = LocalDate.of(2020, 2, 2);
  private static final LocalDate OTHER_DATE = LocalDate.of(2020, 2, 3);

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

  private static final WgsCoordinate INSIDE_SQUARE_1_A = new WgsCoordinate(2, 2);
  private static final WgsCoordinate INSIDE_SQUARE_1_B = new WgsCoordinate(8, 8);
  private static final WgsCoordinate INSIDE_SQUARE_2_A = new WgsCoordinate(25, 25);
  private static final WgsCoordinate INSIDE_SQUARE_2_B = new WgsCoordinate(28, 28);
  private static final WgsCoordinate OUTSIDE_ALL_ZONES = new WgsCoordinate(50, 50);

  private static TaxiZone zone(Polygon geometry, Route route) {
    return new TaxiZone(geometry, route, null, null, LocalDateRange.ofInclusiveEnd(DATE, DATE));
  }

  @Test
  void findsZoneCoveringBothPickupAndDropoff() {
    var index = new TaxiZoneIndex(List.of(zone(SQUARE_1, ROUTE_1)));

    var result = index.findFirstZone(INSIDE_SQUARE_1_A, INSIDE_SQUARE_1_B, DATE);

    assertTrue(result.isPresent());
    assertEquals(ROUTE_1, result.get().route());
  }

  @Test
  void returnsEmptyWhenDropoffOutsideZone() {
    var index = new TaxiZoneIndex(List.of(zone(SQUARE_1, ROUTE_1)));

    var result = index.findFirstZone(INSIDE_SQUARE_1_A, INSIDE_SQUARE_2_A, DATE);

    assertTrue(result.isEmpty());
  }

  @Test
  void returnsEmptyWhenNeitherPointInAnyZone() {
    var index = new TaxiZoneIndex(List.of(zone(SQUARE_1, ROUTE_1), zone(SQUARE_2, ROUTE_2)));

    var result = index.findFirstZone(OUTSIDE_ALL_ZONES, OUTSIDE_ALL_ZONES, DATE);

    assertTrue(result.isEmpty());
  }

  @Test
  void returnsEmptyForEmptyIndex() {
    var index = new TaxiZoneIndex(List.of());

    var result = index.findFirstZone(INSIDE_SQUARE_1_A, INSIDE_SQUARE_1_B, DATE);

    assertTrue(result.isEmpty());
  }

  @Test
  void returnsEmptyWhenDateNotInZoneServiceDates() {
    var index = new TaxiZoneIndex(List.of(zone(SQUARE_1, ROUTE_1)));

    var result = index.findFirstZone(INSIDE_SQUARE_1_A, INSIDE_SQUARE_1_B, OTHER_DATE);

    assertTrue(result.isEmpty());
  }

  @Test
  void findsCorrectZoneAmongMultipleCandidates() {
    var index = new TaxiZoneIndex(List.of(zone(SQUARE_1, ROUTE_1), zone(SQUARE_2, ROUTE_2)));

    var resultInSquare1 = index.findFirstZone(INSIDE_SQUARE_1_A, INSIDE_SQUARE_1_B, DATE);
    var resultInSquare2 = index.findFirstZone(INSIDE_SQUARE_2_A, INSIDE_SQUARE_2_B, DATE);

    assertTrue(resultInSquare1.isPresent());
    assertEquals(ROUTE_1, resultInSquare1.get().route());
    assertTrue(resultInSquare2.isPresent());
    assertEquals(ROUTE_2, resultInSquare2.get().route());
  }
}
