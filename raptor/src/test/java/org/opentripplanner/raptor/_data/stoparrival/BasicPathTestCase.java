package org.opentripplanner.raptor._data.stoparrival;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.stoparrival.TestArrivals.access;
import static org.opentripplanner.raptor._data.stoparrival.TestArrivals.bus;
import static org.opentripplanner.raptor._data.stoparrival.TestArrivals.egress;
import static org.opentripplanner.raptor._data.stoparrival.TestArrivals.transfer;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flexWithOnBoard;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor.api.model.RaptorCostConverter.toRaptorCost;
import static org.opentripplanner.raptor.api.model.RaptorTransferConstraint.REGULAR_TRANSFER;
import static org.opentripplanner.utils.time.DurationUtils.durationToStr;
import static org.opentripplanner.utils.time.TimeUtils.time;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestCostCalculator;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstrainedTransfer;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.path.AccessPathLeg;
import org.opentripplanner.raptor.api.path.EgressPathLeg;
import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.TransferPathLeg;
import org.opentripplanner.raptor.api.path.TransitPathLeg;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.path.Path;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.lifecycle.LifeCycleSubscriptions;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrival;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;

/**
 * This class is used to create a journeys with stop arrivals.
 * <p>
 * It creates different data structures representing the same 'basic' trip to be used in
 * unit-tests:
 * <pre>
 *   ~
 *   Origin 10:00:15
 *   ~ Walk 3m ~ A
 *   ~ BUS L11 10:04 10:35 ~ B
 *   ~ Walk 3m45s ~ C
 *   ~ BUS L21 11:00 11:23 ~ D
 *   ~ BUS L31 11:40 11:52 ~ E
 *   ~ Walk 7m45s
 *   ~ Destination 12:00
 *
 *   Duration: 1h59m45s
 *   Transfers: 2
 *   Generalized-cost: $8154
 * </pre>
 * The Trip has 2 transfers, 1 connected by walking and without. The trip start at 10:00 and ends at
 * 12:00, total 2 hours.
 */
public class BasicPathTestCase implements RaptorTestConstants {

  private static final RaptorConstrainedTransfer EMPTY_CONSTRAINTS = null;

  public static final String BASIC_PATH_AS_DETAILED_STRING =
    "Walk 3m 10:00:15 10:03:15 C₁360 " +
    "~ A 45s ~ " +
    "BUS L11 10:04 10:35 31m C₁1_998 " +
    "~ B 15s ~ " +
    "Walk 3m45s 10:35:15 10:39 C₁450 " +
    "~ C 21m ~ " +
    "BUS L21 11:00 11:23 23m C₁2_640 " +
    "~ D 17m ~ " +
    "BUS L31 11:40 11:52 12m C₁1_776 " +
    "~ E 15s ~ " +
    "Walk 7m45s 11:52:15 12:00 C₁930 " +
    "[10:00:15 12:00 1h59m45s Tₓ2 C₁8_154 C₂7]";

  public static final String BASIC_PATH_AS_STRING =
    "Walk 3m ~ A" +
    " ~ BUS L11 10:04 10:35 ~ B" +
    " ~ Walk 3m45s ~ C" +
    " ~ BUS L21 11:00 11:23 ~ D" +
    " ~ BUS L31 11:40 11:52 ~ E" +
    " ~ Walk 7m45s " +
    "[10:00:15 12:00 1h59m45s Tₓ2 C₁8_154 C₂7]";

  private static final int BOARD_C1_SEC = 60;
  private static final int TRANSFER_C1_SEC = 120;
  private static final double[] TRANSIT_RELUCTANCE = new double[] { 1.0 };
  public static final int TRANSIT_RELUCTANCE_INDEX = 0;
  public static final double WAIT_RELUCTANCE = 0.8;
  private static final int C2 = 7;

  /** Stop cost for stop NA, A, C, E .. H is zero(0), B: 30s, and D: 60s. ?=0, A=1 .. H=8 */
  private static final int[] STOP_C1S = { 0, 0, 3_000, 0, 6_000, 0, 0, 0, 0, 0 };

  // These times should not have eny effect on tests
  private static final int VERY_EARLY = time("00:00");
  private static final int VERY_LATE = time("23:59");

  public static final int RAPTOR_ITERATION_START_TIME = time("09:00");

  // Access (Walk 3m15s ~ A)
  public static final int ACCESS_START = time("10:00:15");
  public static final int ACCESS_END = time("10:03:15");
  public static final int ACCESS_DURATION = ACCESS_END - ACCESS_START;
  public static final RaptorAccessEgress ACCESS_TRANSFER = TestAccessEgress.walk(
    STOP_A,
    ACCESS_DURATION
  );
  public static final int ACCESS_C1 = ACCESS_TRANSFER.c1();
  public static final int ACCESS_C2 = 0;

