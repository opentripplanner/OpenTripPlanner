package org.opentripplanner.transit.raptor._data.stoparrival;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.TRANSIT_RELUCTANCE_INDEX;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.flex;
import static org.opentripplanner.transit.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter.toRaptorCost;
import static org.opentripplanner.util.time.TimeUtils.time;

import org.junit.Test;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTransfer;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.DefaultCostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.rangeraptor.path.DestinationArrival;
import org.opentripplanner.util.time.DurationUtils;
import org.opentripplanner.util.time.TimeUtils;

/**
 * This test case construct two Raptor paths for forward and reverse search, with and without
 * opening hours for the flex access and egress.
 * <p>
 * Case A with flex access and egress and one transit:
 * <ol>
 *     <li>Flex access</li>
 *     <li>Transit, BUS A</li>
 *     <li>Flex </li>
 * </ol>
 * <p>
 * Case B with walking between transit and flex:
 * <ol>
 *     <li>Flex access</li>
 *     <li>Walk transfer</li>
 *     <li>Transit. BUS B</li>
 *     <li>Walk transfer</li>
 *     <li>Flex </li>
 * </ol>
 */
public class FlexAccessAndEgressPathTestCase implements RaptorTestConstants {

    public static final double WALK_RELUCTANCE = 2.0;
    public static final double WAIT_RELUCTANCE = 0.8;
    public static final int BOARD_COST_SEC = 60;
    public static final int TRANSFER_COST_SEC = 120;
    // The COST_CALCULATOR is not under test, so we use it to calculate correct cost values.
    public static final DefaultCostCalculator<TestTripSchedule> COST_CALCULATOR = new DefaultCostCalculator<>(
            BOARD_COST_SEC, TRANSFER_COST_SEC, WALK_RELUCTANCE, WAIT_RELUCTANCE, null, null
    );
    // FLEX Access 5m tx 1 ~ A. Note! The actual times might get time-shifted.
    public static final int ACCESS_DURATION = DurationUtils.duration("5m15s");
    // Using transfer reluctance is incorrect, we should use the cost from the access path
    public static final int ACCESS_COST = COST_CALCULATOR.walkCost(ACCESS_DURATION);
    public static final TestTransfer ACCESS = flex(STOP_A, ACCESS_DURATION);
    // Alternative Flex access with restricted opening hours: 09:00 - 09:50
    public static final int ACCESS_OPEN = time("09:00");
    public static final int ACCESS_CLOSE = time("09:50");
    public static final TestTransfer ACCESS_W_OPENING_HOURS =
            ACCESS.openingHours(ACCESS_OPEN, ACCESS_CLOSE);
    // Transfers (A ~ Walk 1m ~ B) (Used in Case B only)
    public static final int TX1_START = time("10:05:15");
    public static final int TX1_END = time("10:06:15");
    public static final int TX1_DURATION = TX1_END - TX1_START;
    public static final int TX1_COST = COST_CALCULATOR.walkCost(TX1_DURATION);
    // Trip A (B ~ BUS L11 10:08 10:20 ~ C)
    public static final int L1_START = time("10:08");
    public static final int L1_END = time("10:20");
    // The departure time with transfer_slack excluded
    public static final int L1_STOP_ARR_TIME = L1_END + ALIGHT_SLACK;
    public static final int L1_STOP_ARR_TIME_REV = L1_START - BOARD_SLACK;

    // Wait at least 1m45s (45s BOARD_SLACK and 60s TRANSFER_SLACK)
    public static final int L1_TRANSIT_DURATION = L1_END - L1_START;
    public static final int L1_COST_EX_WAIT = COST_CALCULATOR.transitArrivalCost(
            false, STOP_B, 0, L1_TRANSIT_DURATION, TRANSIT_RELUCTANCE_INDEX, STOP_C
    );
    // Transfers (C ~ Walk 2m ~ D) (Used in Case B only)
    public static final int TX2_START = time("10:20:15");
    public static final int TX2_END = time("10:22:15");
    public static final int TX2_DURATION = TX2_END - TX2_START;
    public static final int TX2_COST = COST_CALCULATOR.walkCost(TX2_DURATION);

