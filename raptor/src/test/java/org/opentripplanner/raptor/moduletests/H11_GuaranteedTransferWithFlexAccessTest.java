package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.withoutCost;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION_REV;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorCostConverter;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should support combining multiple features, like Flexible access paths and constrained
 * transfers. This test has only one path available, and it is expected that it should be returned
 * irrespective of the profile.
 */
public class H11_GuaranteedTransferWithFlexAccessTest implements RaptorTestConstants {

  private static final int C1_ONE_STOP = RaptorCostConverter.toRaptorCost(2 * 60);

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    var r1 = route("R1", STOP_B, STOP_C).withTimetable(schedule("0:30 0:45"));
    var r2 = route("R2", STOP_C, STOP_D).withTimetable(schedule("0:45 0:55"));

    var tripA = r1.timetable().getTripSchedule(0);
    var tripB = r2.timetable().getTripSchedule(0);

    data.withRoutes(r1, r2);
    data.withGuaranteedTransfer(tripA, STOP_C, tripB, STOP_C);
    data.withTransfer(STOP_A, TestTransfer.transfer(STOP_B, D10m));
    data.withTransferCost(100);

    requestBuilder
      .searchParams()
      .addAccessPaths(flex(STOP_A, D3m, ONE_RIDE, 2 * C1_ONE_STOP))
      .addEgressPaths(walk(STOP_D, D1m));

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00)
      .constrainedTransfers(true);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> testCases() {
    var expected =
      "Flex 3m 1x ~ A " +
      "~ Walk 10m ~ B " +
      "~ BUS R1 0:30 0:45 ~ C " +
      "~ BUS R2 0:45 0:55 ~ D " +
      "~ Walk 1m " +
      "[0:16 0:56 40m Tₓ2 C₁3_820]";

    return RaptorModuleTestCase.of()
      .add(TC_MIN_DURATION, "[0:00 0:40 40m Tₓ2]")
      .add(TC_MIN_DURATION_REV, "[0:20 1:00 40m Tₓ2]")
      .add(standard(), withoutCost(expected))
      .add(multiCriteria(), expected)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
