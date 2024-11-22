package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptorlegacy._data.transit.TestAccessEgress;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.request.via.PassThroughViaLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

class RaptorRequestMapperTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final StopLocation STOP_A = TEST_MODEL.stop("Stop:A").build();
  private static final List<RaptorAccessEgress> ACCESS = List.of(TestAccessEgress.walk(12, 45));
  private static final List<RaptorAccessEgress> EGRESS = List.of(TestAccessEgress.walk(144, 54));

  private static final CostLinearFunction R1 = CostLinearFunction.of("50 + 1.0x");
  private static final CostLinearFunction R2 = CostLinearFunction.of("0 + 1.5x");
  private static final CostLinearFunction R3 = CostLinearFunction.of("30 + 2.0x");

  private static final Map<FeedScopedId, StopLocation> STOPS_MAP = Map.of(STOP_A.getId(), STOP_A);

  static List<Arguments> testCasesRelaxedCost() {
    return List.of(
      Arguments.of(CostLinearFunction.NORMAL, 0, 0),
      Arguments.of(CostLinearFunction.NORMAL, 10, 10),
      Arguments.of(R1, 0, 5000),
      Arguments.of(R1, 7, 5007),
      Arguments.of(R2, 0, 0),
      Arguments.of(R2, 100, 150),
      Arguments.of(R3, 0, 3000),
      Arguments.of(R3, 100, 3200)
    );
  }

  @ParameterizedTest
  @MethodSource("testCasesRelaxedCost")
  void mapRelaxCost(CostLinearFunction input, int cost, int expected) {
    var calcCost = RaptorRequestMapper.mapRelaxCost(input);
    assertEquals(expected, calcCost.relax(cost));
  }

  @Test
  void testViaLocation() {
    var req = new RouteRequest();
    var minWaitTime = Duration.ofMinutes(13);

    req.setViaLocations(
      List.of(new VisitViaLocation("Via A", minWaitTime, List.of(STOP_A.getId()), List.of()))
    );

    var result = map(req);

    assertTrue(result.searchParams().hasViaLocations());
    assertEquals(
      "[Via{label: Via A, minWaitTime: 13m, connections: [0 13m]}]",
      result.searchParams().viaLocations().toString()
    );
  }

  @Test
  void testPassThroughPoints() {
    var req = new RouteRequest();

    req.setViaLocations(List.of(new PassThroughViaLocation("Via A", List.of(STOP_A.getId()))));

    var result = map(req);

    assertTrue(result.multiCriteria().hasPassThroughPoints());
    assertEquals(
      "[(Via A, stops: " + STOP_A.getIndex() + ")]",
      result.multiCriteria().passThroughPoints().toString()
    );
  }

  @Test
  void testTransitGroupPriority() {
    var req = new RouteRequest();

    // Set relax transit-group-priority
    req.withPreferences(p ->
      p.withTransit(t -> t.withRelaxTransitGroupPriority(CostLinearFunction.of("30m + 1.2t")))
    );

    var result = map(req);

    assertFalse(result.multiCriteria().transitPriorityCalculator().isEmpty());
  }

  @Test
  void testVisitViaAllowsTransitGroupPriority() {
    var req = new RouteRequest();

    // Set visit-via and relax transit-group-priority
    req.setViaLocations(
      List.of(new VisitViaLocation("Via A", null, List.of(STOP_A.getId()), List.of()))
    );
    req.withPreferences(p ->
      p.withTransit(t -> t.withRelaxTransitGroupPriority(CostLinearFunction.of("30m + 1.2t")))
    );

    var result = map(req);

    assertFalse(result.multiCriteria().transitPriorityCalculator().isEmpty());
  }

  @Test
  void testPassThroughPointsTurnTransitGroupPriorityOff() {
    var req = new RouteRequest();

    // Set pass-through and relax transit-group-priority
    req.setViaLocations(List.of(new PassThroughViaLocation("Via A", List.of(STOP_A.getId()))));
    req.withPreferences(p ->
      p.withTransit(t -> t.withRelaxTransitGroupPriority(CostLinearFunction.of("30m + 1.2t")))
    );

    var result = map(req);

    //  transit-group-priority CANNOT be used with pass-through and is turned off...
    assertTrue(result.multiCriteria().transitPriorityCalculator().isEmpty());
  }

  private static RaptorRequest<TestTripSchedule> map(RouteRequest request) {
    return RaptorRequestMapper.mapRequest(
      request,
      ZonedDateTime.now(),
      false,
      ACCESS,
      EGRESS,
      null,
      id -> IntStream.of(STOPS_MAP.get(id).getIndex())
    );
  }
}
