package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.LAKE_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.LAKE_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.LAKE_SOUTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.LAKE_WEST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTrip;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createStopAt;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithStops;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.accessRequest;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.directRequest;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Egress tests are omitted because access and egress share a code path
 * ({@link DistanceTripFilter#isProximateForAccessEgress}); the only difference is which passenger
 * coordinate is read.
 */
class DistanceTripFilterTest {

  private final DistanceTripFilter filter = new DistanceTripFilter();

  // ---------------------------------------------------------------------------
  // Direct routing
  // ---------------------------------------------------------------------------

  @Test
  void direct_passengerAlongRoute_returnsTrue() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var pickup = new WgsCoordinate(59.920, 10.760);
    var dropoff = new WgsCoordinate(59.940, 10.780);
    assertTrue(filter.isCandidateTrip(trip, directRequest(pickup, dropoff)));
  }

  @Test
  void direct_passengerFarFromRoute_returnsFalse() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    // Bergen — ~300 km away
    var pickup = new WgsCoordinate(60.39, 5.32);
    var dropoff = new WgsCoordinate(60.40, 5.33);
    assertFalse(filter.isCandidateTrip(trip, directRequest(pickup, dropoff)));
  }

  @Test
  void direct_oneEndNearRouteOtherFar_returnsTrue() {
    // The filter accepts if EITHER endpoint is within range.
    var trip = createSimpleTrip(new WgsCoordinate(59.9, 10.70), new WgsCoordinate(59.9, 10.80));
    var pickup = new WgsCoordinate(59.9, 10.75);
    var dropoff = new WgsCoordinate(59.9 + 0.5, 10.75);
    assertTrue(filter.isCandidateTrip(trip, directRequest(pickup, dropoff)));
  }

  @Test
  void direct_multipleStops_passengerNearMiddleSegment_returnsTrue() {
    var trip = createTripWithStops(
      LAKE_NORTH,
      List.of(createStopAt(LAKE_EAST), createStopAt(LAKE_SOUTH)),
      LAKE_WEST
    );
    var pickup = new WgsCoordinate(59.9139, 10.735);
    var dropoff = new WgsCoordinate(59.9139, 10.720);
    assertTrue(filter.isCandidateTrip(trip, directRequest(pickup, dropoff)));
  }

  @Test
  void direct_customMaxDistance_acceptsWithinCustomDistance() {
    var customFilter = new DistanceTripFilter(100_000);
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    // ~80 km perpendicular — rejected by 50 km default, accepted by 100 km custom.
    var pickup = new WgsCoordinate(59.920, 10.752 + 0.7);
    var dropoff = new WgsCoordinate(59.940, 10.772 + 0.7);
    assertTrue(customFilter.isCandidateTrip(trip, directRequest(pickup, dropoff)));
  }

  @Test
  void direct_customMaxDistance_rejectsOutsideCustomDistance() {
    var customFilter = new DistanceTripFilter(20_000);
    var trip = createSimpleTrip(new WgsCoordinate(59.9, 10.70), new WgsCoordinate(59.9, 10.80));
    // ~30 km perpendicular — beyond the 20 km custom limit.
    var pickup = new WgsCoordinate(59.9 + 0.27, 10.72);
    var dropoff = new WgsCoordinate(59.9 + 0.27, 10.78);
    assertFalse(customFilter.isCandidateTrip(trip, directRequest(pickup, dropoff)));
  }

  // ---------------------------------------------------------------------------
  // Access (access and egress share a code path — covering access alone is sufficient)
  // ---------------------------------------------------------------------------

  @Test
  void access_passengerNearRoute_returnsTrue() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var passengerPickup = new WgsCoordinate(59.920, 10.760);
    assertTrue(filter.isCandidateTrip(trip, accessRequest(passengerPickup)));
  }

  @Test
  void access_passengerFurtherFromRouteThanTripIsLong_returnsFalse() {
    // Very short trip (~140 m); passenger hundreds of km away.
    var trip = createSimpleTrip(
      new WgsCoordinate(59.900, 10.750),
      new WgsCoordinate(59.901, 10.751)
    );
    assertFalse(filter.isCandidateTrip(trip, accessRequest(new WgsCoordinate(60.5, 11.0))));
  }
}
