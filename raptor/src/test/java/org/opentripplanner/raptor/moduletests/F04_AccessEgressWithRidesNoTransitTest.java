package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flexAndWalk;
import static org.opentripplanner.raptor._data.transit.TestTransfer.transfer;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_STANDARD_ONE;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.api.PathUtils;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should be able to connect to Flex services with and without a walking transfer in
 * between. There is no regular scheduled transit involved in this test, only flex.
 * <p>
 * There is one test for each of the following cases:
 * <pre>
 *   ACCESS                   TRANSFER     EGRESS
 * - Flex                 ~ B ~ Walk ~ C ~ Flex
 * - Flex(Open 0:12-0:16) ~ B ~ Walk ~ C ~ Flex
 * - Flex                 ~ B ~ Walk ~ C ~ Flex(Open 0:24-0:28)
 * - Flex+Walk                       ~ C ~ Flex
 * - Flex                            ~ C ~ Walk+Flex
 * </pre>
 * {@code B} and {@code C} are stops. The 2 last cases does not have any walking transfer.
 */
public class F04_AccessEgressWithRidesNoTransitTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();

  @BeforeEach
  public void setup() {
    data
      // The transit must exist for data to be valid, but it is not routed on or used by the test
      .withTransit("Any", "12:00 13:00", STOP_A, STOP_D)
      .withTransfer(STOP_B, transfer(STOP_C, D5m));
    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_10)
      .latestArrivalTime(T00_30)
      .searchWindowInSeconds(D10m);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  /* Flex ~ B ~ Walk ~ C ~ Flex */

  static List<RaptorModuleTestCase> flexTransferFlexTestCases() {
    var path = "Flex 2m 1x ~ B ~ Walk 5m ~ C ~ Flex 2m 1x [0:10 0:20 10m Tₓ1 C₁1_140]";
    var stdPathRev = "Flex 2m 1x ~ B ~ Walk 5m ~ C ~ Flex 2m 1x [0:20 0:30 10m Tₓ1]";
    return RaptorModuleTestCase.of()
      .withRequest(requestBuilder ->
        requestBuilder
          .searchParams()
          .addAccessPaths(flex(STOP_B, D2m))
          .addEgressPaths(flex(STOP_C, D2m))
      )
      .addMinDuration("10m", TX_1, T00_10, T00_30)
      .add(standard().forwardOnly(), PathUtils.withoutCost(path))
      .add(standard().reverseOnly(), stdPathRev)
      .add(multiCriteria(), path)
      .build();
  }

  @ParameterizedTest
  @MethodSource("flexTransferFlexTestCases")
  void flexTransferFlexTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  /* Flex(Open 0:12-0:16) ~ B ~ Walk ~ C ~ Flex */

  static List<RaptorModuleTestCase> flexOpeningHoursTransferFlexTestCases() {
    var path =
      "Flex 2m 1x Open(0:12 0:16) ~ B ~ Walk 5m ~ C ~ Flex 2m 1x [0:12 0:22 10m Tₓ1 C₁1_140]";
    var stdPathRev =
      "Flex 2m 1x Open(0:12 0:16) ~ B ~ Walk 5m ~ C ~ Flex 2m 1x [0:16 0:26 10m Tₓ1]";
    return RaptorModuleTestCase.of()
      .withRequest(requestBuilder ->
        requestBuilder
          .searchParams()
          .addAccessPaths(flex(STOP_B, D2m).openingHours("0:12", "0:16"))
          .addEgressPaths(flex(STOP_C, D2m))
      )
      .addMinDuration("10m", TX_1, T00_10, T00_30)
      .add(standard().forwardOnly(), PathUtils.withoutCost(path))
      .add(standard().reverseOnly(), stdPathRev)
      .add(multiCriteria(), path)
      .build();
  }

  @ParameterizedTest
  @MethodSource("flexOpeningHoursTransferFlexTestCases")
  void flexOpeningHoursTransferFlexTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  /* Flex ~ B ~ Walk ~ C ~ Flex(Open 0:24-0:28) */

  static List<RaptorModuleTestCase> flexTransferFlexOpeningHoursTestCases() {
    var path = "Flex 2m 1x ~ B ~ Walk 5m ~ C ~ Flex 2m 1x Open(0:22 0:26) ";
    return RaptorModuleTestCase.of()
      .withRequest(requestBuilder ->
        requestBuilder
          .searchParams()
          .addAccessPaths(flex(STOP_B, D2m))
          .addEgressPaths(flex(STOP_C, D2m).openingHours("0:22", "0:26"))
      )
      .addMinDuration("10m", TX_1, T00_10, T00_30)
      .add(TC_STANDARD, path + "[0:14 0:24 10m Tₓ1]")
      .add(TC_STANDARD_ONE, path + "[0:10 0:24 14m Tₓ1]")
      .add(standard().reverseOnly(), path + "[0:18 0:28 10m Tₓ1]")
      .add(multiCriteria(), path + "[0:14 0:24 10m Tₓ1 C₁1_140]")
      .build();
  }

  @ParameterizedTest
  @MethodSource("flexTransferFlexOpeningHoursTestCases")
  void flexTransferFlexOpeningHoursTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  /* Flex+Walk ~ C ~ Flex  (No transfer) */

  static List<RaptorModuleTestCase> flexAndWalkToFlexTestCases() {
    var path = "Flex+Walk 7m 1x ~ C ~ Flex 2m 1x [0:10 0:20 10m Tₓ1 C₁1_140]";
    var stdPathRev = "Flex+Walk 7m 1x ~ C ~ Flex 2m 1x [0:20 0:30 10m Tₓ1]";
    return RaptorModuleTestCase.of()
      .withRequest(requestBuilder ->
        requestBuilder
          .searchParams()
          .addAccessPaths(flexAndWalk(STOP_C, D7m))
          .addEgressPaths(flex(STOP_C, D2m))
      )
      .addMinDuration("10m", TX_1, T00_10, T00_30)
      .add(standard().forwardOnly(), PathUtils.withoutCost(path))
      .add(standard().reverseOnly(), stdPathRev)
      .add(multiCriteria(), path)
      .build();
  }

  @ParameterizedTest
  @MethodSource("flexAndWalkToFlexTestCases")
  void flexAndWalkToFlexTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }

  /* Flex ~ C ~ Walk+Flex  (No transfer) */

  static List<RaptorModuleTestCase> flexToFlexAndWalkTestCases() {
    var path = "Flex 2m 1x ~ C ~ Flex+Walk 7m 1x [0:10 0:20 10m Tₓ1 C₁1_140]";
    var stdPathRev = "Flex 2m 1x ~ C ~ Flex+Walk 7m 1x [0:20 0:30 10m Tₓ1]";
    return RaptorModuleTestCase.of()
      .withRequest(requestBuilder ->
        requestBuilder
          .searchParams()
          .addAccessPaths(flex(STOP_C, D2m))
          .addEgressPaths(flexAndWalk(STOP_C, D7m))
      )
      .addMinDuration("10m", TX_1, T00_10, T00_30)
      .add(standard().forwardOnly(), PathUtils.withoutCost(path))
      .add(standard().reverseOnly(), stdPathRev)
      .add(multiCriteria(), path)
      .build();
  }

  @ParameterizedTest
  @MethodSource("flexToFlexAndWalkTestCases")
  void flexToFlexAndWalkTest(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
