package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.join;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.raptor.api.request.RaptorProfile.STANDARD;
import static org.opentripplanner.raptor.spi.RaptorSlackProvider.defaultSlackProvider;
import static org.opentripplanner.raptor.spi.SearchDirection.REVERSE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;

/**
 * FEATURE UNDER TEST
 * <p>
 * This test focuses on on-foot and flex egresses. You are not allowed to have two walk legs after
 * each other, so depending on how you arrived at the stop where the egress starts, the walking
 * option might not be possible.
 * <p>
 * Test case:
 * <img src="images/B13.svg" width="432" height="212" />
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
public class B13_MultipleOptimalEgressOptions implements RaptorTestConstants {

  private static final String EXPECTED_PATH_FLEX =
    "A ~ BUS R2 0:05 0:16 ~ B ~ Walk 2m ~ C ~ Flex 7m 1x [0:05 0:26 21m 1tx";
  private static final String EXPECTED_STD_FLEX = EXPECTED_PATH_FLEX + "]";
  private static final String EXPECTED_MC_FLEX = EXPECTED_PATH_FLEX + " $2160]";

  private static final String EXPECTED_PATH_WALK_5M =
    "A ~ BUS R1 0:04 0:20 ~ C ~ Walk 5m [0:04 0:25 21m 0tx";
  private static final String EXPECTED_STD_WALK_5M = EXPECTED_PATH_WALK_5M + "]";
  private static final String EXPECTED_MC_WALK_5M = EXPECTED_PATH_WALK_5M + " $2160]";

  private static final String EXPECTED_MC_WALK_6M =
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
    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .searchWindowInSeconds(D20m)
      .latestArrivalTime(T00_30);

    // We will test board- and alight-slack in a separate test
    requestBuilder.slackProvider(defaultSlackProvider(D1m, D0s, D0s));

    requestBuilder.searchParams().addAccessPaths(TestAccessEgress.walk(STOP_A, D0s));

    data.withTransfer(STOP_B, TestTransfer.transfer(STOP_C, D2m));

    // Set ModuleTestDebugLogging.DEBUG=true to enable debugging output
    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void standardFlex() {
    withFlexEgressAsBestDestinationArrivalTime();
    requestBuilder.profile(STANDARD);
    assertEquals(EXPECTED_STD_FLEX, runSearch());
  }

  @Test
  public void standardWalking() {
    withWalkingAsBestDestinationArrivalTime();
    requestBuilder.profile(STANDARD);
    assertEquals(EXPECTED_STD_WALK_5M, runSearch());
  }

  @Test
  public void standardReverseFlex() {
    withFlexEgressAsBestDestinationArrivalTime();
    requestBuilder.profile(STANDARD).searchDirection(REVERSE);
    assertEquals(EXPECTED_STD_FLEX, runSearch());
  }

  @Test
  public void standardReverseWalking() {
    withWalkingAsBestDestinationArrivalTime();
    requestBuilder.profile(STANDARD).searchDirection(REVERSE);
    assertEquals(EXPECTED_STD_WALK_5M, runSearch());
  }

  @Test
  public void multiCriteriaFlex() {
    withFlexEgressAsBestDestinationArrivalTime();
    requestBuilder.profile(MULTI_CRITERIA);
    assertEquals(join(EXPECTED_MC_FLEX, EXPECTED_MC_WALK_6M), runSearch());
  }

  @Test
  public void multiCriteriaWalking() {
    withWalkingAsBestDestinationArrivalTime();
    requestBuilder.profile(MULTI_CRITERIA);
    assertEquals(EXPECTED_MC_WALK_5M, runSearch());
  }

  private void withFlexEgressAsBestDestinationArrivalTime() {
    requestBuilder
      .searchParams()
      .addEgressPaths(flex(STOP_C, D7m, 1, COST_10m), TestAccessEgress.walk(STOP_C, D7m));
  }

  private void withWalkingAsBestDestinationArrivalTime() {
    requestBuilder
      .searchParams()
      .addEgressPaths(flex(STOP_C, D7m, 1, COST_10m), TestAccessEgress.walk(STOP_C, D5m));
  }

  private String runSearch() {
    return pathsToString(raptorService.route(requestBuilder.build(), data));
  }
}
