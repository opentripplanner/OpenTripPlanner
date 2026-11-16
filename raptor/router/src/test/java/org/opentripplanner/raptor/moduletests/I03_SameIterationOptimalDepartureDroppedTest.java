package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MULTI_CRITERIA;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MULTI_CRITERIA_DEST_PRUNING;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.request.SearchParamsBuilder;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig;

/// FEATURE UNDER TEST
///
/// In some cases Raptor drops Pareto optimal results if the optimal departure happens within the
/// same Range Raptor iteration. Raptor only looks at the _iteration departure time_ (in most
/// cases), not the actual departure time. This test documents in which cases we drop,
/// and in which we keep, a path that is optimal on **duration**.
///
/// Notes:
/// - To support this, we would need to keep the actual departure time in the state, at a cost
///   to performance (memory/speed). Or, iterate over each second, instead of minutes.
/// - A proper fix is to compute all exact departure times using the access and all trips
///   an access can board. Then iterate over these departure times instead of iterating over
///   minutes.
///
/// **Test**
///
/// The test is set up so that R1 competes with R2+R3. R1 is better than R2+R3 in all criteria,
/// except departure-time/duration. The test covers both the destination pruning and the
/// destination arrival, but not the stop arrival. To test the qualify method (MC_DEST_PRUNING),
/// the arrival at stop D must happen in another iteration; Hence, the two trip in the journey.
public class I03_SameIterationOptimalDepartureDroppedTest implements RaptorTestConstants {

  private static final String TIME_TABLE = """
  -- R1
  A                   C
  00:10:10            00:20:00
  -- R2
  A         B
  00:10:50  00:15:00
  -- R3
            B         D
            00:16:00  00:20:10
  -- R4
                      E         F
                      00:22:00  00:23:00
  """;

  private static final Map<String, String> JOURNEYS = Map.of(
    "R1",
    "A ~ BUS R1 0:10:10 0:20 ~ C [0:10:10 0:20 9m50s Tₙ0 C₁1_190]",
    "R1+R4",
    "A ~ BUS R1 0:10:10 0:20 ~ C ~ Walk 10s ~ E ~ BUS R4 0:22 0:23 ~ F [0:10:10 0:23 12m50s Tₙ1 C₁1_980]",
    "R2+R3",
    "A ~ BUS R2 0:10:50 0:15 ~ B ~ BUS R3 0:16 0:20:10 ~ D [0:10:50 0:20:10 9m20s Tₙ1 C₁1_760]"
  );

  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  private final TestTransitData data = new TestTransitData()
    .access("Free ~ A")
    .withTimetables(TIME_TABLE)
    .withTransfer(STOP_C, TestTransfer.transfer(STOP_E, D10_s))
    .withTransfer(STOP_D, TestTransfer.transfer(STOP_E, D10_s));

  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();

  /// The "R2+R3" is the shortest duration and only survives in two cases. The reason they survive
  /// is due to the fact that we have duration as a criterion for non-timetable queries.
  /// - **STOP** (egress from F): R2+R3 is dropped by the destination pruning `qualify` check when
  ///   it arrives at STOP D.
  /// - **DEST** (egress from C and D): R1 and R2+R3 compete in the DESTINATION arrival pareto set.
  @ParameterizedTest()
  @CsvSource(
    value = """
    STOP   | TIMETABLE | DEPART_AFTER | MC_RAPTOR       | R1+R4
    STOP   | TIMETABLE | DEPART_AFTER | MC_DEST_PRUNING | R1+R4
    STOP   | TIMETABLE | ARRIVE_BY    | MC_RAPTOR       | R1+R4
    STOP   | TIMETABLE | ARRIVE_BY    | MC_DEST_PRUNING | R1+R4
    STOP   |           | DEPART_AFTER | MC_RAPTOR       | R1+R4
    STOP   |           | DEPART_AFTER | MC_DEST_PRUNING | R1+R4
    STOP   |           | ARRIVE_BY    | MC_RAPTOR       | R1+R4
    STOP   |           | ARRIVE_BY    | MC_DEST_PRUNING | R1+R4
    DEST   | TIMETABLE | DEPART_AFTER | MC_RAPTOR       | R1
    DEST   | TIMETABLE | DEPART_AFTER | MC_DEST_PRUNING | R1
    DEST   | TIMETABLE | ARRIVE_BY    | MC_RAPTOR       | R1
    DEST   | TIMETABLE | ARRIVE_BY    | MC_DEST_PRUNING | R1
    DEST   |           | DEPART_AFTER | MC_RAPTOR       | R1 R2+R3
    DEST   |           | DEPART_AFTER | MC_DEST_PRUNING | R1
    DEST   |           | ARRIVE_BY    | MC_RAPTOR       | R1 R2+R3
    DEST   |           | ARRIVE_BY    | MC_DEST_PRUNING | R1
    """,
    delimiter = '|'
  )
  void test(String test, String timetable, String arriveBy, String config, String expected) {
    testStopArrivalOrQualifyDestinationPruning(test);
    requestBuilder
      .searchParams()
      .timetable("TIMETABLE".equals(timetable))
      .searchWindowInSeconds(D20_m);

    addDepartAfterOrArriveBy(requestBuilder.searchParams(), arriveBy);

    var tc = new RaptorModuleTestCase(null, testConfig(config), expected(expected));

    assertEquals(tc.expected(), tc.run(raptorService, data, requestBuilder));
  }

  private void testStopArrivalOrQualifyDestinationPruning(String test) {
    data.egress(
      switch (test) {
        case "STOP" -> new String[] { "F ~ Free" };
        case "DEST" -> new String[] { "C ~ Free", "D ~ Free" };
        default -> throw new RuntimeException("Unknown test: " + test);
      }
    );
  }

  private static void addDepartAfterOrArriveBy(
    SearchParamsBuilder<?> searchParams,
    String arriveBy
  ) {
    if ("DEPART_AFTER".equals(arriveBy)) {
      searchParams.earliestDepartureTime(T00_02);
    } else if ("ARRIVE_BY".equals(arriveBy)) {
      searchParams.latestArrivalTime(T00_30);
    } else {
      throw new RuntimeException("Unknown arriveBy: " + arriveBy);
    }
  }

  private static RaptorModuleTestConfig testConfig(String config) {
    return switch (config) {
      case "MC_RAPTOR" -> TC_MULTI_CRITERIA;
      case "MC_DEST_PRUNING" -> TC_MULTI_CRITERIA_DEST_PRUNING;
      default -> throw new IllegalArgumentException("Unknown config: " + config);
    };
  }

  private static String expected(String keys) {
    return Arrays.stream(keys.trim().split("\\s+"))
      .map(JOURNEYS::get)
      .collect(Collectors.joining("\n"));
  }
}