    // Wait 15s (ALIGHT_SLACK)
    // D ~ FLEX Egress 6m tx 1 . Note! The actual times might get time-shifted.
    public static final int EGRESS_DURATION = DurationUtils.duration("6m");
    // Using transfer reluctance is incorrect, we should use the cost from the egress path
    public static final int EGRESS_COST = COST_CALCULATOR.walkCost(EGRESS_DURATION);
    public static final TestTransfer EGRESS = flex(STOP_D, EGRESS_DURATION);
    public static final TestTransfer EGRESS_W_OPENING_HOURS =
            EGRESS.openingHours(TimeUtils.time("10:30"), TimeUtils.time("11:00"));
    public static final String LINE_A = "A";
    public static final String LINE_B = "B";
    public static final TestTripSchedule TRIP_A = TestTripSchedule
            .schedule(pattern(LINE_A, STOP_A, STOP_D))
            .times(L1_START, L1_END)
            .build();
    public static final TestTripSchedule TRIP_B = TestTripSchedule
            .schedule(pattern(LINE_B, STOP_B, STOP_C))
            .times(L1_START, L1_END)
            .build();
    private static final int TOT_COST_A = 2514;
    private static final int TOT_COST_W_OPENING_HOURS_A = 3462;
    private static final int TOT_COST_B = 2874;
    private static final int TOT_COST_W_OPENING_HOURS_B = 3678;


    /* TEST CASES WITH EXPECTED TO-STRING TEXTS */

    public static DestinationArrival<TestTripSchedule> flexCaseAForwardSearch() {
        return flexForwardSearch(ACCESS, EGRESS, LINE_A);
    }

    public static String flexCaseAForwardSearchAsText() {
        return flexCaseAText(630, 888);
    }

    public static DestinationArrival<TestTripSchedule> flexCaseBForwardSearch() {
        return flexForwardSearch(ACCESS, EGRESS, LINE_B);
    }

    public static String flexCaseBForwardSearchAsText() {
        return flexCaseBText(630, 888);
    }

    public static DestinationArrival<TestTripSchedule> flexCaseAWithOpeningHoursForwardSearch() {
        return flexForwardSearch(ACCESS_W_OPENING_HOURS, EGRESS_W_OPENING_HOURS, LINE_A);
    }

    public static String flexCaseAWithOpeningHoursForwardSearchAsText() {
        return flexCaseAWithOpeningHours(630, 1524, 1308);
    }

    public static DestinationArrival<TestTripSchedule> flexCaseBWithOpeningHoursForwardSearch() {
        return flexForwardSearch(ACCESS_W_OPENING_HOURS, EGRESS_W_OPENING_HOURS, LINE_B);
    }

    public static String flexCaseBWithOpeningHoursForwardSearchAsText() {
        return flexCaseBWithOpeningHours(630, 1476, 1212);
    }

    public static DestinationArrival<TestTripSchedule> flexCaseAReverseSearch() {
        return flexReverseSearch(ACCESS, EGRESS, LINE_A);
    }

    public static String flexCaseAReverseSearchAsText() {
        return flexCaseAText(798, 720);
    }

    public static DestinationArrival<TestTripSchedule> flexCaseBReverseSearch() {
        return flexReverseSearch(ACCESS, EGRESS, LINE_B);
    }

    public static String flexCaseBReverseSearchAsText() {
        return flexCaseBText(798, 720);
    }

    public static DestinationArrival<TestTripSchedule> flexCaseAWithOpeningHoursReverseSearch() {
        return flexReverseSearch(ACCESS_W_OPENING_HOURS, EGRESS_W_OPENING_HOURS, LINE_A);
    }

    public static String flexCaseAWithOpeningHoursReverseSearchAsText() {
        return flexCaseAWithOpeningHours(1326, 1416, 720);
    }

    public static DestinationArrival<TestTripSchedule> flexCaseBWithOpeningHoursReverseSearch() {
        return flexReverseSearch(ACCESS_W_OPENING_HOURS, EGRESS_W_OPENING_HOURS, LINE_B);
    }

    public static String flexCaseBWithOpeningHoursReverseSearchAsText() {
        return flexCaseBWithOpeningHours(1278, 1320, 720);
    }

