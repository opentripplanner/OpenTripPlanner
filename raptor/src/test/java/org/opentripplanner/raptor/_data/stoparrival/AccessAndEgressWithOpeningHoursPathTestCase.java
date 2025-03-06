package org.opentripplanner.raptor._data.stoparrival;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.stoparrival.TestArrivals.access;
import static org.opentripplanner.raptor._data.stoparrival.TestArrivals.bus;
import static org.opentripplanner.raptor.api.model.RaptorCostConverter.toRaptorCost;
import static org.opentripplanner.raptor.api.model.RaptorValueFormatter.formatC1;
import static org.opentripplanner.utils.time.DurationUtils.durationInSeconds;
import static org.opentripplanner.utils.time.TimeUtils.time;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestCostCalculator;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTripPattern;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorCostConverter;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrival;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * This test case construct two Raptor paths for forward and reverse search, with and without
 * opening hours for the flex access and egress.
 * <p>
 * Case A with flex access and egress and one transit:
 * <ol>
 *     <li>Flex access</li>
 *     <li>Transit, BUS A</li>
 *     <li>Flex egress</li>
 * </ol>
 * <p>
 * Case B with walking between transit and flex:
 * <ol>
 *     <li>Flex access</li>
 *     <li>Walk transfer</li>
 *     <li>Transit. BUS B</li>
 *     <li>Walk transfer</li>
 *     <li>Flex egress</li>
 * </ol>
 */
public class AccessAndEgressWithOpeningHoursPathTestCase implements RaptorTestConstants {

  private static final int ZERO = 0;
  // The transit reluctance is ignored, any value should work
  private static final int TRANSIT_RELUCTANCE_INDEX = -1;
  public static final double WAIT_RELUCTANCE = 0.8;
  public static final int BOARD_C1_SEC = 60;
  public static final int TRANSFER_C1_SEC = 120;
  // The C1_CALCULATOR is not under test, so we use it to calculate correct cost values.
  public static final TestCostCalculator C1_CALCULATOR = new TestCostCalculator(
    BOARD_C1_SEC,
    TRANSFER_C1_SEC,
    WAIT_RELUCTANCE,
    null
  );

  public static final RaptorSlackProvider SLACK_PROVIDER = new DefaultSlackProvider(
    TRANSFER_SLACK,
    BOARD_SLACK,
    ALIGHT_SLACK
  );

  // FLEX Access 5m tx 1 ~ A. Note! The actual times might get time-shifted.
  public static final int ACCESS_DURATION = durationInSeconds("5m15s");
  public static final int ACCESS_C1 = toRaptorCost(600);
  // Using transfer reluctance is incorrect, we should use the cost from the access path
  public static final TestAccessEgress ACCESS = TestAccessEgress.flex(
    STOP_A,
    ACCESS_DURATION,
    ONE_RIDE,
    ACCESS_C1
  );
  // Alternative Flex access with restricted opening hours: 09:00 - 09:50
  public static final int ACCESS_OPEN = time("09:00");
  public static final int ACCESS_CLOSE = time("09:50");
  public static final TestAccessEgress ACCESS_W_OPENING_HOURS = ACCESS.openingHours(
    ACCESS_OPEN,
    ACCESS_CLOSE
  );

  // Transfers (A ~ Walk 1m ~ B) (Used in Case B only)
  public static final int TX1_START = time("10:05:15");
  public static final int TX1_END = time("10:06:15");
  public static final int TX1_DURATION = TX1_END - TX1_START;
  public static final RaptorTransfer TX1_TRANSFER = TestTransfer.transfer(STOP_B, TX1_DURATION);
  public static final RaptorTransfer TX1_TRANSFER_REV = TestTransfer.transfer(STOP_A, TX1_DURATION);
  public static final int TX1_C1 = TX1_TRANSFER.c1();

  // Trip A (B ~ BUS L11 10:08 10:20 ~ C)
  public static final int L1_START = time("10:08");
  public static final int L1_END = time("10:20");
  // The departure time with transfer_slack excluded
  public static final int L1_STOP_ARR_TIME = L1_END + ALIGHT_SLACK;
  public static final int L1_STOP_ARR_TIME_REV = L1_START - BOARD_SLACK;

  // Wait at least 1m45s (45s BOARD_SLACK and 60s TRANSFER_SLACK)
  public static final int L1_TRANSIT_DURATION = L1_END - L1_START;

  // Transfers (C ~ Walk 2m ~ D) (Used in Case B only)
  public static final int TX2_START = time("10:20:15");
  public static final int TX2_END = time("10:22:15");
  public static final int TX2_DURATION = TX2_END - TX2_START;
  public static final RaptorTransfer TX2_TRANSFER = TestTransfer.transfer(STOP_D, TX2_DURATION);
  public static final RaptorTransfer TX2_TRANSFER_REV = TestTransfer.transfer(STOP_C, TX2_DURATION);
  public static final int TX2_C1 = TX2_TRANSFER.c1();

