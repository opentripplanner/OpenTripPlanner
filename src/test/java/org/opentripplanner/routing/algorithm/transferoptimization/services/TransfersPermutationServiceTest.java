package org.opentripplanner.routing.algorithm.transferoptimization.services;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.T2TTransferDummy.dummyT2TTransferService;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.T2TTransferDummy.tx;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.T2TTransferDummy.tx;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.COST_CALCULATOR;
import static org.opentripplanner.transit.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.util.time.TimeUtils.time;

import java.util.List;
import org.junit.Test;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.api.PathBuilder;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;

/**
 * The {@code TransfersPermutationService} is tested using a fixed set of trips/routes, but
 * we construct paths and transfers for each case. All testes uses this set of trips:
 *
 * <pre>
 * DEPARTURE TIMES
 * Stop        A      B      C      D      E      F      G
 * Trip 1    10:02  10:10         10:20                10:35
 * Trip 2           10:12  10:15  10:22         10:35  10:40
 * Trip 3                                10:24  10:37  10:49
 *
 * Note! The ARRIVAL TIMES are 1 minute BEFORE the departure times for all stops.
 *</pre>
 *
 * <p>This set allow us to construct all wanted testcases using different paths with different
 * set of transfers.
 *
 * <p>The test start with the simple cases and build up to more and more complicated cases.
 */
public class TransfersPermutationServiceTest implements RaptorTestConstants {
  /** The exact start time to walk to stop A to catch Trip_1 with 40s board slack */
  private final int START_TIME_T1 = time("10:00:20");
  private final int TRANSFER_SLACK = D1m;
  private final int BOARD_SLACK = D40s;
  private final int ALIGHT_SLACK = D20s;

  private final RaptorSlackProvider SLACK_PROVIDER = RaptorSlackProvider
      .defaultSlackProvider(TRANSFER_SLACK, BOARD_SLACK, ALIGHT_SLACK);

  private final TestTripSchedule TRIP_1  = TestTripSchedule.schedule()
      .arrDepOffset(60)
      .pattern(pattern("T1", STOP_A, STOP_B, STOP_D, STOP_G))
      .departures("10:02 10:10 10:20 10:35").build();

  private final TestTripSchedule TRIP_2  = TestTripSchedule.schedule()
      .arrDepOffset(60)
      .pattern(pattern("T2", STOP_B, STOP_C, STOP_D, STOP_F, STOP_G))
      .departures("10:12 10:15 10:22 10:35 10:40").build();

  private final TestTripSchedule TRIP_3  = TestTripSchedule.schedule()
          .arrDepOffset(0)
          .pattern(pattern("T3", STOP_E, STOP_F, STOP_G))
          .departures("10:24 10:37 10:49").build();

  private final PathBuilder pathBuilder = new PathBuilder(ALIGHT_SLACK, COST_CALCULATOR);


  /**
   * A Path without any transfers should be returned without any change.
   */
  @Test
  public void testTripWithoutTransfers() {
    // Given
    var original = pathBuilder
        .access(START_TIME_T1, D1m, STOP_A)
        .bus(TRIP_1, STOP_B)
        .egress(D1m);
    var transfers = dummyT2TTransferService();
    var subject = new TransfersPermutationService<>(transfers, COST_CALCULATOR, SLACK_PROVIDER);

    // When
    var result = subject.findAllTransitPathPermutations(original);

    assertEquals(result.toString(), 1, result.size());
    assertEquals(original.toString(), result.get(0).toString());
  }

  /**
   * This test emulate the normal case were there is only one option to transfer between two
   * trips and the TransfersPermutationService should find the exact same option. The path should
   * exactly match the original path after the path is reconstructed.
   */
  @Test
  public void testTripWithOneTransfer() {
    // Given
    var original = pathBuilder
        .access(START_TIME_T1, D1m, STOP_A)
        .bus(TRIP_1, STOP_B)
        .walk(D1m, STOP_C)
        .bus(TRIP_2, STOP_D)
        .egress(D1m);
    var transfers = dummyT2TTransferService(
        // Original transfer
        tx(TRIP_1, STOP_B, D1m, STOP_C, TRIP_2)
    );

    var subject = new TransfersPermutationService<>(transfers, COST_CALCULATOR, SLACK_PROVIDER);

    // When
    var result = subject.findAllTransitPathPermutations(original);

    assertEquals(result.toString(), 1, result.size());
    assertEquals(original.toString(), result.get(0).toString());
  }