    @Test
    public void testSetup() {
        // Assert test data is configured correct
        int cost;

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
        assertEquals(63000, ACCESS_COST);
        assertEquals(12000, TX1_COST);
        assertEquals(90000, L1_COST_EX_WAIT);
        assertEquals(24000, TX2_COST);
        assertEquals(72000, EGRESS_COST);
    }


    /* PRIVATE METHODS */

    private static String flexCaseAText(int accessCost, int egressCost) {
        assertEquals(TOT_COST_A, accessCost + 996 + egressCost);
        return String.format(
                "Flex 5m15s 1tx 10:01 10:06:15 $%d ~ 1 1m45s ~ "
                        + "BUS A 10:08 10:20 12m $996 ~ 4 1m15s ~ "
                        + "Flex 6m 1tx 10:21:15 10:27:15 $%d "
                        + "[10:01 10:27:15 26m15s $%d]",
                accessCost, egressCost, TOT_COST_A
        );
    }

    private static String flexCaseAWithOpeningHours(int accessCost, int busCost, int egressCost) {
        assertEquals(TOT_COST_W_OPENING_HOURS_A, accessCost + busCost + egressCost);
        return String.format(
                "Flex 5m15s 1tx 9:50 9:55:15 $%d ~ 1 12m45s ~ "
                        + "BUS A 10:08 10:20 12m $%d ~ 4 10m ~ "
                        + "Flex 6m 1tx 10:30 10:36 $%d "
                        + "[9:50 10:36 46m $%d]",
                accessCost, busCost, egressCost, TOT_COST_W_OPENING_HOURS_A
        );
    }

    private static String flexCaseBText(int accessCost, int egressCost) {
        assertEquals(TOT_COST_B, accessCost + 120 + 996 + 240 + egressCost);
        return String.format(
                "Flex 5m15s 1tx 10:00 10:05:15 $%d ~ 1 0s ~ "
                        + "Walk 1m 10:05:15 10:06:15 $120 ~ 2 1m45s ~ "
                        + "BUS B 10:08 10:20 12m $996 ~ 3 15s ~ "
                        + "Walk 2m 10:20:15 10:22:15 $240 ~ 4 1m ~ "
                        + "Flex 6m 1tx 10:23:15 10:29:15 $%d"
                        + " [10:00 10:29:15 29m15s $%d]",
                accessCost, egressCost, TOT_COST_B
        );
    }

    private static String flexCaseBWithOpeningHours(int accessCost, int busCost, int egressCost) {
        //assertEquals(TOT_COST_W_OPENING_HOURS_B, accessCost + 120 + busCost + 240 + egressCost);
        return String.format(
                "Flex 5m15s 1tx 9:50 9:55:15 $%d ~ 1 0s ~ "
                        + "Walk 1m 9:55:15 9:56:15 $120 ~ 2 11m45s ~ "
                        + "BUS B 10:08 10:20 12m $%d ~ 3 15s ~ "
                        + "Walk 2m 10:20:15 10:22:15 $240 ~ 4 7m45s ~ "
                        + "Flex 6m 1tx 10:30 10:36 $%d"
                        + " [9:50 10:36 46m $%d]",
                accessCost, busCost, egressCost, TOT_COST_W_OPENING_HOURS_B
        );
    }