  // Wait 15s (ALIGHT_SLACK)
  // D ~ FLEX Egress 6m tx 1 . Note! The actual times might get time-shifted.
  public static final int EGRESS_DURATION = durationInSeconds("6m");
  public static final int EGRESS_C1 = toRaptorCost(800);
  // Using transfer reluctance is incorrect, we should use the cost from the egress path
  public static final TestAccessEgress EGRESS = TestAccessEgress.flex(
    STOP_D,
    EGRESS_DURATION,
    ONE_RIDE,
    EGRESS_C1
  );
  public static final int EGRESS_OPENING = TimeUtils.time("10:30");
  public static final int EGRESS_CLOSING = TimeUtils.time("11:00");
  public static final TestAccessEgress EGRESS_W_OPENING_HOURS = EGRESS.openingHours(
    EGRESS_OPENING,
    EGRESS_CLOSING
  );

  public static final int EGRESS_C1_W_1M_SLACK =
    EGRESS_C1 + toRaptorCost(TRANSFER_C1_SEC) + C1_CALCULATOR.waitCost(TRANSFER_SLACK);
  public static final int EGRESS_C1_W_7M45S_SLACK =
    EGRESS_C1_W_1M_SLACK + C1_CALCULATOR.waitCost(durationInSeconds("6m45s"));
  public static final int EGRESS_C1_W_9M45S_SLACK =
    EGRESS_C1_W_1M_SLACK + C1_CALCULATOR.waitCost(durationInSeconds("8m45s"));

  public static final String LINE_A = "A";
  public static final String LINE_B = "B";

  public static final TestTripSchedule TRIP_A = TestTripSchedule.schedule(
    TestTripPattern.pattern(LINE_A, STOP_A, STOP_D)
  )
    .times(L1_START, L1_END)
    .build();

  public static final TestTripSchedule TRIP_B = TestTripSchedule.schedule(
    TestTripPattern.pattern(LINE_B, STOP_B, STOP_C)
  )
    .times(L1_START, L1_END)
    .build();

  public static final int L1_C1_EX_WAIT = C1_CALCULATOR.transitArrivalCost(
    C1_CALCULATOR.boardingCostRegularTransfer(false, L1_START, STOP_B, L1_START),
    ZERO,
    L1_TRANSIT_DURATION,
    TRIP_A,
    STOP_C
  );

  private static final int TOT_C1_A = toRaptorCost(2564);
  private static final int TOT_C1_W_OPENING_HOURS_A = toRaptorCost(3512);
  private static final int TOT_C1_B = toRaptorCost(2924);
  private static final int TOT_C1_W_OPENING_HOURS_B = toRaptorCost(3728);
  // Wait before 12m45s + ALIGHT SLACK 15s
  private static final int L1_C1_INC_WAIT_W_OPENING_HOURS_A =
    L1_C1_EX_WAIT + C1_CALCULATOR.waitCost(durationInSeconds("13m"));
  private static final int L1_C1_INC_WAIT_W_OPENING_HOURS_B =
    L1_C1_EX_WAIT + C1_CALCULATOR.waitCost(durationInSeconds("12m"));

  /* TEST CASES WITH EXPECTED TO-STRING TEXTS */

  public static DestinationArrival<TestTripSchedule> flexCaseAForwardSearch() {
    return flexForwardSearch(ACCESS, EGRESS, LINE_A);
  }

  public static String flexCaseAText() {
    return String.format(
      "Flex 5m15s 1x 10:01 10:06:15 %s ~ A 1m45s ~ " +
      "BUS A 10:08 10:20 12m C₁996 ~ D 1m15s ~ " +
      "Flex 6m 1x 10:21:15 10:27:15 %s " +
      "[10:01 10:27:15 26m15s Tₓ2 %s]",
      RaptorCostConverter.toString(ACCESS_C1),
      RaptorCostConverter.toString(EGRESS_C1_W_1M_SLACK),
      RaptorCostConverter.toString(TOT_C1_A)
    );
  }

  public static DestinationArrival<TestTripSchedule> flexCaseBForwardSearch() {
    return flexForwardSearch(ACCESS, EGRESS, LINE_B);
  }

