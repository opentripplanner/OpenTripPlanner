package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.egressRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.geometry.WgsCoordinate;

class DistanceBasedFilterTest {

  private DistanceBasedFilter filter;

  @BeforeEach
  void setup() {
    filter = new DistanceBasedFilter();
  }

  // ---------------------------------------------------------------------------
  // Direct routing
  // ---------------------------------------------------------------------------

  @Test
  void isCandidateTrip_direct_passengerAlongRoute_returnsTrue() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var pickup = new WgsCoordinate(59.920, 10.760);
    var dropoff = new WgsCoordinate(59.940, 10.780);
    assertTrue(filter.isCandidateTrip(trip, directRequest(pickup, dropoff), null));
  }

  @Test
  void isCandidateTrip_direct_passengerParallelToRouteNearby_returnsTrue() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var pickup = new WgsCoordinate(59.920, 10.740);
    var dropoff = new WgsCoordinate(59.940, 10.760);
    assertTrue(filter.isCandidateTrip(trip, directRequest(pickup, dropoff), null));
  }

  @Test
  void isCandidateTrip_direct_passengerFarFromRoute_returnsFalse() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    // Bergen — ~300 km away
    var pickup = new WgsCoordinate(60.39, 5.32);
    var dropoff = new WgsCoordinate(60.40, 5.33);
    assertFalse(filter.isCandidateTrip(trip, directRequest(pickup, dropoff), null));
  }

  @Test
  void isCandidateTrip_direct_passengerPerpendicularFarAway_returnsFalse() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    // > 50 km perpendicular west
    var pickup = new WgsCoordinate(59.9139, 9.5);
    var dropoff = new WgsCoordinate(59.9549, 9.5);
    assertFalse(filter.isCandidateTrip(trip, directRequest(pickup, dropoff), null));
  }

  @Test
  void isCandidateTrip_direct_oneEndNearRouteOtherFar_returnsTrue() {
    // Filter accepts if either endpoint is within range
    var tripStart = new WgsCoordinate(59.9, 10.70);
    var tripEnd = new WgsCoordinate(59.9, 10.80);
    var trip = createSimpleTrip(tripStart, tripEnd);
    // pickup on route, dropoff ~55 km north — filter accepts if either end is within range
    var pickup = new WgsCoordinate(59.9, 10.75);
    var dropoff = new WgsCoordinate(59.9 + 0.5, 10.75);
    assertTrue(filter.isCandidateTrip(trip, directRequest(pickup, dropoff), null));
  }

  @Test
  void isCandidateTrip_direct_multipleStops_passengerNearMiddleSegment_returnsTrue() {
    var stop1 = createStopAt(LAKE_EAST);
    var stop2 = createStopAt(LAKE_SOUTH);
    var trip = createTripWithStops(LAKE_NORTH, java.util.List.of(stop1, stop2), LAKE_WEST);
    var pickup = new WgsCoordinate(59.9139, 10.735);
    var dropoff = new WgsCoordinate(59.9139, 10.720);
    assertTrue(filter.isCandidateTrip(trip, directRequest(pickup, dropoff), null));
  }

  @Test
  void isCandidateTrip_direct_customMaxDistance_acceptsWithinCustomDistance() {
    var customFilter = new DistanceBasedFilter(100_000);
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    // ~80 km perpendicular — rejected by 50 km default, accepted by 100 km custom
    var pickup = new WgsCoordinate(59.920, 10.752 + 0.7);
    var dropoff = new WgsCoordinate(59.940, 10.772 + 0.7);
    assertTrue(customFilter.isCandidateTrip(trip, directRequest(pickup, dropoff), null));
  }

  @Test
  void isCandidateTrip_direct_customMaxDistance_rejectsOutsideCustomDistance() {
    var customFilter = new DistanceBasedFilter(20_000);
    var tripStart = new WgsCoordinate(59.9, 10.70);
    var tripEnd = new WgsCoordinate(59.9, 10.80);
    var trip = createSimpleTrip(tripStart, tripEnd);
    // ~30 km perpendicular — beyond the 20 km custom limit
    var pickup = new WgsCoordinate(59.9 + 0.27, 10.72);
    var dropoff = new WgsCoordinate(59.9 + 0.27, 10.78);
    assertFalse(customFilter.isCandidateTrip(trip, directRequest(pickup, dropoff), null));
  }

  // ---------------------------------------------------------------------------
  // Access routing — passenger coordinate is the pickup; trip length > distance to passenger
  // ---------------------------------------------------------------------------

  @Test
  void isCandidateTrip_access_passengerNearRoute_returnsTrue() {
    // Trip is long relative to the passenger's perpendicular distance from it.
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var passengerPickup = new WgsCoordinate(59.920, 10.760);
    assertTrue(filter.isCandidateTrip(trip, accessRequest(passengerPickup), null));
  }

  @Test
  void isCandidateTrip_access_passengerFurtherFromRouteThanTripIsLong_returnsFalse() {
    // Short trip; passenger is farther away than the trip is long.
    var tripStart = new WgsCoordinate(59.900, 10.750);
    // very short trip (~140 m); passenger hundreds of km away
    var tripEnd = new WgsCoordinate(59.901, 10.751);
    var trip = createSimpleTrip(tripStart, tripEnd);
    var passengerPickup = new WgsCoordinate(60.5, 11.0);
    assertFalse(filter.isCandidateTrip(trip, accessRequest(passengerPickup), null));
  }

  // ---------------------------------------------------------------------------
  // Egress routing — passenger coordinate is the dropoff; same geometry as access
  // ---------------------------------------------------------------------------

  @Test
  void isCandidateTrip_egress_passengerNearRoute_returnsTrue() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var passengerDropoff = new WgsCoordinate(59.920, 10.760);
    assertTrue(filter.isCandidateTrip(trip, egressRequest(passengerDropoff), null));
  }

  @Test
  void isCandidateTrip_egress_passengerFurtherFromRouteThanTripIsLong_returnsFalse() {
    var tripStart = new WgsCoordinate(59.900, 10.750);
    var tripEnd = new WgsCoordinate(59.901, 10.751);
    var trip = createSimpleTrip(tripStart, tripEnd);
    var passengerDropoff = new WgsCoordinate(60.5, 11.0);
    assertFalse(filter.isCandidateTrip(trip, egressRequest(passengerDropoff), null));
  }

  // ---------------------------------------------------------------------------
  // Configuration
  // ---------------------------------------------------------------------------

  @Test
  void defaultMaxDistance_is50km() {
    assertEquals(50_000, filter.getMaxDistanceMeters());
  }

  @Test
  void getMaxDistanceMeters_returnsConfiguredDistance() {
    assertEquals(75_000, new DistanceBasedFilter(75_000).getMaxDistanceMeters());
  }
}
