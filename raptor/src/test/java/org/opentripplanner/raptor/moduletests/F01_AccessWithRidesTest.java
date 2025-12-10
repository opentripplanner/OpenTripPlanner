package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION_REV;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_ONE;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorCostConverter;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;
import org.opentripplanner.raptor.spi.TestSlackProvider;

/**
 * FEATURE UNDER TEST
 * <p>
 * With FLEX access Raptor must support access paths with more than one leg. These access paths have
 * more transfers that regular paths, hence should not dominate access walking, but only get
 * accepted when they are better on time and/or cost.
 */
public class F01_AccessWithRidesTest implements RaptorTestConstants {

  private static final int TRANSFER_SLACK = 60;
  private static final int C1_ONE_STOP = RaptorCostConverter.toRaptorCost(2 * 60);
  private static final int C1_TRANSFER_SLACK = RaptorCostConverter.toRaptorCost(TRANSFER_SLACK);
  private static final int C1_ONE_SEC = RaptorCostConverter.toRaptorCost(1);

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @BeforeEach
  void setup() {
    data
      .access(
        // lowest num-of-transfers (0)
        "Walk 10m C₁180 ~ B",
        // lowest cost
        "Flex+Walk 2m Rₙ2 C₁239 ~ C",
        // latest departure time
        "Flex 3m Rₙ2 C₁360 ~ D",
        // best on combination of transfers and time
        "Flex+Walk 7m Rₙ1 C₁480 ~ E"
      )
      .withTimetables(
        """
        B     C     D     E     F
        0:10  0:12  0:14  0:16  0:20
        """
      )
      .egress("F ~ Walk 1m");

    // We will test board- and alight-slack in a separate test
    data.withSlackProvider(new TestSlackProvider(TRANSFER_SLACK, 0, 0));
    requestBuilder.searchParams().earliestDepartureTime(T00_00).latestArrivalTime(T00_30);

    ModuleTestDebugLogging.setupDebugLogging(data);
  }

  static List<RaptorModuleTestCase> testCases() {
    String expFlexAccess = "Flex 3m Rₙ2 ~ D ~ BUS R1 0:14 0:20 ~ F ~ Walk 1m [0:10 0:21 11m Tₙ2]";
    String expWalkAccess = "Walk 10m ~ B ~ BUS R1 0:10 0:20 ~ F ~ Walk 1m [0:00 0:21 21m Tₙ0]";
    return RaptorModuleTestCase.of()
      // TODO - Why do we get only one result here - when there is 3 different pareto-optimal
      //      - paths
      .add(TC_MIN_DURATION, "[0:00 0:11 11m Tₙ0]")
      // Return pareto optimal paths with 0, 1 and 2 num-of-transfers
      .add(TC_MIN_DURATION_REV, "[0:19 0:30 11m Tₙ2]", "[0:17 0:30 13m Tₙ1]", "[0:09 0:30 21m Tₙ0]")
      .add(standard().not(TC_STANDARD_ONE), expFlexAccess)
      // First boarding wins with one-iteration (apply to min-duration and std-one)
      .add(TC_STANDARD_ONE, expWalkAccess)
      .add(
        multiCriteria(),
        // Latest departure time
        "Flex 3m Rₙ2 ~ D ~ BUS R1 0:14 0:20 ~ F ~ Walk 1m [0:10 0:21 11m Tₙ2 C₁1_500]",
        // Lowest cost
        "Flex+Walk 2m Rₙ2 ~ C ~ BUS R1 0:12 0:20 ~ F ~ Walk 1m [0:09 0:21 12m Tₙ2 C₁1_499]",
        // Lowest num-of-transfers + time
        "Flex+Walk 7m Rₙ1 ~ E ~ BUS R1 0:16 0:20 ~ F ~ Walk 1m [0:08 0:21 13m Tₙ1 C₁1_500]",
        // Lowest num-of-transfers
        "Walk 10m ~ B ~ BUS R1 0:10 0:20 ~ F ~ Walk 1m [0:00 0:21 21m Tₙ0 C₁1_500]"
      )
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
