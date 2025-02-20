package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.RaptorTestConstants.D2m;
import static org.opentripplanner.raptor._data.RaptorTestConstants.D30s;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_A;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_B;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_C;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_D;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_E;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_F;
import static org.opentripplanner.raptor._data.RaptorTestConstants.T00_00;
import static org.opentripplanner.raptor._data.RaptorTestConstants.T01_00;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.api.request.RaptorViaLocation.passThrough;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;
import org.opentripplanner.raptor.configure.RaptorConfig;

/**
 * FEATURE UNDER TEST
 *
 * Raptor should be able to handle route request with a specified pass-through point.
 * If a stop point is specified as pass-through point in the request then all the results returned
 * from raptor should include this stop point either as alight or board point for a trip or as an
 * intermediate point in the trip.
 *
 * It should be possible to specify more than one pass through point. The result should include
 * stop points in the order as they were specified in the request. Only alternatives that pass
 * through all the stop point should be included in the result.
 *
 * In order to support stop areas raptor should also support multiple stop points in the same
 * pass-through group. It should be possible to define both stop A and B as a pass-through. Then
 * alternatives that pass either stop A or B should not be dropped.
 */
class J01_PassThroughTest {

  static final RaptorViaLocation PASS_THROUGH_STOP_A = passThrough("A")
    .addPassThroughStop(STOP_A)
    .build();
  static final RaptorViaLocation PASS_THROUGH_STOP_C = passThrough("C")
    .addPassThroughStop(STOP_C)
    .build();
  static final RaptorViaLocation PASS_THROUGH_STOP_D = passThrough("D")
    .addPassThroughStop(STOP_D)
    .build();
  static final List<RaptorViaLocation> PASS_THROUGH_STOP_B_OR_C = List.of(
    passThrough("B&C").addPassThroughStop(STOP_B).addPassThroughStop(STOP_C).build()
  );
  static final List<RaptorViaLocation> PASS_THROUGH_STOP_B_THEN_C = List.of(
    passThrough("B").addPassThroughStop(STOP_B).build(),
    passThrough("C").addPassThroughStop(STOP_C).build()
  );
  static final List<RaptorViaLocation> PASS_THROUGH_STOP_B_THEN_D = List.of(
    passThrough("B").addPassThroughStop(STOP_B).build(),
    passThrough("D").addPassThroughStop(STOP_D).build()
  );

  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  private RaptorRequestBuilder<TestTripSchedule> prepareRequest() {
    var builder = new RaptorRequestBuilder<TestTripSchedule>();

    builder
      .profile(RaptorProfile.MULTI_CRITERIA)
      // TODO: 2023-07-24 Currently heuristics does not work with pass-through so we
      //  have to turn them off. Make sure to re-enable optimization later when it's fixed
      .clearOptimizations();

    builder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .searchWindow(Duration.ofMinutes(2))
      .timetable(true);

    return builder;
  }

