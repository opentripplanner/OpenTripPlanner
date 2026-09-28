package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_A;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_B;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_C;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_D;
import static org.opentripplanner.raptor._data.RaptorTestConstants.T00_00;
import static org.opentripplanner.raptor._data.RaptorTestConstants.T01_00;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor.api.request.via.RaptorViaLocation.passThrough;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.request.via.RaptorViaLocation;
import org.opentripplanner.raptor.configure.RaptorTestFactory;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should be able to handle a route request with a specified pass-through point.
 * If a stop point is specified as a pass-through point in the request, then all the results
 * returned from Raptor should include this stop point either as an alight or board point for a
 * trip or as an intermediate point in the trip.
 * <p>
 * It should be possible to specify more than one pass-through point. The result should include
 * stop points in the order in which they were specified in the request. Only alternatives that
 * pass through all stop points should be included in the result.
 * <p>
 * In order to support stop areas raptor should also support multiple stop points in the same
 * pass-through group. It should be possible to define both stop A and B as a pass-through. Then
 * alternatives that pass either stop A or B should not be dropped.
 */
class J01_PassThroughTest {

  static final RaptorViaLocation PASS_THROUGH_STOP_A = passThrough("A").addStop(STOP_A).build();
  static final RaptorViaLocation PASS_THROUGH_STOP_C = passThrough("C").addStop(STOP_C).build();
  static final RaptorViaLocation PASS_THROUGH_STOP_D = passThrough("D").addStop(STOP_D).build();
  static final List<RaptorViaLocation> PASS_THROUGH_STOP_B_OR_C = List.of(
    passThrough("B|C").addStop(STOP_B, STOP_C).build()
  );
  static final List<RaptorViaLocation> PASS_THROUGH_STOP_B_THEN_C = List.of(
    passThrough("B").addStop(STOP_B).build(),
    passThrough("C").addStop(STOP_C).build()
  );
  static final List<RaptorViaLocation> PASS_THROUGH_STOP_B_THEN_D = List.of(
    passThrough("B").addStop(STOP_B).build(),
    passThrough("D").addStop(STOP_D).build()
  );

  private final TestTransitData data = new TestTransitData();

  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  private RaptorRequestBuilder<TestTripSchedule> prepareRequest() {
    var builder = data.requestBuilder();

    builder
      .profile(RaptorProfile.MULTI_CRITERIA)
      // TODO: 2023-07-24 Currently heuristics does not work with pass-through so we
      //  have to turn them off. Make sure to re-enable optimization later when it's fixed
      .clearOptimizations();

    builder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .searchWindow(Duration.ofMinutes(8))
      .timetable(true);

    return builder;
  }

