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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;

/**
 * FEATURE UNDER TEST
 * <p>
 * This test focus on selecting the correct start on the journey, where a trip L1 competes with a
 * flex access to get to stop C. The Flex access arrives at C one minute after Trip L1 + walking,
 * but the flex arrives on-board. This should give the flex an advantage, the ability to transfer to
 * a nearby stop. In this test we manipulate the EGRESS for make the two paths optional. If we can
 * get both paths (start with flex access or trip L1) as optimal results by changing the egress,
 * then we have proven that both these results are kept in stop arrival at stop C.
 * <p>
 * Further this test also makes sure the path is constructed correctly when we have a mix of access,
 * transfer and transit at the same stop, in the same Raptor round. Two walking legs are not allowed
 * after each other.
 * <p>
 * <img src="images/B12.svg" width="548" height="206"/>
 * <p>
 * We use the same data and changes the egress walk leg to cover all cases. The egress walk leg
 * becomes optimal is it is 3 minutes, while the flex is optimal when we set the egress to 10
 * minutes. Trip L2 is faster than trip L1, but must be using the FLEX egress - since it arrive at
 * the stop C by walking.
 * <p>
 * Note! The 'earliest-departure-time' is set to 00:02, and the board and alight slacks are zero.
 */
public class B12_MultipleOptimalAccessOptions implements RaptorTestConstants {

  private static final String EXPECTED_FLEX =
    "Flex 11m 1x ~ C ~ Walk 2m ~ D ~ BUS L3 0:16 0:22 ~ F [0:02 0:22 20m 1tx";
  private static final String EXPECTED_FLEX_STD = EXPECTED_FLEX + "]";
  private static final String EXPECTED_FLEX_MC = EXPECTED_FLEX + " $2580]";

  private static final String EXPECTED_WALK =
    "A ~ BUS L1 0:02 0:10 ~ B ~ Walk 2m ~ C ~ BUS L2 0:15 0:20 ~ E ~ Walk 1m [0:02 0:21 19m 1tx";
  private static final String EXPECTED_WALK_STD = EXPECTED_WALK + "]";
  private static final String EXPECTED_WALK_MC = EXPECTED_WALK + " $2460]";

  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );
  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();

  @BeforeEach
  public void setup() {
    data.withRoutes(
      route("L1", STOP_A, STOP_B).withTimetable(schedule("0:02 0:10")),
      route("L2", STOP_C, STOP_E).withTimetable(schedule("0:15 0:20")),
      route("L3", STOP_D, STOP_F).withTimetable(schedule("0:16 0:22"))
    );
    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_02)
      .latestArrivalTime(T00_30)
      .searchOneIterationOnly();

    // We will test board- and alight-slack in a separate test
    requestBuilder.slackProvider(defaultSlackProvider(D1m, D0s, D0s));

    requestBuilder
      .searchParams()
      .addAccessPaths(TestAccessEgress.walk(STOP_A, D0s), flex(STOP_C, D11m));

    data
      .withTransfer(STOP_B, TestTransfer.transfer(STOP_C, D2m))
      .withTransfer(STOP_C, TestTransfer.transfer(STOP_D, D2m));

    // Set ModuleTestDebugLogging.DEBUG=true to enable debugging output
    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void standardExpectFlex() {
    requestBuilder.profile(STANDARD);
    withFlexAccessAsBestOption();

    assertEquals(EXPECTED_FLEX_STD, runSearch());
  }

  @Test
  public void standardExpectTripL1() {
    requestBuilder.profile(STANDARD);
    withTripL1AsBestStartOption();

    assertEquals(EXPECTED_WALK_STD, runSearch());
  }

  @Test
  public void standardReverseExpectFlex() {
    requestBuilder.profile(STANDARD).searchDirection(REVERSE);
    requestBuilder.searchParams().addEgressPaths(TestAccessEgress.walk(STOP_F, D0s));

    assertEquals(EXPECTED_FLEX_STD, runSearch());
  }

  @Test
  @Disabled(
    "This test so not work due to an error in the onBoard mc-pareto function, " +
    "witch do not account for departure time. The flex access dominate the " +
    "L1+walk even the L1 leaves one minute after the access."
  )
  public void multiCriteria() {
    requestBuilder.profile(MULTI_CRITERIA);
    requestBuilder.searchParams().searchWindowInSeconds(D5m);
    requestBuilder.searchParams().earliestDepartureTime(T00_00);
    requestBuilder
      .searchParams()
      .addEgressPaths(TestAccessEgress.walk(STOP_E, D3m), TestAccessEgress.walk(STOP_F, D0s));

    String actual = runSearch();

    assertEquals(join(EXPECTED_FLEX_MC, EXPECTED_WALK_MC), actual);
  }

  /**
   * This set the egress-paths, so the flex access become optimal. The Egress from Stop E to Stop F
   * is set to 3 minutes.
   */
  private void withFlexAccessAsBestOption() {
    requestBuilder
      .searchParams()
      .addEgressPaths(TestAccessEgress.walk(STOP_F, D0s), TestAccessEgress.walk(STOP_E, D3m));
  }

  /**
   * This set the egress-paths, so trip L1 is the best way to begin the journey. The Egress from
   * Stop E to Stop F is set to 1 minute.
   */
  private void withTripL1AsBestStartOption() {
    requestBuilder
      .searchParams()
      .addEgressPaths(TestAccessEgress.walk(STOP_F, D0s), TestAccessEgress.walk(STOP_E, D1m));
  }

  private String runSearch() {
    return pathsToString(raptorService.route(requestBuilder.build(), data));
  }
}
