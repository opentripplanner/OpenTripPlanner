package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.join;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.api.PathUtils.withoutCost;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION_REV;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_ONE;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_REV;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_REV_ONE;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.minDuration;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;

/**
 * FEATURE UNDER TEST
 * <p>
 * This test focuses on on-foot and flex egresses. You are not allowed to have two walk legs after
 * each other, so depending on how you arrived at the stop where the egress starts, the walking
 * option might not be possible.
 * <p>
 * Test case:
 * <img src="images/F12.svg" width="432" height="212" />
 * <p>
 * <pre>
 * // Allowed paths
 * A ~ L1 ~ C ~ Walk ~ D
 * A ~ L1 ~ C ~ Flex ~ D
 * A ~ L2 ~ B ~ Walk 2m ~ C ~ Flex ~ D
 * // Not allowed
 * A ~ L2 ~ B ~ Walk 2m ~ C ~ Walk ~ D
 * </pre>
 * To alternate which egress leg is the best, we change the egress walk between 5 minutes (walking
 * is better than the path with flex) and 7 minutes (the path with flex egress becomes the fastest
 * option). Note! There is 1 minute transfer slack.
 */
public class F12_EgressWithRidesMultipleOptimalPaths implements RaptorTestConstants {

  private static final String EXPECTED_PATH_FLEX_7M =
    "A ~ BUS R2 0:05 0:16 ~ B ~ Walk 2m ~ C ~ Flex 7m 1x [0:05 0:26 21m 1tx $2160]";

  private static final String EXPECTED_PATH_WALK_5M =
    "A ~ BUS R1 0:04 0:20 ~ C ~ Walk 5m [0:04 0:25 21m 0tx $2160]";

  private static final String EXPECTED_PATH_WALK_7M =
    "A ~ BUS R1 0:04 0:20 ~ C ~ Walk 7m [0:04 0:27 23m 0tx $2400]";

  private static final int COST_10m = RaptorCostConverter.toRaptorCost(D10m);

  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );
  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();

  @BeforeEach
  public void setup() {
    data.withRoutes(
      route("R1", STOP_A, STOP_C).withTimetable(schedule("0:04 0:20")),
      route("R2", STOP_A, STOP_B).withTimetable(schedule("0:05 0:16"))
    );

    // We will test board- and alight-slack in a separate test
    data.withSlackProvider(new DefaultSlackProvider(D1m, D0s, D0s));

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .searchWindowInSeconds(D20m)
      .latestArrivalTime(T00_30);

    requestBuilder.searchParams().addAccessPaths(TestAccessEgress.free(STOP_A));

    data.withTransfer(STOP_B, TestTransfer.transfer(STOP_C, D2m));

    // Set ModuleTestDebugLogging.DEBUG=true to enable debugging output
    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> withFlexAsBestOptionTestCases() {
    return RaptorModuleTestCase
      .of()
      // min-duration is added to verify that we get a result, the min-duration is not part of
      // the destination criteria; Hence we can not verify that the min-duration is correct
      .add(TC_MIN_DURATION, join(withoutCost(EXPECTED_PATH_FLEX_7M)))
      .add(TC_MIN_DURATION_REV, join(withoutCost(EXPECTED_PATH_FLEX_7M, EXPECTED_PATH_WALK_7M)))
      .add(TC_STANDARD, join(withoutCost(EXPECTED_PATH_FLEX_7M, EXPECTED_PATH_WALK_7M)))
      .add(TC_STANDARD_ONE, join(withoutCost(EXPECTED_PATH_FLEX_7M)))
      .add(TC_STANDARD_REV, join(withoutCost(EXPECTED_PATH_FLEX_7M)))
      .add(TC_STANDARD_REV_ONE, join(withoutCost(EXPECTED_PATH_FLEX_7M, EXPECTED_PATH_WALK_7M)))
      .add(multiCriteria(), join(EXPECTED_PATH_FLEX_7M, EXPECTED_PATH_WALK_7M))
      .build();
  }

  @ParameterizedTest
  @MethodSource("withFlexAsBestOptionTestCases")
  void withFlexAsBestOptionTest(RaptorModuleTestCase testCase) {
    // with Flex egress as the best destination arrival-time
    requestBuilder.searchParams().addEgressPaths(flex(STOP_C, D7m, 1, COST_10m), walk(STOP_C, D7m));

    var request = testCase.withConfig(requestBuilder);
    var response = raptorService.route(request, data);
    assertEquals(testCase.expected(), pathsToString(response));
  }

  static List<RaptorModuleTestCase> withWalkingAsBestOptionTestCase() {
    return RaptorModuleTestCase
      .of()
      .add(minDuration(), withoutCost(EXPECTED_PATH_WALK_5M))
      .add(standard(), withoutCost(EXPECTED_PATH_WALK_5M))
      .add(multiCriteria(), join(EXPECTED_PATH_WALK_5M))
      .build();
  }

  @ParameterizedTest
  @MethodSource("withWalkingAsBestOptionTestCase")
  void withWalkingAsBestOptionTest(RaptorModuleTestCase testCase) {
    // with walk egress as the best destination arrival-time
    requestBuilder.searchParams().addEgressPaths(flex(STOP_C, D7m, 1, COST_10m), walk(STOP_C, D5m));

    var request = testCase.withConfig(requestBuilder);
    var response = raptorService.route(request, data);
    assertEquals(testCase.expected(), pathsToString(response));
  }
}