  @Test
  @DisplayName("Pass-through stop point as a last point in the journey.")
  void passThroughPointOnEgress() {
    // Create two routes.
    // Only one of them includes required pass-through point.
    // Pass-through point is the last stop in the trip.
    // The trip with pass-through point has significantly longer travel time so that normally it
    //  should not be used
    data.withTimetables(
      """
      -- R1
      A     B     C
      0:02  0:05  0:20
      -- R2
      A     B           D
      0:02  0:10        0:50
      """
    );

    data.access("Walk 30s ~ A").egress("D ~ Walk 30s", "C ~ Walk 30s");

    var requestBuilder = prepareRequest();

    requestBuilder.searchParams().addViaLocation(PASS_THROUGH_STOP_D);

    // Verify that only the journey with pass-through stop point is included in response
    assertEquals(
      "Walk 30s ~ A ~ BUS R2 0:02 0:50 ~ D ~ Walk 30s [0:01:30 0:50:30 49m Tₙ0 C₁3_600]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Pass-through stop point as a first point in the journey.")
  void passThroughPointOnAccess() {
    data
      .access("Walk 30s ~ A", "Walk 30s ~ B")
      // Create two routes.
      // Only one of them includes required pass-through point.
      // Pass-through point is the first stop in the trip.
      // The trip with pass-through point has significantly longer travel time so that normally it
      //  should not be used
      .withTimetables(
        """
              B     C     D
              0:02  0:05  0:20
        ---
        A           C     D
        0:02        0:10  0:50
        """
      )
      .egress("D ~ Walk 30s");

    var requestBuilder = prepareRequest();

    requestBuilder.searchParams().addViaLocation(PASS_THROUGH_STOP_A);

    // Verify that only the journey with pass-through stop point is included in response
    assertEquals(
      "Walk 30s ~ A ~ BUS R2 0:02 0:50 ~ D ~ Walk 30s [0:01:30 0:50:30 49m Tₙ0 C₁3_600]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Pass-through stop point as an intermediate point in the journey.")
  void passThroughPointInTheMiddle() {
    // Create two routes.
    // Only one of them includes required pass-through point.
    // Pass-through point is the intermediate stop in the trip.
    // The trip with pass-through point has significantly longer travel time so that normally it
    //  should not be used
    data.withTimetables(
      """
      -- R1
      A     B     D
      0:02  0:05  0:20
      -- R2
      A     C     D
      0:02  0:10  0:50
      """
    );
    data.withTransferCost(100);
    data.access("Walk 30s ~ A").egress("D ~ Walk 30s");

    var requestBuilder = prepareRequest();

    requestBuilder.searchParams().addViaLocation(PASS_THROUGH_STOP_C);

    // Verify that only the journey with pass-through stop point is included in response
    assertEquals(
      "Walk 30s ~ A ~ BUS R2 0:02 0:50 ~ D ~ Walk 30s [0:01:30 0:50:30 49m Tₙ0 C₁3_600]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Multiple pass-through stop points")
  void multiplePassThroughPoints() {
    // Create two routes.
    // First one includes one pass-through stop point.
    // The second one includes the second pass-through point.
    // Both arrive at the desired destination so normally there should not be any transfers.
    data.withTimetables(
      """
      -- R1
      A     B     C                 F
      0:02  0:05  0:10              0:20
      -- R2
                  C     D     E     F
                  0:15  0:20  0:30  0:50
      """
    );

    data.withTransferCost(100);
    data.access("Walk 30s ~ A").egress("F ~ Walk 30s");

    var requestBuilder = prepareRequest();

    requestBuilder.searchParams().addViaLocations(PASS_THROUGH_STOP_B_THEN_D);

    // Verify that Raptor generated journey with a transfer to r2 so that both pass-through points
    //  are included
    assertEquals(
      "Walk 30s ~ A ~ BUS R1 0:02 0:10 ~ C ~ BUS R2 0:15 0:50 ~ F ~ Walk 30s [0:01:30 0:50:30 49m Tₙ1 C₁4_300]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Pass-through order")
  void passThroughOrder() {
    // Create two routes.
    // Both include all the desired pass-through stop points but only one of them have correct order.
    data.withTimetables(
      """
      R1
      A     B     C     D
      0:05  0:10  0:15  0:20
      --
      R2
      A     C     B     D
      0:05  0:10  0:15  0:17
      """
    );

    data.access("Walk 30s ~ A").egress("D ~ Walk 30s");

    var requestBuilder = prepareRequest();

    requestBuilder.searchParams().addViaLocations(PASS_THROUGH_STOP_B_THEN_C);

    // Verify that only route with correct pass-through order is returned
    assertEquals(
      "Walk 30s ~ A ~ BUS R1 0:05 0:20 ~ D ~ Walk 30s [0:04:30 0:20:30 16m Tₙ0 C₁1_620]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName("Multiple stops in same pass-through group")
  void passThroughGroup() {
    // Create two routes, route one includes STOP_B and route two includes STOP_C.
    // Both stops will be part of the same pass-through group so that both routes should be pareto
    // optimal. R2 is faster, but it contains more walk so R1 is better on cost(only 2 cost points).
    data.withTimetables(
      """
      R1
      A     C     E
      0:04  0:10  0:15
      --
      R2
      B     D     E
      0:05  0:10  0:14
      """
    );

    // NOTE! We keep the cost calculation tight (2 cost points in favor of R1) to ensure the
    //       cost is handled correct in all steps during the algorithm, not just at the end where
    //       we compare paths. The cost is "recalculated" in the PathMapper, so by keeping it tight
    //       we are more likely to discover mistakes in the algorithm - if the cost is off.
    //
    // COST CALCULATION
    //
    //  |            | R1             | R2             |
    //  | Access     | 59s  118   118 |  2m  240   240 |
    //  | board-cost |      600   718 |      600   840 |
    //  | Transit    | 11m  660  1378 |  9m  540  1380 |
    //  | Egress     | 30s   60  1438 | 30s   60  1440 |
    //
    data.access("Walk 59s ~ A", "Walk 2m ~ B").egress("E ~ Walk 30s");

    var requestBuilder = prepareRequest();

    requestBuilder.searchParams().addViaLocations(PASS_THROUGH_STOP_B_OR_C);

    assertEquals(
      """
      Walk 2m ~ B ~ BUS R2 0:05 0:14 ~ E ~ Walk 30s [0:03 0:14:30 11m30s Tₙ0 C₁1_440]
      Walk 59s ~ A ~ BUS R1 0:04 0:15 ~ E ~ Walk 30s [0:03:01 0:15:30 12m29s Tₙ0 C₁1_438]
      """.trim(),
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName(
    """
    When the pass-through stop is in the middle of a loop, the journey should board before it
    and alight after it. For example with pattern A-B-C-A-B, board at A, pass-through at C, and
    alight at B, then the journey must bard at A and ride the whole pattern to B. It can not
    board at the second time it passes A, because the pass-through stop is missed.
    """
  )
  @Disabled(
    """
    This test fails, because the PathMapper does not know if we should board at the first or
    second time the trip visit stop A. The arrival state does not carry enough information
    to determine this. In case there is no pass-though stop the algoritm should board at the
    second pass to allow for a late depature and short duration.
    """
  )
  void passThroughOnLoopRouteWithPassThroughStopInTheMiddle() {
    data.withTimetables(
      """
      Loop
      A     B     C     A     B
      0:02  0:04  0:06  0:08  0:10
      """
    );
    data.access("Free ~ A").egress("B ~ Free");

    var requestBuilder = prepareRequest();
    requestBuilder.searchParams().addViaLocation(PASS_THROUGH_STOP_C);

    assertEquals(
      "A ~ BUS Loop 0:02 0:10 ~ B [0:02 0:10 8m Tₙ1 C₁1_040]",
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }

  @Test
  @DisplayName(
    "The pass-through is implemented with listeners at the alight stop. If another path exists at " +
      "the same stop, then this should have no effect on the pass-through connection."
  )
  void passThroughStopVisitShouldNotBeDominatedByAnotherPath() {
    // Create two routes that both arrive at stop C at the same time. R1 is faster and leaves
    // from stop A later. Hence it will be part of an earlier RangeRaptor iteration - and the
    // arrival at stop C is better than route R2.
    data.withTimetables(
      """
      -- R1
      A     C
      0:02  0:10
      -- R2
      A     C     E
      0:00  0:10  0:15
      """
    );
    data.access("Free ~ A").egress("E ~ Free");

    var requestBuilder = prepareRequest();

    requestBuilder.searchParams().addViaLocation(PASS_THROUGH_STOP_C);

    // R2 is the only path which takes you all the way to the destination, so the state should be
    // copied over from segment 1 to segment 2 after stop C is "passed-through". Even when R1 ≺ R2
    // at stop arrival at stop C. R1 is optimal compared to R2 at stop arrival C, because the cost
    // and departure time is better, while number of transfers and arrival time is the same.
    assertEquals(
      """
      A ~ BUS R2 0:00 0:15 ~ E [0:00 0:15 15m Tₙ0 C₁1_500]
      """.trim(),
      pathsToString(raptorService.route(requestBuilder.build(), data))
    );
  }
}
