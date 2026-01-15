package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.Optimization;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.rangeraptor.internalapi.Heuristics;
import org.opentripplanner.raptor.service.RangeRaptorDynamicSearch;

/**
 * Feature under test
 * <p>
 * Raptor should return a proper array for heuristic values, i.e, time and transfers
 */
public class I01_HeuristicTest implements RaptorTestConstants {

  // Any big negative number will do, but -1 is a legal value
  private static final int UNREACHED = -9999;
  private static final int[] BEST_TRANSFERS = { UNREACHED, 1, 0, 0, -1 };
  private static final int[] BEST_TIMES = {
    UNREACHED,
    //  Egress + R2     + Transfer + Slack + R1
    20 + 3 * 60 + 30 + 60 + 2 * 60,
    20 + 3 * 60 + 30,
    20 + 3 * 60,
    20,
  };

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();
  private final RaptorConfig<TestTripSchedule> config = RaptorTestFactory.configForTest();

  @BeforeEach
  public void setup() {
    data
      .access("Walk 30s ~ A")
      .withTimetables(
        """
        A      B
        00:01  00:03
        --
        C      D
        00:05  00:08
        """
      )
      .egress("D ~ Walk 20s");

    data.withTransfer(STOP_B, TestTransfer.transfer(STOP_C, D30s));

    requestBuilder.searchParams().earliestDepartureTime(T00_00).timetable(true);

    requestBuilder.profile(RaptorProfile.MULTI_CRITERIA);

    requestBuilder.optimizations().add(Optimization.PARETO_CHECK_AGAINST_DESTINATION);
  }

  @Test
  public void regular() {
    var request = requestBuilder.build();

    var search = new RangeRaptorDynamicSearch<>(config, data, null, request);

    search.route();

    var destinationHeuristics = search.getDestinationHeuristics();

    assertHeuristics(destinationHeuristics);
  }

  @Test
  public void withConstrainedTransfers() {
    requestBuilder.searchParams().constrainedTransfers(true);

    var request = requestBuilder.build();

    var search = new RangeRaptorDynamicSearch<>(config, data, null, request);

    search.route();

    Heuristics destinationHeuristics = search.getDestinationHeuristics();

    assertHeuristics(destinationHeuristics);
  }

  private void assertHeuristics(Heuristics destinationHeuristics) {
    assertNotNull(destinationHeuristics);

    assertArrayLessOrEqual(
      BEST_TRANSFERS,
      destinationHeuristics.bestNumOfTransfersToIntArray(UNREACHED),
      "best number of transfers"
    );
    assertArrayLessOrEqual(
      BEST_TIMES,
      destinationHeuristics.bestTravelDurationToIntArray(UNREACHED),
      "best times"
    );
  }

  private void assertArrayLessOrEqual(int[] expected, int[] actual, String arrayName) {
    assertNotNull(actual);
    assertEquals(expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertTrue(
        expected[i] >= actual[i],
        String.format(
          "Value %d is greater than %d for index %d in %s",
          actual[i],
          expected[i],
          i,
          arrayName
        )
      );
    }
  }
}
