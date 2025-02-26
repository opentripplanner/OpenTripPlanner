package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.RaptorTestConstants.D1m;
import static org.opentripplanner.raptor._data.RaptorTestConstants.D20s;
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
import static org.opentripplanner.raptor.api.request.RaptorViaLocation.via;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;
import org.opentripplanner.raptor.configure.RaptorConfig;

/**
 * FEATURE UNDER TEST
 *
 * Raptor should be able to handle route request with one or more via locations using transfers
 * . The via point is a coordinate/node in the street map, but Raptor only see this as a special
 * kind of transfer. If a stop is specified as via location in the request, then all the results
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
class J03_ViaTransferSearchTest {

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
      route("R1")
        .timetable(
          """
          A    B    C    D
          0:02 0:10 0:20 0:30
          0:12 0:20 0:30 0:40
          """
        )
    );

    var requestBuilder = prepareRequest();

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addViaLocation(
        RaptorViaLocation
          .via("B")
          .addViaTransfer(STOP_B, TestTransfer.transfer(STOP_B, D1m))
          .build()
      )
      .addEgressPaths(walk(STOP_D, D30s));

    var result = raptorService.route(requestBuilder.build(), data);

    // Verify that we alight the first trip at stop C and board the second trip
    assertEquals(
      "Walk 30s ~ A ~ BUS R1 0:02 0:10 ~ B ~ Walk 1m ~ B ~ BUS R1 0:20 0:40 ~ D ~ Walk 30s [0:01:30 0:40:30 39m Tₓ1 C₁3_660]",
      pathsToString(result)
    );
  }

  @Test
  @DisplayName(
    "Basic via search with just two routes. You should be forced use the provided via transfer " +
    "even when a better regular transfer exist and an eariler depature could be reached. "
  )
  void viaSearchArrivingByTransferAtViaStop() {
    var data = new TestTransitData();

    data.withRoutes(
      route("R1", STOP_A, STOP_B, STOP_D).withTimetable("""
        0:02 0:10 0:12
        """),
      route("R2", STOP_C, STOP_D, STOP_E)
        .withTimetable("""
        0:10 0:13 0:15
        0:12 0:15 0:17
        """)
    );

    var requestBuilder = prepareRequest();

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addViaLocation(via("BxC").addViaTransfer(STOP_B, TestTransfer.transfer(STOP_C, D1m)).build())
      .addEgressPaths(walk(STOP_E, D30s));

    var result = raptorService.route(requestBuilder.build(), data);

    // Verify that we alight the first trip at stop C and board the second trip
    assertEquals(
      "Walk 30s ~ A ~ BUS R1 0:02 0:10 ~ B ~ Walk 1m ~ C ~ BUS R2 0:12 0:17 ~ E ~ Walk 30s " +
      "[0:01:30 0:17:30 16m Tₓ1 C₁2_280]",
      pathsToString(result)
    );
  }

  @Test
  @DisplayName(
    "Via search with via transfer should force the usage of a route at the destination " +
    "avoiding using a via-transfer followed by a regular transfer."
  )
  void viaTransferSearchNotFolloedByRegularTransfer() {
    var data = new TestTransitData();

    data
      .withRoutes(
        route("R1", STOP_A, STOP_B).withTimetable("0:02 0:10"),
        route("R2", STOP_C, STOP_D).withTimetable("0:12 0:15"),
        route("R2", STOP_E, STOP_F).withTimetable("""
        0:15 0:17
        0:17 0:15
        """)
      )
      .withTransfer(STOP_C, transfer(STOP_E, D1m))
      .withTransfer(STOP_D, transfer(STOP_E, D1m));
    var requestBuilder = prepareRequest();

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addViaLocation(via("BxC").addViaTransfer(STOP_B, TestTransfer.transfer(STOP_C, D1m)).build())
      .addEgressPaths(walk(STOP_F, D30s));

    var result = raptorService.route(requestBuilder.build(), data);

    // Verify that we alight the first trip at stop C and board the second trip
    assertEquals(
      "Walk 30s ~ A ~ " +
      "BUS R1 0:02 0:10 ~ B ~ Walk 1m ~ C ~ " +
      "BUS R2 0:12 0:15 ~ D ~ Walk 1m ~ E ~ " +
      "BUS R2 0:17 0:15 ~ F ~ Walk 30s " +
      "[0:01:30 0:15:30 14m Tₓ2 C₁2_820]",
      pathsToString(result)
    );
  }

  @Test
  @DisplayName("Test minimum wait time")
  void testMinWaitTime() {
    var data = new TestTransitData();
    data.withRoutes(
      route("R1", STOP_A, STOP_B).withTimetable("0:02:00 0:04:00"),
      route("R2")
        .timetable(
          """
        B        C
        0:05:44  0:10
        0:05:45  0:11
        0:05:46  0:12
        """
        )
    );

    var requestBuilder = prepareRequest();
    var minWaitTime = Duration.ofSeconds(25);

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addViaLocations(
        List.of(
          RaptorViaLocation
            .via("B", minWaitTime)
            .addViaTransfer(STOP_B, transfer(STOP_B, D20s))
            .build()
        )
      )
      .addEgressPaths(walk(STOP_C, D30s));

    // We expect to bard the second trip at 0:05:45, since the minWaitTime is 45s and the
    // transfer slack is 60s.
    assertEquals(
      "Walk 30s ~ A ~ BUS R1 0:02 0:04 ~ B ~ Walk 20s ~ B ~ BUS R2 0:05:45 0:11 ~ C ~ Walk 30s " +
      "[0:01:30 0:11:30 10m Tₓ1 C₁1_880]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }
}
