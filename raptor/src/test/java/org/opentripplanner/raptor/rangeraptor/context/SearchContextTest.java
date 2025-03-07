package org.opentripplanner.raptor.rangeraptor.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor.api.model.SearchDirection.FORWARD;
import static org.opentripplanner.raptor.api.model.SearchDirection.REVERSE;
import static org.opentripplanner.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.raptor.api.request.RaptorProfile.STANDARD;
import static org.opentripplanner.raptor.rangeraptor.context.SearchContext.accessOrEgressPaths;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_ARRIVAL_TIME;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_DEPARTURE_TIME;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_TIMETABLE;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime;

class SearchContextTest implements RaptorTestConstants {

  public static final TestAccessEgress ANY_WALK = TestAccessEgress.walk(1, 1);
  private final boolean GET_ACCESS = true;
  private final boolean GET_EGRESS = false;
  private final RaptorAccessEgress PATH_A_10s = TestAccessEgress.walk(STOP_A, D10s);
  private final RaptorAccessEgress PATH_A_11s = TestAccessEgress.walk(STOP_A, D11s);
  private final RaptorAccessEgress PATH_B = TestAccessEgress.walk(STOP_B, D1m);
  private final RaptorAccessEgress PATH_C_30s = TestAccessEgress.walk(STOP_C, D30s);
  private final RaptorAccessEgress PATH_C_40s = TestAccessEgress.walk(STOP_C, D40s);
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();

  @BeforeEach
  void setup() {
    requestBuilder.searchParams().earliestDepartureTime(0);
  }

  @Test
  void accessOrEgressPathsHandlesOneElementPaths() {
    // given:
    var emptyPaths = requestBuilder
      .searchParams()
      .addAccessPaths(PATH_A_10s)
      .addEgressPaths(PATH_C_30s)
      .build()
      .searchParams();

    assertEquals(List.of(PATH_A_10s), accessOrEgressPaths(GET_ACCESS, STANDARD, emptyPaths));
    assertEquals(List.of(PATH_A_10s), accessOrEgressPaths(GET_ACCESS, MULTI_CRITERIA, emptyPaths));
    assertEquals(List.of(PATH_C_30s), accessOrEgressPaths(GET_EGRESS, STANDARD, emptyPaths));
    assertEquals(List.of(PATH_C_30s), accessOrEgressPaths(GET_EGRESS, MULTI_CRITERIA, emptyPaths));
  }

  @Test
  void accessOrEgressPathsHandlesDuplicatesPaths() {
    var emptyPaths = requestBuilder
      .searchParams()
      .addAccessPaths(PATH_A_10s, PATH_A_11s, PATH_B)
      .addEgressPaths(PATH_B, PATH_C_40s, PATH_C_30s)
      .build()
      .searchParams();

    assertEquals(
      List.of(PATH_A_10s, PATH_B),
      sort(accessOrEgressPaths(GET_ACCESS, STANDARD, emptyPaths))
    );
    assertEquals(
      List.of(PATH_A_10s, PATH_A_11s, PATH_B),
      sort(accessOrEgressPaths(GET_ACCESS, MULTI_CRITERIA, emptyPaths))
    );
    assertEquals(
      List.of(PATH_B, PATH_C_30s),
      sort(accessOrEgressPaths(GET_EGRESS, STANDARD, emptyPaths))
    );
    assertEquals(
      List.of(PATH_B, PATH_C_30s, PATH_C_40s),
      sort(accessOrEgressPaths(GET_EGRESS, MULTI_CRITERIA, emptyPaths))
    );
  }

  private static List<RaptorAccessEgress> sort(Collection<RaptorAccessEgress> c) {
    return c
      .stream()
      .sorted(Comparator.comparingInt(it -> it.stop() * 10_000 + it.durationInSeconds()))
      .collect(Collectors.toList());
  }

  static List<Arguments> paretoSetTimeConfigTestCase() {
    return List.of(
      Arguments.of(USE_TIMETABLE, true, false, FORWARD),
      Arguments.of(USE_TIMETABLE, true, false, REVERSE),
      Arguments.of(USE_DEPARTURE_TIME, false, true, FORWARD),
      Arguments.of(USE_DEPARTURE_TIME, false, false, REVERSE),
      Arguments.of(USE_ARRIVAL_TIME, false, true, REVERSE),
      Arguments.of(USE_ARRIVAL_TIME, false, false, FORWARD)
    );
  }

  @ParameterizedTest
  @MethodSource("paretoSetTimeConfigTestCase")
  void testParetoSetTimeConfig(
    ParetoSetTime expected,
    boolean timetable,
    boolean prefLateArrival,
    SearchDirection searchDirection
  ) {
    assertEquals(
      expected,
      SearchContext.paretoSetTimeConfig(
        new RaptorRequestBuilder()
          .searchParams()
          // EDT, LAT, access and egress is required, but not used
          .addAccessPaths(ANY_WALK)
          .addEgressPaths(ANY_WALK)
          .earliestDepartureTime(12)
          .latestArrivalTime(120)
          // Relevant parameters
          .timetable(timetable)
          .preferLateArrival(prefLateArrival)
          .build()
          .searchParams(),
        searchDirection
      )
    );
  }
}