  // Trip 1 (A ~ BUS L11 10:04 10:35 ~ B)
  public static final int L11_START = time("10:04");
  private static final int L11_END = time("10:35");
  public static final int L11_DURATION = L11_END - L11_START;
  private static final int L11_WAIT_DURATION = L11_START - ACCESS_END + ALIGHT_SLACK;
  public static final int LINE_11_C1 =
    STOP_C1S[STOP_A] +
    STOP_C1S[STOP_B] +
    toRaptorCost(BOARD_C1_SEC + WAIT_RELUCTANCE * L11_WAIT_DURATION + L11_DURATION);
  public static final int LINE_11_C2 = 2;

  // Transfers (B ~ Walk 3m45s ~ C)
  private static final int TX_START = time("10:35:15");
  private static final int TX_END = time("10:39:00");
  public static final int TX_DURATION = TX_END - TX_START;
  public static final RaptorTransfer TX_TRANSFER = TestTransfer.transfer(STOP_C, TX_DURATION);
  public static final int TX_C1 = TX_TRANSFER.c1();
  public static final int TX_C3 = 3;

  // Trip 2 (C ~ BUS L21 11:00 11:23 ~ D)
  public static final int L21_START = time("11:00");
  private static final int L21_END = time("11:23");
  public static final int L21_DURATION = L21_END - L21_START;
  private static final int L21_WAIT_DURATION = L21_START - TX_END + ALIGHT_SLACK;
  public static final int LINE_21_C1 =
    STOP_C1S[STOP_C] +
    STOP_C1S[STOP_D] +
    toRaptorCost(
      BOARD_C1_SEC + TRANSFER_C1_SEC + WAIT_RELUCTANCE * L21_WAIT_DURATION + L21_DURATION
    );
  public static final int LINE_21_C2 = 5;

  // Trip 3 (D ~ BUS L31 11:40 11:52 ~ E)
  public static final int L31_START = time("11:40");
  private static final int L31_END = time("11:52");
  public static final int L31_DURATION = L31_END - L31_START;
  private static final int L31_WAIT_DURATION = L31_START - (L21_END + ALIGHT_SLACK) + ALIGHT_SLACK;
  public static final int LINE_31_C1 =
    STOP_C1S[STOP_D] +
    STOP_C1S[STOP_E] +
    toRaptorCost(
      BOARD_C1_SEC + TRANSFER_C1_SEC + WAIT_RELUCTANCE * L31_WAIT_DURATION + L31_DURATION
    );
  public static final int LINE_31_C2 = 6;

  // Egress (E ~ Walk 7m45s ~ )
  public static final int EGRESS_START = time("11:52:15");
  public static final int EGRESS_END = time("12:00");
  public static final int EGRESS_DURATION = EGRESS_END - EGRESS_START;
  public static final RaptorAccessEgress EGRESS_TRANSFER = TestAccessEgress.walk(
    STOP_E,
    EGRESS_DURATION
  );
  public static final int EGRESS_C1 = EGRESS_TRANSFER.c1();
  public static final int EGRESS_C2 = 7;

  public static final int TRIP_DURATION = EGRESS_END - ACCESS_START;

  private static final RaptorAccessEgress ACCESS = TestAccessEgress.walk(
    STOP_A,
    ACCESS_DURATION,
    ACCESS_C1
  );
  private static final RaptorAccessEgress EGRESS = TestAccessEgress.walk(
    STOP_E,
    EGRESS_DURATION,
    EGRESS_C1
  );
  // this is of course not a real flex egress
  private static final RaptorAccessEgress FLEX = flexWithOnBoard(
    STOP_E,
    EGRESS_DURATION,
    EGRESS_C1
  );

  public static final String LINE_11 = "L11";
  public static final String LINE_21 = "L21";
  public static final String LINE_31 = "L31";

  public static final TestTripSchedule TRIP_1 = TestTripSchedule
    .schedule(pattern(LINE_11, STOP_A, STOP_B))
    .times(L11_START, L11_END)
    .build();

  public static final TestTripSchedule TRIP_2 = TestTripSchedule
    .schedule(pattern(LINE_21, STOP_C, STOP_D))
    .times(L21_START, L21_END)
    .build();

  public static final TestTripSchedule TRIP_3 = TestTripSchedule
    .schedule(pattern(LINE_31, STOP_D, STOP_E))
    // The early arrival and late departure should not have any effect on tests
    .arrivals(VERY_EARLY, L31_END)
    .departures(L31_START, VERY_LATE)
    .build();

  public static final RaptorCostCalculator<TestTripSchedule> C1_CALCULATOR = new TestCostCalculator(
    BOARD_C1_SEC,
    TRANSFER_C1_SEC,
    WAIT_RELUCTANCE,
    STOP_C1S
  );