  @Test
  public void testTripWithOneTransferAtTheSameStop() {
    // Given
    var original = pathBuilder
        .access(START_TIME_T1, D1m, STOP_A)
        .bus(TRIP_1, STOP_D)
        .bus(TRIP_2, STOP_F)
        .egress(D1m);
    var transfers = dummyT2TTransferService(
        // Original transfer
        tx(TRIP_1, STOP_D, TRIP_2),
        // Transfer exist after egress stop
        tx(TRIP_1, STOP_G, TRIP_2)
    );

    var subject = new TransfersPermutationService<>(transfers, COST_CALCULATOR, SLACK_PROVIDER);

    // When
    var result = subject.findAllTransitPathPermutations(original);

    assertEquals(result.toString(), 1, result.size());
    assertEquals(original.toString(), result.get(0).toString());
  }

  @Test
  public void testPathWithThreeTripsAndMultiplePlacesToTransfer() {
    // Given
    var original = pathBuilder
            .access(START_TIME_T1, 0, STOP_A)
            .bus(TRIP_1, STOP_B)
            .bus(TRIP_2, STOP_D)
            .walk(D30s, STOP_E)
            .bus(TRIP_3, STOP_G)
            .egress(0);
    var transfers = dummyT2TTransferService(
            tx(TRIP_1, STOP_B, TRIP_2),
            tx(TRIP_1, STOP_B, D30s, STOP_C, TRIP_2),
            tx(TRIP_1, STOP_D, TRIP_2),
            tx(TRIP_1, STOP_G, TRIP_2),
            tx(TRIP_1, STOP_G, TRIP_3),
            tx(TRIP_2, STOP_D, D30s, STOP_E, TRIP_3),
            tx(TRIP_2, STOP_F, TRIP_3),
            tx(TRIP_2, STOP_G, TRIP_3)
    );

    var subject = new TransfersPermutationService<>(transfers, COST_CALCULATOR, SLACK_PROVIDER);

    // When
    var result = subject.findAllTransitPathPermutations(original);

    var expected = List.of(
            "1 ~ BUS T1 10:02 10:09 ~ 2 ~ BUS T2 10:12 10:21 ~ 4 ~ Walk 30s ~ 5 ~ BUS T3 10:24 10:49 ~ 7 [10:00:20 10:49:20 49m $3420]",
            "1 ~ BUS T1 10:02 10:09 ~ 2 ~ BUS T2 10:12 10:34 ~ 6 ~ BUS T3 10:37 10:49 ~ 7 [10:00:20 10:49:20 49m $3324]",
            "1 ~ BUS T1 10:02 10:09 ~ 2 ~ Walk 30s ~ 3 ~ BUS T2 10:15 10:21 ~ 4 ~ Walk 30s ~ 5 ~ BUS T3 10:24 10:49 ~ 7 [10:00:20 10:49:20 49m $3390]",
            "1 ~ BUS T1 10:02 10:09 ~ 2 ~ Walk 30s ~ 3 ~ BUS T2 10:15 10:34 ~ 6 ~ BUS T3 10:37 10:49 ~ 7 [10:00:20 10:49:20 49m $3294]",
            "1 ~ BUS T1 10:02 10:19 ~ 4 ~ BUS T2 10:22 10:34 ~ 6 ~ BUS T3 10:37 10:49 ~ 7 [10:00:20 10:49:20 49m $3384]"
    );

    for(int i=0; i<expected.size() && i< result.size(); ++i) {
      assertEquals("#" + i, expected.get(i), result.get(i).toString());
    }
    assertEquals(result.toString(), expected.size(), result.size());
  }
}