  public static String flexCaseBText() {
    return String.format(
      "Flex 5m15s 1x 10:00 10:05:15 %s ~ A 0s ~ " +
      "Walk 1m 10:05:15 10:06:15 C₁120 ~ B 1m45s ~ " +
      "BUS B 10:08 10:20 12m C₁996 ~ C 15s ~ " +
      "Walk 2m 10:20:15 10:22:15 C₁240 ~ D 1m ~ " +
      "Flex 6m 1x 10:23:15 10:29:15 %s" +
      " [10:00 10:29:15 29m15s Tₓ2 %s]",
      RaptorCostConverter.toString(ACCESS_C1),
      RaptorCostConverter.toString(EGRESS_C1_W_1M_SLACK),
      RaptorCostConverter.toString(TOT_C1_B)
    );
  }

  public static DestinationArrival<TestTripSchedule> flexCaseAWithOpeningHoursForwardSearch() {
    return flexForwardSearch(ACCESS_W_OPENING_HOURS, EGRESS_W_OPENING_HOURS, LINE_A);
  }

  public static String flexCaseAWithOpeningHoursText() {
    return String.format(
      "Flex 5m15s 1x Open(9:00 9:50) 9:50 9:55:15 %s ~ A 12m45s ~ " +
      "BUS A 10:08 10:20 12m %s ~ D 10m ~ " +
      "Flex 6m 1x Open(10:30 11:00) 10:30 10:36 %s " +
      "[9:50 10:36 46m Tₓ2 %s]",
      formatC1(ACCESS_C1),
      formatC1(L1_C1_INC_WAIT_W_OPENING_HOURS_A),
      formatC1(EGRESS_C1_W_9M45S_SLACK),
      formatC1(TOT_C1_W_OPENING_HOURS_A)
    );
  }

  public static DestinationArrival<TestTripSchedule> flexCaseBWithOpeningHoursForwardSearch() {
    return flexForwardSearch(ACCESS_W_OPENING_HOURS, EGRESS_W_OPENING_HOURS, LINE_B);
  }

  public static String flexCaseBWithOpeningHoursText() {
    return String.format(
      "Flex 5m15s 1x Open(9:00 9:50) 9:50 9:55:15 %s ~ A 0s ~ " +
      "Walk 1m 9:55:15 9:56:15 C₁120 ~ B 11m45s ~ " +
      "BUS B 10:08 10:20 12m %s ~ C 15s ~ " +
      "Walk 2m 10:20:15 10:22:15 C₁240 ~ D 7m45s ~ " +
      "Flex 6m 1x Open(10:30 11:00) 10:30 10:36 %s" +
      " [9:50 10:36 46m Tₓ2 %s]",
      formatC1(ACCESS_C1),
      formatC1(L1_C1_INC_WAIT_W_OPENING_HOURS_B),
      formatC1(EGRESS_C1_W_7M45S_SLACK),
      formatC1(TOT_C1_W_OPENING_HOURS_B)
    );
  }

  public static DestinationArrival<TestTripSchedule> flexCaseAReverseSearch() {
    return flexReverseSearch(ACCESS, EGRESS, LINE_A);
  }

  public static DestinationArrival<TestTripSchedule> flexCaseBReverseSearch() {
    return flexReverseSearch(ACCESS, EGRESS, LINE_B);
  }

  public static DestinationArrival<TestTripSchedule> flexCaseAWithOpeningHoursReverseSearch() {
    return flexReverseSearch(ACCESS_W_OPENING_HOURS, EGRESS_W_OPENING_HOURS, LINE_A);
  }

  public static DestinationArrival<TestTripSchedule> flexCaseBWithOpeningHoursReverseSearch() {
    return flexReverseSearch(ACCESS_W_OPENING_HOURS, EGRESS_W_OPENING_HOURS, LINE_B);
  }

  @Test
  public void testSetup() {
    // Assert test data is configured correct

    // Assert all durations
    assertEquals(TX1_END - TX1_START, TX1_DURATION);
    assertEquals(L1_END - L1_START, L1_TRANSIT_DURATION);
    assertEquals(TX2_END - TX2_START, TX2_DURATION);

    // Asset proper wait times
    int txBoardSlack = TRANSFER_SLACK + BOARD_SLACK;
    assertEquals(TX1_END + txBoardSlack, L1_START);
    assertEquals(L1_END + ALIGHT_SLACK, TX2_START);

    // Assert cost
    // The calculator is not under test here, so we assert everything is as expected
    assertEquals(12000, TX1_C1);
    assertEquals(90000, L1_C1_EX_WAIT);
    assertEquals(24000, TX2_C1);
  }

  /* PRIVATE METHODS */