  public static final int TOTAL_C1 =
    ACCESS_C1 + LINE_11_C1 + TX_C1 + LINE_21_C1 + LINE_31_C1 + EGRESS_C1;

  /** Wait time between trip L11 and L21 including slack */
  public static final int WAIT_TIME_L11_L21 = L21_START - L11_END - TX_DURATION;

  /** Wait time between trip L21 and L31 including slack */
  public static final int WAIT_TIME_L21_L31 = L31_START - L21_END;

  public static WorkerLifeCycle lifeCycle() {
    return new LifeCycleSubscriptions();
  }

  public static DestinationArrival<TestTripSchedule> basicTripByForwardSearch() {
    ArrivalView<TestTripSchedule> prevArrival, egress;
    prevArrival = access(STOP_A, ACCESS_START, ACCESS_END, ACCESS_C1, ACCESS_C2);
    prevArrival = bus(1, STOP_B, L11_END, LINE_11_C1, LINE_11_C2, TRIP_1, prevArrival);
    prevArrival = transfer(1, STOP_C, TX_START, TX_END, TX_C1, prevArrival);
    prevArrival = bus(2, STOP_D, L21_END, LINE_21_C1, LINE_21_C2, TRIP_2, prevArrival);
    prevArrival = bus(3, STOP_E, L31_END, LINE_31_C1, LINE_31_C2, TRIP_3, prevArrival);
    egress = egress(EGRESS_START, EGRESS_END, EGRESS_C1, EGRESS_C2, prevArrival);
    return new DestinationArrival<>(
      egress.egressPath().egress(),
      egress.previous(),
      egress.arrivalTime(),
      egress.egressPath().egress().c1(),
      egress.c2()
    );
  }

  /**
   * This is the same itinerary as {@link #basicTripByForwardSearch()}, as found by a reverse
   * search:
   */
  public static DestinationArrival<TestTripSchedule> basicTripByReverseSearch() {
    ArrivalView<TestTripSchedule> nextArrival, egress;
    nextArrival = access(STOP_E, EGRESS_END, EGRESS_START, EGRESS_C1, EGRESS_C2);
    // Board slack is subtracted from the arrival time to get the latest possible
    nextArrival = bus(1, STOP_D, L31_START, LINE_31_C1, LINE_31_C2, TRIP_3, nextArrival);
    nextArrival = bus(2, STOP_C, L21_START, LINE_21_C1, LINE_21_C2, TRIP_2, nextArrival);
    nextArrival = transfer(2, STOP_B, TX_END, TX_START, TX_C1, nextArrival);
    nextArrival = bus(3, STOP_A, L11_START, LINE_11_C1, LINE_11_C2, TRIP_1, nextArrival);
    egress = egress(ACCESS_END, ACCESS_START, ACCESS_C1, ACCESS_C2, nextArrival);
    return new DestinationArrival<>(
      egress.egressPath().egress(),
      egress.previous(),
      egress.arrivalTime(),
      egress.egressPath().egress().c1(),
      egress.c2()
    );
  }

  /**
   * Both {@link #basicTripByForwardSearch()} and {@link #basicTripByReverseSearch()} should return
   * the same trip, here returned as a path.
   */
  public static RaptorPath<TestTripSchedule> basicTripAsPath() {
    PathLeg<TestTripSchedule> leg6 = new EgressPathLeg<>(
      EGRESS,
      EGRESS_START,
      EGRESS_END,
      EGRESS_C1
    );
    TransitPathLeg<TestTripSchedule> leg5 = new TransitPathLeg<>(
      TRIP_3,
      L31_START,
      L31_END,
      TRIP_3.findDepartureStopPosition(L31_START, STOP_D),
      TRIP_3.findArrivalStopPosition(L31_END, STOP_E),
      EMPTY_CONSTRAINTS,
      LINE_31_C1,
      leg6
    );
    TransitPathLeg<TestTripSchedule> leg4 = new TransitPathLeg<>(
      TRIP_2,
      L21_START,
      L21_END,
      TRIP_2.findDepartureStopPosition(L21_START, STOP_C),
      TRIP_2.findArrivalStopPosition(L21_END, STOP_D),
      EMPTY_CONSTRAINTS,
      LINE_21_C1,
      leg5
    );
    var transfer = TestTransfer.transfer(STOP_C, TX_END - TX_START);
    PathLeg<TestTripSchedule> leg3 = new TransferPathLeg<>(
      STOP_B,
      TX_START,
      TX_END,
      transfer.c1(),
      transfer,
      leg4.asTransitLeg()
    );
    var leg2 = new TransitPathLeg<>(
      TRIP_1,
      L11_START,
      L11_END,
      TRIP_1.findDepartureStopPosition(L11_START, STOP_A),
      TRIP_1.findArrivalStopPosition(L11_END, STOP_B),
      EMPTY_CONSTRAINTS,
      LINE_11_C1,
      leg3
    );
    AccessPathLeg<TestTripSchedule> leg1 = new AccessPathLeg<>(
      ACCESS,
      ACCESS_START,
      ACCESS_END,
      ACCESS_C1,
      leg2.asTransitLeg()
    );
    return new Path<>(RAPTOR_ITERATION_START_TIME, leg1, TOTAL_C1, 7);
  }

