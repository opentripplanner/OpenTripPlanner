package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.CarpoolTripTestData;
import org.opentripplanner.ext.carpooling.CarpoolTripWithVerticesTestData;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripWithVertices;
import org.opentripplanner.street.geometry.WgsCoordinate;

class ClosestCandidateTripsTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(63.43, 10.39);

  /** A trip running east from a start that lies {@code northMetres} north of the origin. */
  private static CarpoolTripWithVertices eastboundTrip(double northMetres) {
    var start = ORIGIN.moveNorthMeters(northMetres);
    return CarpoolTripWithVerticesTestData.withDummyVertices(
      CarpoolTripTestData.createSimpleTrip(start, start.moveEastMeters(20_000))
    );
  }

  @Test
  void keepsTheTripsPassingClosestToThePassenger() {
    var far = eastboundTrip(5_000);
    var near = eastboundTrip(300);
    var middle = eastboundTrip(1_500);

    var kept = ClosestCandidateTrips.closest(List.of(far, near, middle), List.of(ORIGIN), 2);

    assertEquals(List.of(near, middle), kept, "ordered by closeness");
  }

  @Test
  void distanceIsToTheNearestRouteSegmentNotToTheEndpoints() {
    var trip = eastboundTrip(400);
    // A point beside the middle of the 20 km route is 400 m from the route although it is ~10 km
    // from either end.
    var besideTheMiddle = ORIGIN.moveEastMeters(10_000);
    double d = ClosestCandidateTrips.distanceToRoute(trip.trip(), besideTheMiddle);
    assertTrue(d > 350 && d < 450, "distance to the route was " + d);
  }

  @Test
  void directRequestsScoreBothEndsOfTheJourney() {
    var passengerPickup = ORIGIN;
    var passengerDropoff = ORIGIN.moveEastMeters(15_000);
    // Passes right by the pickup but ends far north of the dropoff.
    var goodPickupOnly = CarpoolTripWithVerticesTestData.withDummyVertices(
      CarpoolTripTestData.createSimpleTrip(
        ORIGIN.moveNorthMeters(100),
        ORIGIN.moveNorthMeters(8_000)
      )
    );
    // A kilometre off at both ends.
    var fairAtBothEnds = eastboundTrip(1_000);

    var kept = ClosestCandidateTrips.closest(
      List.of(goodPickupOnly, fairAtBothEnds),
      List.of(passengerPickup, passengerDropoff),
      1
    );

    assertEquals(List.of(fairAtBothEnds), kept);
  }

  @Test
  void fewEnoughTripsAreReturnedUnrankedInInputOrder() {
    var far = eastboundTrip(5_000);
    var near = eastboundTrip(300);
    assertEquals(
      List.of(far, near),
      ClosestCandidateTrips.closest(List.of(far, near), List.of(ORIGIN), 2)
    );
    assertEquals(List.of(), ClosestCandidateTrips.closest(List.of(), List.of(ORIGIN), 50));
  }

  @Test
  void maxMustBePositive() {
    assertThrows(IllegalArgumentException.class, () ->
      ClosestCandidateTrips.closest(List.of(eastboundTrip(0)), List.of(ORIGIN), 0)
    );
  }
}