  private static DestinationArrival<TestTripSchedule> flexForwardSearch(
    RaptorAccessEgress accessPath,
    RaptorAccessEgress egressPath,
    String line
  ) {
    int departureTime, arrivalTime, waitTime;
    ArrivalView<TestTripSchedule> prevArrival;

    if (LINE_A.equals(line)) {
      // The latest time the access can arrive is the same as the TX1 arrival time in case B
      arrivalTime = accessPath.latestArrivalTime(TX1_END);
      prevArrival = access(accessPath.stop(), arrivalTime, accessPath);

      int waitCost = costL1ForwardIncWait(prevArrival.arrivalTime());
      prevArrival = bus(2, STOP_D, L1_STOP_ARR_TIME, waitCost, 0, TRIP_A, prevArrival);
    } else {
      arrivalTime = accessPath.latestArrivalTime(TX1_START);
      prevArrival = access(accessPath.stop(), arrivalTime, accessPath);
      int timeShift = TX1_START - prevArrival.arrivalTime();

      prevArrival = new Transfer(1, TX1_END - timeShift, TX1_TRANSFER, prevArrival);

      int waitCost = costL1ForwardIncWait(prevArrival.arrivalTime());
      prevArrival = bus(2, STOP_C, L1_STOP_ARR_TIME, waitCost, 0, TRIP_B, prevArrival);

      prevArrival = new Transfer(2, TX2_END, TX2_TRANSFER, prevArrival);
    }

    // Egress
    departureTime = prevArrival.arrivalTime() + TRANSFER_SLACK;
    // Time-shift departure time
    departureTime = egressPath.earliestDepartureTime(departureTime);
    arrivalTime = departureTime + egressPath.durationInSeconds();
    waitTime = departureTime - prevArrival.arrivalTime();
    int additionalCost =
      egressPath.c1() + toRaptorCost(waitTime * WAIT_RELUCTANCE + TRANSFER_C1_SEC);

    return new DestinationArrival<>(
      egressPath,
      prevArrival,
      arrivalTime,
      additionalCost,
      RaptorConstants.NOT_SET
    );
  }

  private static DestinationArrival<TestTripSchedule> flexReverseSearch(
    RaptorAccessEgress accessPath,
    RaptorAccessEgress egressPath,
    String line
  ) {
    int departureTime, arrivalTime, cost;
    ArrivalView<TestTripSchedule> prevArrival;

    if (LINE_A.equals(line)) {
      arrivalTime = L1_END + ALIGHT_SLACK + TRANSFER_SLACK;
      arrivalTime = egressPath.earliestDepartureTime(arrivalTime);
      prevArrival = access(egressPath.stop(), arrivalTime, egressPath);

      cost = costL1ReverseIncWait(prevArrival.arrivalTime());
      prevArrival = bus(2, STOP_A, L1_STOP_ARR_TIME_REV, cost, 0, TRIP_A, prevArrival);
    } else {
      arrivalTime = L1_END + ALIGHT_SLACK + TX2_DURATION + TRANSFER_SLACK;
      arrivalTime = egressPath.earliestDepartureTime(arrivalTime);
      prevArrival = access(egressPath.stop(), arrivalTime, egressPath);
      arrivalTime = prevArrival.arrivalTime() - TX2_DURATION;
      prevArrival = new Transfer(1, arrivalTime, TX2_TRANSFER_REV, prevArrival);
      cost = costL1ReverseIncWait(prevArrival.arrivalTime());
      prevArrival = bus(2, STOP_B, L1_STOP_ARR_TIME_REV, cost, 0, TRIP_B, prevArrival);
      arrivalTime = prevArrival.arrivalTime() - TX1_DURATION;
      prevArrival = new Transfer(2, arrivalTime, TX1_TRANSFER_REV, prevArrival);
    }

    // Access
    departureTime = prevArrival.arrivalTime() - TRANSFER_SLACK;
    // Time-shift departure time
    departureTime = accessPath.latestArrivalTime(departureTime);
    arrivalTime = departureTime - accessPath.durationInSeconds();
    int waitTime = prevArrival.arrivalTime() - departureTime;
    int additionalCost =
      accessPath.c1() + toRaptorCost(waitTime * WAIT_RELUCTANCE + TRANSFER_C1_SEC);

    return new DestinationArrival<>(
      accessPath,
      prevArrival,
      arrivalTime,
      additionalCost,
      RaptorConstants.NOT_SET
    );
  }

  private static int costL1ForwardIncWait(int prevArrivalTime) {
    int waitTime = L1_START - prevArrivalTime + ALIGHT_SLACK;
    return toRaptorCost(waitTime * WAIT_RELUCTANCE) + L1_C1_EX_WAIT;
  }

  private static int costL1ReverseIncWait(int prevArrivalTime) {
    int waitTime = (prevArrivalTime - L1_END) + BOARD_SLACK;
    return toRaptorCost(waitTime * WAIT_RELUCTANCE) + L1_C1_EX_WAIT;
  }
}