  @Test
  @DisplayName("Pass-through stop point as a last point in the journey.")
  void passThroughPointOnEgress() {
    var data = new TestTransitData();

    // Create two routes.
    // Only one of them includes required pass-through point.
    // Pass-through point is the last stop in the trip.
    // The trip with pass-through point have significant longer travel time so that normally it
    //  should not be used
    var r1 = route("R1", STOP_A, STOP_B, STOP_C).withTimetable(schedule("0:02 0:05 0:20"));
    var r2 = route("R2", STOP_A, STOP_B, STOP_D).withTimetable(schedule("0:02 0:10 0:50"));

    data.withRoutes(r1, r2);

    var requestBuilder = prepareRequest();

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addViaLocation(PASS_THROUGH_STOP_D)
      .addEgressPaths(walk(STOP_D, D30s))
      .addEgressPaths(walk(STOP_C, D30s));

    // Verify that only the journey with pass-through stop point is included in response
    assertEquals(
      "Walk 30s ~ A ~ BUS R2 0:02 0:50 ~ D ~ Walk 30s [0:01:30 0:50:30 49m Tₓ0 C₁3_600 C₂1]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Pass-through stop point as a first point in the journey.")
  void passThroughPointOnAccess() {
    var data = new TestTransitData();

    // Create two routes.
    // Only one of them includes required pass-through point.
    // Pass-through point is the first stop in the trip.
    // The trip with pass-through point have significant longer travel time so that normally it
    //  should not be used
    var r1 = route("R1", STOP_B, STOP_C, STOP_D).withTimetable(schedule("0:02 0:05 0:20"));
    var r2 = route("R2", STOP_A, STOP_C, STOP_D).withTimetable(schedule("0:02 0:10 0:50"));

    data.withRoutes(r1, r2);

    var requestBuilder = prepareRequest();

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addAccessPaths(walk(STOP_B, D30s))
      .addViaLocation(PASS_THROUGH_STOP_A)
      .addEgressPaths(walk(STOP_D, D30s));

    // Verify that only the journey with pass-through stop point is included in response
    assertEquals(
      "Walk 30s ~ A ~ BUS R2 0:02 0:50 ~ D ~ Walk 30s [0:01:30 0:50:30 49m Tₓ0 C₁3_600 C₂1]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Pass-through stop point as an intermediate point in the journey.")
  void passThroughPointInTheMiddle() {
    var data = new TestTransitData();

    // Create two routes.
    // Only one of them includes required pass-through point.
    // Pass-through point is the intermediate stop in the trip.
    // The trip with pass-through point have significant longer travel time so that normally it
    //  should not be used
    var r1 = route("R1", STOP_A, STOP_B, STOP_D).withTimetable(schedule("0:02 0:05 0:20"));
    var r2 = route("R2", STOP_A, STOP_C, STOP_D).withTimetable(schedule("0:02 0:10 0:50"));

    data.withRoutes(r1, r2);
    data.withTransferCost(100);

    var requestBuilder = prepareRequest();

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addViaLocation(PASS_THROUGH_STOP_C)
      .addEgressPaths(walk(STOP_D, D30s));

    // Verify that only the journey with pass-through stop point is included in response
    assertEquals(
      "Walk 30s ~ A ~ BUS R2 0:02 0:50 ~ D ~ Walk 30s [0:01:30 0:50:30 49m Tₓ0 C₁3_600 C₂1]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Multiple pass-through stop points")
  void multiplePassThroughPoints() {
    var data = new TestTransitData();

    // Create two routes.
    // First one includes one pass-through stop point.
    // Second one include the second pass-through point.
    // Both arrive at the desired destination so normally there should not be any transfers.
    var r1 = route("R1", STOP_A, STOP_B, STOP_C, STOP_F)
      .withTimetable(schedule("0:02 0:05 0:10 0:20"));
    var r2 = route("R2", STOP_C, STOP_D, STOP_E, STOP_F)
      .withTimetable(schedule("0:15 0:20 0:30 0:50"));

    data.withRoutes(r1, r2);
    data.withTransferCost(100);

    var requestBuilder = prepareRequest();

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addViaLocations(PASS_THROUGH_STOP_B_THEN_D)
      .addEgressPaths(walk(STOP_F, D30s));

    // Verify that Raptor generated journey with a transfer to r2 so that both pass-through points
    //  are included
    assertEquals(
      "Walk 30s ~ A ~ BUS R1 0:02 0:10 ~ C ~ BUS R2 0:15 0:50 ~ F ~ Walk 30s [0:01:30 0:50:30 49m Tₓ1 C₁4_300 C₂2]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Pass-through order")
  void passThroughOrder() {
    var data = new TestTransitData();

    // Create two routes.
    // Both include all the desired pass-through stop points but only one of them have correct order.
    var r1 = route("R1", STOP_A, STOP_B, STOP_C, STOP_D)
      .withTimetable(schedule("0:05 0:10 0:15 0:20"));
    var r2 = route("R2", STOP_A, STOP_C, STOP_B, STOP_D)
      .withTimetable(schedule("0:05 0:10 0:15 0:17"));

    data.withRoutes(r1, r2);

    var requestBuilder = prepareRequest();

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addViaLocations(PASS_THROUGH_STOP_B_THEN_C)
      .addEgressPaths(walk(STOP_D, D30s));

    // Verify that only route with correct pass-through order is returned
    assertEquals(
      "Walk 30s ~ A ~ BUS R1 0:05 0:20 ~ D ~ Walk 30s [0:04:30 0:20:30 16m Tₓ0 C₁1_620 C₂2]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Multiple stops in same pass-through group")
  void passThroughGroup() {
    var data = new TestTransitData();

    // Create two routes.
    // Route one include STOP_B and route two include STOP_C.
    // Both stops with be part of the same pass-through group
    //  so that both routes should be valid
    var r1 = route("R1", STOP_A, STOP_C, STOP_E).withTimetable(schedule("0:04 0:10 0:15"));
    var r2 = route("R2", STOP_B, STOP_D, STOP_E).withTimetable(schedule("0:05 0:10 0:14"));

    data.withRoutes(r1, r2);

    var requestBuilder = prepareRequest();

    requestBuilder
      .searchParams()
      // Both routes are pareto optimal.
      // Route 2 is faster but it contains more walk
      .addAccessPaths(walk(STOP_A, D30s))
      .addAccessPaths(walk(STOP_B, D2m))
      .addViaLocations(PASS_THROUGH_STOP_B_OR_C)
      .addEgressPaths(walk(STOP_E, D30s));

    // Verify that both routes are included as a valid result
    assertEquals(
      """
      Walk 2m ~ B ~ BUS R2 0:05 0:14 ~ E ~ Walk 30s [0:03 0:14:30 11m30s Tₓ0 C₁1_440 C₂1]
      Walk 30s ~ A ~ BUS R1 0:04 0:15 ~ E ~ Walk 30s [0:03:30 0:15:30 12m Tₓ0 C₁1_380 C₂1]
      """.trim(),
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }
}