  public static RaptorPath<TestTripSchedule> flexTripAsPath() {
    PathLeg<TestTripSchedule> leg6 = new EgressPathLeg<>(FLEX, EGRESS_START, EGRESS_END, EGRESS_C1);
    var transfer = TestTransfer.transfer(STOP_E, TX_END - TX_START);
    PathLeg<TestTripSchedule> leg3 = new TransferPathLeg<>(
      STOP_B,
      TX_START,
      TX_END,
      transfer.c1(),
      transfer,
      leg6
    );
    var leg2 = new TransitPathLeg<>(
      TRIP_1,
      L11_START,
      L11_END,
      TRIP_1.findDepartureStopPosition(L11_START, STOP_A),
      TRIP_1.findArrivalStopPosition(L11_END, STOP_B),
      EMPTY_CONSTRAINTS,
      LINE_11_C1,
      leg3
    );
    AccessPathLeg<TestTripSchedule> leg1 = new AccessPathLeg<>(
      ACCESS,
      ACCESS_START,
      ACCESS_END,
      ACCESS_C1,
      leg2.asTransitLeg()
    );
    return new Path<>(RAPTOR_ITERATION_START_TIME, leg1, TOTAL_C1, C2);
  }

  public static List<Integer> basicTripStops() {
    return Arrays.asList(STOP_A, STOP_B, STOP_C, STOP_D, STOP_E);
  }

  @Test
  public void testSetup() {
    // Assert test data is configured correct
    assertEquals(ACCESS_END + BOARD_SLACK, L11_START);
    assertEquals(BOARD_SLACK + ALIGHT_SLACK, L11_WAIT_DURATION);
    assertEquals(L31_END + ALIGHT_SLACK, EGRESS_START);
    assertEquals(
      durationToStr(TRIP_DURATION),
      durationToStr(
        ACCESS_DURATION +
        L11_DURATION +
        L11_WAIT_DURATION +
        TX_DURATION +
        L21_DURATION +
        L21_WAIT_DURATION +
        L31_DURATION +
        L31_WAIT_DURATION +
        EGRESS_DURATION
      ),
      "Access: " +
      durationToStr(ACCESS_DURATION) +
      ", Line 11: " +
      durationToStr(L11_DURATION) +
      " (wait " +
      durationToStr(L11_WAIT_DURATION) +
      ")" +
      ", Tx: " +
      durationToStr(TX_DURATION) +
      ", Line 21: " +
      durationToStr(L21_DURATION) +
      " (wait " +
      durationToStr(L21_WAIT_DURATION) +
      ")" +
      ", Line 31: " +
      durationToStr(L31_DURATION) +
      " (wait " +
      durationToStr(L31_WAIT_DURATION) +
      ")" +
      ", Egress: " +
      durationToStr(EGRESS_DURATION)
    );

    // The calculator is not under test here, so we assert everything is as expected
    assertEquals(
      LINE_11_C1,
      transitArrivalCost(ACCESS_END, TRIP_1, STOP_A, L11_START, STOP_B, L11_END)
    );
    assertEquals(
      LINE_21_C1,
      transitArrivalCost(TX_END, TRIP_2, STOP_C, L21_START, STOP_D, L21_END)
    );
    assertEquals(
      LINE_31_C1,
      transitArrivalCost(L21_END + ALIGHT_SLACK, TRIP_3, STOP_D, L31_START, STOP_E, L31_END)
    );

    assertEquals(
      BASIC_PATH_AS_STRING,
      basicTripAsPath().toString(RaptorTestConstants::stopIndexToName)
    );

    assertEquals(
      BASIC_PATH_AS_DETAILED_STRING,
      basicTripAsPath().toStringDetailed(RaptorTestConstants::stopIndexToName)
    );
  }

  private static int transitArrivalCost(
    int prevArrivalTime,
    TestTripSchedule trip,
    int boardStop,
    int boardTime,
    int alightStop,
    int alightTime
  ) {
    boolean firstTransit = TRIP_1 == trip;
    int boardCost = C1_CALCULATOR.boardingCost(
      firstTransit,
      prevArrivalTime,
      boardStop,
      boardTime,
      trip,
      REGULAR_TRANSFER
    );

    return C1_CALCULATOR.transitArrivalCost(
      boardCost,
      ALIGHT_SLACK,
      alightTime - boardTime,
      trip,
      alightStop
    );
  }
}
