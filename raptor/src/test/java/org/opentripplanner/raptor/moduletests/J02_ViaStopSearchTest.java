package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.RaptorTestConstants.D1m;
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
import static org.opentripplanner.raptor._data.transit.TestTransfer.transfer;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.api.request.RaptorViaLocation.via;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.api.PathUtils;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;
import org.opentripplanner.raptor.configure.RaptorConfig;

/**
 * FEATURE UNDER TEST
 *
 * Raptor should be able to handle route request with one or more via locations transfering at a
 * given set of stops. If a stop is specified as via location in the request, then all the results
 * returned from raptor should include the stop. The stop should be a alight, board or intermediate
 * stop of one of the trips in the path.
 *
 * It should be possible to specify more than one connection. The result should include the via
 * locations in the order as they were specified in the request. Only alternatives that pass
 * through all via locations should be included in the result.
 *
 * To support stations and other collections of stops, Raptor should also support multiple via
 * connections in one via location.
 */
class J02_ViaStopSearchTest {

  static final List<RaptorViaLocation> VIA_LOCATION_STOP_B = List.of(viaLocation("B", STOP_B));
  static final List<RaptorViaLocation> VIA_LOCATION_STOP_C = List.of(viaLocation("C", STOP_C));
  static final List<RaptorViaLocation> VIA_LOCATION_STOP_A_OR_B = List.of(
    viaLocation("B&C", STOP_A, STOP_B)
  );

  static final List<RaptorViaLocation> VIA_LOCATION_STOP_B_THEN_D = List.of(
    viaLocation("B", STOP_B),
    viaLocation("D", STOP_D)
  );
  static final List<RaptorViaLocation> VIA_LOCATION_STOP_C_THEN_B = List.of(
    viaLocation("B", STOP_C),
    viaLocation("D", STOP_B)
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
      .searchWindow(Duration.ofMinutes(10))
      .timetable(true);

    return builder;
  }

  @Test
  @DisplayName(
    "Basic via search with just one route. You should be forced to get off the " +
    "first trip and wait for the next one at the specified via stop."
  )
  void viaSearchAlightingAtViaStop() {
    var data = new TestTransitData();

    data.withRoutes(
      route("R1", STOP_A, STOP_B, STOP_C, STOP_D)
        .withTimetable(schedule("0:02 0:10 0:20 0:30"), schedule("0:12 0:20 0:30 0:40"))
    );

    var requestBuilder = prepareRequest();

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addViaLocation(via("C").addViaStop(STOP_C).build())
      .addEgressPaths(walk(STOP_D, D30s));

    var result = raptorService.route(requestBuilder.build(), data);

    // Verify that we alight the first trip at stop C and board the second trip
    assertEquals(
      "Walk 30s ~ A ~ BUS R1 0:02 0:20 ~ C ~ BUS R1 0:30 0:40 ~ D ~ Walk 30s [0:01:30 0:40:30 39m Tₓ1 C₁3_600]",
      pathsToString(result)
    );
  }

  @Test
  @DisplayName(
    "Basic via search with just two routes. You should be forced to get off the first route, " +
    "then transfer and BOARD the second trip at the specified via stop. This test that via works " +
    "at the boarding stop. We will add better options for the transfer to see that the given via " +
    "stop is used over the alternatives."
  )
  void viaSearchArrivingByTransferAtViaStop() {
    var data = new TestTransitData();

    data
      .withRoutes(
        route("R1", STOP_A, STOP_B, STOP_D, STOP_E).withTimetable(schedule("0:02 0:10 0:20 0:30")),
        route("R2", STOP_C, STOP_D, STOP_E).withTimetable(schedule("0:25 0:30 0:40"))
      )
      // Walk 1 minute to transfer from D to C - this is the only way to visit stop C
      .withTransfer(STOP_D, transfer(STOP_C, D1m));

    var requestBuilder = prepareRequest();

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addViaLocation(via("C").addViaStop(STOP_C).build())
      .addEgressPaths(walk(STOP_E, D30s));

    var result = raptorService.route(requestBuilder.build(), data);

    // Verify that we alight the first trip at stop C and board the second trip
    assertEquals(
      "Walk 30s ~ A ~ BUS R1 0:02 0:20 ~ D ~ Walk 1m ~ C ~ BUS R2 0:25 0:40 ~ E ~ Walk 30s " +
      "[0:01:30 0:40:30 39m Tₓ1 C₁3_660]",
      pathsToString(result)
    );
  }

