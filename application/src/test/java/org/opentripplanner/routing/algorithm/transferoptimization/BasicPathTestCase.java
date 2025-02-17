package org.opentripplanner.routing.algorithm.transferoptimization;

import static org.opentripplanner.raptor.api.model.RaptorCostConverter.toRaptorCost;
import static org.opentripplanner.utils.time.TimeUtils.time;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstrainedTransfer;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.path.AccessPathLeg;
import org.opentripplanner.raptor.api.path.EgressPathLeg;
import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.TransferPathLeg;
import org.opentripplanner.raptor.api.path.TransitPathLeg;
import org.opentripplanner.raptor.path.Path;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptorlegacy._data.RaptorTestConstants;
import org.opentripplanner.raptorlegacy._data.transit.TestAccessEgress;
import org.opentripplanner.raptorlegacy._data.transit.TestTransfers;
import org.opentripplanner.raptorlegacy._data.transit.TestTripPattern;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultCostCalculator;

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
 *
 * @deprecated This was earlier part of Raptor and should not be used outside the Raptor
 *             module. Use the OTP model entities instead.
 */
@Deprecated
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

  // Some times which should not have eny effect on tests
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

  // Trip 1 (A ~ BUS L11 10:04 10:35 ~ B)
  public static final int L11_START = time("10:04");
  private static final int L11_END = time("10:35");
  public static final int L11_DURATION = L11_END - L11_START;
  private static final int L11_WAIT_DURATION = L11_START - ACCESS_END + ALIGHT_SLACK;
  public static final int LINE_11_C1 =
    STOP_C1S[STOP_A] +
    STOP_C1S[STOP_B] +
    toRaptorCost(BOARD_C1_SEC + WAIT_RELUCTANCE * L11_WAIT_DURATION + L11_DURATION);

  // Transfers (B ~ Walk 3m45s ~ C)
  private static final int TX_START = time("10:35:15");
  private static final int TX_END = time("10:39:00");
  public static final int TX_DURATION = TX_END - TX_START;
  public static final RaptorTransfer TX_TRANSFER = TestTransfers.transfer(STOP_C, TX_DURATION);
  public static final int TX_C1 = TX_TRANSFER.c1();

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

  // Egress (E ~ Walk 7m45s ~ )
  public static final int EGRESS_START = time("11:52:15");
  public static final int EGRESS_END = time("12:00");
  public static final int EGRESS_DURATION = EGRESS_END - EGRESS_START;
  public static final RaptorAccessEgress EGRESS_TRANSFER = TestAccessEgress.walk(
    STOP_E,
    EGRESS_DURATION
  );
  public static final int EGRESS_C1 = EGRESS_TRANSFER.c1();

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
  private static final RaptorAccessEgress FLEX = TestAccessEgress.flexWithOnBoard(
    STOP_E,
    EGRESS_DURATION,
    EGRESS_C1
  );

  public static final String LINE_11 = "L11";
  public static final String LINE_21 = "L21";
  public static final String LINE_31 = "L31";

  public static final TestTripSchedule TRIP_1 = TestTripSchedule
    .schedule(TestTripPattern.pattern(LINE_11, STOP_A, STOP_B))
    .times(L11_START, L11_END)
    .transitReluctanceIndex(TRANSIT_RELUCTANCE_INDEX)
    .build();

  public static final TestTripSchedule TRIP_2 = TestTripSchedule
    .schedule(TestTripPattern.pattern(LINE_21, STOP_C, STOP_D))
    .times(L21_START, L21_END)
    .transitReluctanceIndex(TRANSIT_RELUCTANCE_INDEX)
    .build();

  public static final TestTripSchedule TRIP_3 = TestTripSchedule
    .schedule(TestTripPattern.pattern(LINE_31, STOP_D, STOP_E))
    // The early arrival and late departure should not have any effect on tests
    .arrivals(VERY_EARLY, L31_END)
    .departures(L31_START, VERY_LATE)
    .transitReluctanceIndex(TRANSIT_RELUCTANCE_INDEX)
    .build();

  public static final RaptorCostCalculator<TestTripSchedule> C1_CALCULATOR = new DefaultCostCalculator<>(
    BOARD_C1_SEC,
    TRANSFER_C1_SEC,
    WAIT_RELUCTANCE,
    TRANSIT_RELUCTANCE,
    STOP_C1S
  );

  public static final int TOTAL_C1 =
    ACCESS_C1 + LINE_11_C1 + TX_C1 + LINE_21_C1 + LINE_31_C1 + EGRESS_C1;

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
    var transfer = TestTransfers.transfer(STOP_C, TX_END - TX_START);
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
    var transfer = TestTransfers.transfer(STOP_E, TX_END - TX_START);
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
}