    private static DestinationArrival<TestTripSchedule> flexForwardSearch(
            RaptorTransfer accessPath,
            RaptorTransfer egressPath,
            String line
    ) {
        int departureTime, arrivalTime, waitTime;
        AbstractStopArrival prevArrival;

        if (LINE_A.equals(line)) {
            // The latest time the access can arrive is the same as the TX1 arrival time in case B
            arrivalTime = accessPath.latestArrivalTime(TX1_END);
            prevArrival = new Access(accessPath.stop(), arrivalTime, ACCESS_COST, accessPath);

            int waitCost = waitCostTripL1Forward(prevArrival.arrivalTime());
            prevArrival = new Bus(2, STOP_D, L1_STOP_ARR_TIME, waitCost, TRIP_A, prevArrival);
        }
        else {
            arrivalTime = accessPath.latestArrivalTime(TX1_START);
            prevArrival = new Access(accessPath.stop(), arrivalTime, ACCESS_COST, accessPath);
            int timeShift = TX1_START - prevArrival.arrivalTime();

            prevArrival = new Walk(1, STOP_B, TX1_START - timeShift, TX1_END - timeShift, TX1_COST,
                    prevArrival
            );

            int waitCost = waitCostTripL1Forward(prevArrival.arrivalTime());
            prevArrival = new Bus(2, STOP_C, L1_STOP_ARR_TIME, waitCost, TRIP_B, prevArrival);

            prevArrival = new Walk(2, STOP_D, TX2_START, TX2_END, TX2_COST, prevArrival);
        }

        // Egress
        departureTime = prevArrival.arrivalTime() + TRANSFER_SLACK;
        // Time-shift departure time
        departureTime = egressPath.earliestDepartureTime(departureTime);
        arrivalTime = departureTime + egressPath.durationInSeconds();
        waitTime = departureTime - prevArrival.arrivalTime();
        int additionalCost = EGRESS_COST
                + toRaptorCost(waitTime * WAIT_RELUCTANCE + TRANSFER_COST_SEC);

        return new DestinationArrival<>(egressPath, prevArrival, arrivalTime, additionalCost);
    }

    private static DestinationArrival<TestTripSchedule> flexReverseSearch(
            RaptorTransfer accessPath,
            RaptorTransfer egressPath,
            String line
    ) {
        int departureTime, arrivalTime, cost;
        AbstractStopArrival prevArrival;

        if (LINE_A.equals(line)) {
            arrivalTime = L1_END + ALIGHT_SLACK + TRANSFER_SLACK;
            arrivalTime = egressPath.earliestDepartureTime(arrivalTime);
            prevArrival = new Access(egressPath.stop(), arrivalTime, EGRESS_COST, egressPath);

            cost = waitCostTripL1Reverse(prevArrival.arrivalTime());
            prevArrival = new Bus(2, STOP_A, L1_STOP_ARR_TIME_REV, cost, TRIP_A, prevArrival);
        }
        else {
            arrivalTime = L1_END + ALIGHT_SLACK + TX2_DURATION + TRANSFER_SLACK;
            arrivalTime = egressPath.earliestDepartureTime(arrivalTime);
            prevArrival = new Access(egressPath.stop(), arrivalTime, EGRESS_COST, egressPath);
            arrivalTime = prevArrival.arrivalTime() - TX2_DURATION;
            departureTime = arrivalTime + TX2_DURATION;
            prevArrival = new Walk(1, STOP_C, departureTime, arrivalTime, TX2_COST, prevArrival);
            cost = waitCostTripL1Reverse(prevArrival.arrivalTime());
            prevArrival = new Bus(2, STOP_B, L1_STOP_ARR_TIME_REV, cost, TRIP_B, prevArrival);
            prevArrival =
                    new Walk(2, STOP_A, L1_STOP_ARR_TIME_REV, L1_STOP_ARR_TIME_REV - TX1_DURATION,
                            TX1_COST, prevArrival
                    );
        }

        // Access
        departureTime = prevArrival.arrivalTime() - TRANSFER_SLACK;
        // Time-shift departure time
        departureTime = accessPath.latestArrivalTime(departureTime);
        arrivalTime = departureTime - accessPath.durationInSeconds();
        int waitTime = prevArrival.arrivalTime() - departureTime;
        int additionalCost =
                ACCESS_COST + toRaptorCost(waitTime * WAIT_RELUCTANCE + TRANSFER_COST_SEC);

        return new DestinationArrival<>(accessPath, prevArrival, arrivalTime, additionalCost);
    }


    private static int waitCostTripL1Forward(int prevArrivalTime) {
        int waitTime = L1_START - prevArrivalTime + ALIGHT_SLACK;
        return toRaptorCost(waitTime * WAIT_RELUCTANCE) + L1_COST_EX_WAIT;
    }

    private static int waitCostTripL1Reverse(int prevArrivalTime) {
        int waitTime = (prevArrivalTime - L1_END) + BOARD_SLACK;
        return toRaptorCost(waitTime * WAIT_RELUCTANCE) + L1_COST_EX_WAIT;
    }
}