  @Test
  @DisplayName(
    "Via stop as the first stop in the journey - only the access will be used for the first " +
    "part, no transit. Access arrival should be copied over to 'next' worker."
  )
  void accessWalkToViaStopWithoutTransit() {
    var data = new TestTransitData();

    data.withRoutes(
      route("R1", STOP_A, STOP_B, STOP_C, STOP_D)
        .withTimetable(
          schedule("0:02 0:05 0:10 0:15"),
          // We add another trip to allow riding trip one - via B - then ride trip two, this
          // is not a pareto-optimal solution and should only appear if there is something wrong.
          schedule("0:12 0:15 0:20 0:25")
        )
    );

    var requestBuilder = prepareRequest();

    // We will add access to A, B, and C, but since the B stop is the via point we expect that to
    // be used
    requestBuilder
      .searchParams()
      .addViaLocations(VIA_LOCATION_STOP_B)
      // We allow access to A, B, and C - if the via search works as expected, only access to B
      // should be used - access to A would require an extra transfer; C has no valid paths.
      .addAccessPaths(walk(STOP_A, D30s))
      .addAccessPaths(walk(STOP_B, D30s))
      .addAccessPaths(walk(STOP_C, D30s))
      .addEgressPaths(walk(STOP_D, D30s));

    // Verify that the journey start by walking to the via stop, the uses one trip to the destination.
    // A combination of trip one and two with a transfer is not expected.
    assertEquals(
      PathUtils.join(
        "Walk 30s ~ B ~ BUS R1 0:05 0:15 ~ D ~ Walk 30s [0:04:30 0:15:30 11m Tₓ0 C₁1_320]",
        "Walk 30s ~ B ~ BUS R1 0:15 0:25 ~ D ~ Walk 30s [0:14:30 0:25:30 11m Tₓ0 C₁1_320]"
      ),
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName(
    "Via stop as the last stop in the journey - only the egress will be used for the last " +
    "part, no transit. The transit arrival at the via stop should be copied over to the " +
    "next worker and then this should be used to add the egress - without any transfers or" +
    "more transit."
  )
  void transitToViaStopThenTakeEgressWalkToDestination() {
    var data = new TestTransitData();

    data.withRoutes(
      route("R1", STOP_A, STOP_B, STOP_C, STOP_D)
        .withTimetable(
          schedule("0:02 0:05 0:10 0:20"),
          // We add another trip to check that we do not transfer to the other trip at some point.
          schedule("0:12 0:15 0:20 0:25")
        )
    );

    var requestBuilder = prepareRequest();

    // We will add access to A, B, and C, but since the B stop is the via point we expect that to
    // be used
    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addViaLocations(VIA_LOCATION_STOP_C)
      // We allow egress from B, C, and D - if the via search works as expected, only egress from C
      // should be used - egress from B has not visited via stop C, and egress from stop D would
      // require a transfer at stop C to visit the via stop - this is not an optimal path.
      .addEgressPaths(walk(STOP_B, D30s))
      .addEgressPaths(walk(STOP_C, D30s))
      .addEgressPaths(walk(STOP_D, D30s));

    assertEquals(
      PathUtils.join(
        "Walk 30s ~ A ~ BUS R1 0:02 0:10 ~ C ~ Walk 30s [0:01:30 0:10:30 9m Tₓ0 C₁1_200]",
        "Walk 30s ~ A ~ BUS R1 0:12 0:20 ~ C ~ Walk 30s [0:11:30 0:20:30 9m Tₓ0 C₁1_200]"
      ),
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Multiple via points")
  void multipleViaPoints() {
    var data = new TestTransitData();

    // Create two routes.
    // The first one includes one via stop point.
    // The second one includes the second via point.
    // Both arrive at the desired destination, so normally there should not be any transfers.
    data.withRoutes(
      route("R2")
        .timetable(
          """
        A    B    C    D    E    F
        0:02 0:05 0:10 0:15 0:20 0:25
        0:12 0:15 0:20 0:25 0:30 0:35
        0:22 0:25 0:30 0:35 0:40 0:45
        """
        )
    );

    data.withTransferCost(100);

    var requestBuilder = prepareRequest();

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addViaLocations(VIA_LOCATION_STOP_B_THEN_D)
      .addEgressPaths(walk(STOP_F, D30s));

    // Verify that both via points are included
    assertEquals(
      "Walk 30s ~ A " +
      "~ BUS R2 0:02 0:05 ~ B " +
      "~ BUS R2 0:15 0:25 ~ D " +
      "~ BUS R2 0:35 0:45 ~ F " +
      "~ Walk 30s " +
      "[0:01:30 0:45:30 44m Tₓ2 C₁4_700]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Multiple via points works with circular lines, visit stop C than stop B")
  void viaSearchWithCircularLine() {
    var data = new TestTransitData();

    data.withRoute(
      route("R1", STOP_A, STOP_B, STOP_C, STOP_B, STOP_C, STOP_B, STOP_C, STOP_B, STOP_D)
        .withTimetable(schedule("0:05 0:10 0:15 0:20 0:25 0:30 0:35 0:40 0:45"))
    );

    var requestBuilder = prepareRequest();

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addViaLocations(VIA_LOCATION_STOP_C_THEN_B)
      .addEgressPaths(walk(STOP_D, D30s));

    assertEquals(
      "Walk 30s ~ A " +
      "~ BUS R1 0:05 0:15 ~ C " +
      "~ BUS R1 0:25 0:30 ~ B " +
      "~ BUS R1 0:40 0:45 ~ D " +
      "~ Walk 30s " +
      "[0:04:30 0:45:30 41m Tₓ2 C₁4_320]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Multiple stops in the same via location")
  void testViaSearchWithManyStopsInTheViaLocation() {
    var data = new TestTransitData();

    data.withRoutes(
      route("R1", STOP_A, STOP_C).withTimetable(schedule("0:04 0:15")),
      route("R2", STOP_B, STOP_C).withTimetable(schedule("0:05 0:14"))
    );

    var requestBuilder = prepareRequest();

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addAccessPaths(walk(STOP_B, D2m))
      .addViaLocations(VIA_LOCATION_STOP_A_OR_B)
      .addEgressPaths(walk(STOP_C, D30s));

    // Both routes are pareto optimal.
    // Route 2 is faster, but it contains more walking
    // Verify that both routes are included as a valid result
    assertEquals(
      PathUtils.join(
        "Walk 2m ~ B ~ BUS R2 0:05 0:14 ~ C ~ Walk 30s [0:03 0:14:30 11m30s Tₓ0 C₁1_440]",
        "Walk 30s ~ A ~ BUS R1 0:04 0:15 ~ C ~ Walk 30s [0:03:30 0:15:30 12m Tₓ0 C₁1_380]"
      ),
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Test minimum wait time")
  void testMinWaitTime() {
    var data = new TestTransitData();
    data.withRoutes(
      route("R1", STOP_A, STOP_B).withTimetable(schedule("0:02:00 0:04:00")),
      route("R2", STOP_B, STOP_C)
        .withTimetable(schedule("0:05:44 0:10"), schedule("0:05:45 0:11"), schedule("0:05:46 0:12"))
    );

    var requestBuilder = prepareRequest();
    var minWaitTime = Duration.ofSeconds(45);

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addViaLocations(List.of(RaptorViaLocation.via("B", minWaitTime).addViaStop(STOP_B).build()))
      .addEgressPaths(walk(STOP_C, D30s));

    // We expect to bard the second trip at 0:05:45, since the minWaitTime is 45s and the
    // transfer slack is 60s.
    assertEquals(
      "Walk 30s ~ A ~ BUS R1 0:02 0:04 ~ B ~ BUS R2 0:05:45 0:11 ~ C ~ Walk 30s " +
      "[0:01:30 0:11:30 10m Tₓ1 C₁1_860]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  private static RaptorViaLocation viaLocation(String label, int... stops) {
    var builder = RaptorViaLocation.via(label);
    Arrays.stream(stops).forEach(builder::addViaStop);
    return builder.build();
  }
}
