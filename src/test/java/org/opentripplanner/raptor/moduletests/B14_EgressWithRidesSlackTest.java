package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.time.TimeUtils.hm2time;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walkCost;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;

/**
 * FEATURE UNDER TEST
 * <p>
 * With FLEX egress Raptor must support egress paths with at least one leg. These egress legs should
 * only be boarded if there is enough slack to wait for the opening times to still be open after the
 * slack time has passed from the arrival of the vehicle.
 */
public class B14_EgressWithRidesSlackTest implements RaptorTestConstants {

  // First access/egress is possible, second one is too tight
  private static final RaptorAccessEgress[] WALK_ACCESSES = {
    TestAccessEgress.flexAndWalk(STOP_B, D2m, 1, walkCost(D2m), T00_02, T00_02),
    TestAccessEgress.flexAndWalk(STOP_B, D2m, 1, walkCost(D2m), hm2time(0, 5), hm2time(0, 5)),
  };
  private static final RaptorAccessEgress[] WALK_EGRESSES = {
    TestAccessEgress.flexAndWalk(STOP_C, D2m, 1, walkCost(D2m), hm2time(0, 26), hm2time(0, 26)),
    TestAccessEgress.flexAndWalk(STOP_C, D2m, 1, walkCost(D2m), hm2time(0, 21), hm2time(0, 21)),
  };
  private static final RaptorAccessEgress[] ONBOARD_ACCESSES = {
    TestAccessEgress.flex(STOP_B, D2m, 1, walkCost(D2m), T00_02, T00_02),
    TestAccessEgress.flex(STOP_B, D2m, 1, walkCost(D2m), hm2time(0, 5), hm2time(0, 5)),
  };
  private static final RaptorAccessEgress[] ONBOARD_EGRESSES = {
    TestAccessEgress.flex(STOP_C, D2m, 1, walkCost(D2m), hm2time(0, 26), hm2time(0, 26)),
    TestAccessEgress.flex(STOP_C, D2m, 1, walkCost(D2m), hm2time(0, 21), hm2time(0, 21)),
  };
  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  void setup() {
    data.withRoute(route("R1", STOP_B, STOP_C).withTimetable(schedule("0:10, 0:20")));
    data.withSlackProvider(new DefaultSlackProvider(5 * 60, 0, 0));

    requestBuilder.searchParams().earliestDepartureTime(T00_00).latestArrivalTime(hm2time(25, 0));

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> testCases() {
    return RaptorModuleTestCase
      .of()
      .add(standard(), "Flex 2m 1x ~ B ~ BUS R1 0:10 0:20 ~ C ~ Flex 2m 1x [0:02 0:28 26m 2tx]")
      .add(
        multiCriteria(),
        "Flex 2m 1x ~ B ~ BUS R1 0:10 0:20 ~ C ~ Flex 2m 1x [0:02 0:28 26m 2tx $2400]"
      )
      .build();
  }

  static List<RaptorModuleTestCase> testCasesImpossible() {
    return RaptorModuleTestCase.of().add(standard(), "").add(multiCriteria(), "").build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testWithWalking(RaptorModuleTestCase testCase) {
    requestBuilder.searchParams().addAccessPaths(WALK_ACCESSES).addEgressPaths(WALK_EGRESSES);

    var request = testCase.withConfig(requestBuilder);
    var response = raptorService.route(request, data);
    assertEquals(testCase.expected(), pathsToString(response));
  }

  @ParameterizedTest
  @MethodSource("testCasesImpossible")
  void testWithWalkingImpossible(RaptorModuleTestCase testCase) {
    requestBuilder.searchParams().addAccessPaths(WALK_ACCESSES[1]).addEgressPaths(WALK_EGRESSES[1]);

    var request = testCase.withConfig(requestBuilder);
    var response = raptorService.route(request, data);
    assertEquals(testCase.expected(), pathsToString(response));
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testArriveAtStop(RaptorModuleTestCase testCase) {
    requestBuilder.searchParams().addAccessPaths(ONBOARD_ACCESSES).addEgressPaths(ONBOARD_EGRESSES);

    var request = testCase.withConfig(requestBuilder);
    var response = raptorService.route(request, data);
    assertEquals(testCase.expected(), pathsToString(response));
  }

  @ParameterizedTest
  @MethodSource("testCasesImpossible")
  void testArriveAtStopImpossible(RaptorModuleTestCase testCase) {
    requestBuilder
      .searchParams()
      .addAccessPaths(ONBOARD_ACCESSES[1])
      .addEgressPaths(ONBOARD_EGRESSES[1]);

    var request = testCase.withConfig(requestBuilder);
    var response = raptorService.route(request, data);
    assertEquals(testCase.expected(), pathsToString(response));
  }
}
