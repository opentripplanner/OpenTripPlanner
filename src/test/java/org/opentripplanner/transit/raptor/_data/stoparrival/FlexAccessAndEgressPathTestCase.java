package org.opentripplanner.transit.raptor._data.stoparrival;

import static org.junit.Assert.assertEquals;
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
    private static final int TOT_COST_A = 2514;
    private static final int TOT_COST_W_OPENING_HOURS_A = 3462;
    private static final int TOT_COST_B = 2874;
    private static final int TOT_COST_W_OPENING_HOURS_B = 3678;

    private static final String PATH_A =
            "Flex 5m15s 1tx ~ 1 ~ BUS A 10:08 10:20 ~ 4 ~ Flex 6m 1tx";

    public static final String FLEX_PATH_A = PATH_A
            + " [10:01 10:27:15 26m15s $" + TOT_COST_A + "]";

    public static final String FLEX_PATH_W_OPENING_HOURS_A = PATH_A
            + " [9:50 10:36 46m $" + TOT_COST_W_OPENING_HOURS_A + "]";

    private static final String PATH_B =
            "Flex 5m15s 1tx ~ 1 ~ Walk 1m ~ 2 ~ BUS B 10:08 10:20 ~ 3 ~ Walk 2m ~ 4 ~ Flex 6m 1tx";

    public static final String FLEX_PATH_B = PATH_B
            + " [10:00 10:29:15 29m15s $" + TOT_COST_B + "]";

    public static final String FLEX_PATH_W_OPENING_HOURS_B = PATH_B
            + " [9:50 10:36 46m $" + TOT_COST_W_OPENING_HOURS_B + "]";

    public static final double WALK_RELUCTANCE = 2.0;
    public static final double WAIT_RELUCTANCE = 0.8;
    public static final int BOARD_COST_SEC = 60;
    public static final int TRANSFER_COST_SEC = 120;

    // The COST_CALCULATOR is not under test, so we use it to calculate correct cost values.
    public static final DefaultCostCalculator<TestTripSchedule> COST_CALCULATOR = new DefaultCostCalculator<>(
            null, BOARD_COST_SEC, TRANSFER_COST_SEC, WALK_RELUCTANCE, WAIT_RELUCTANCE
    );

    // FLEX Access 5m tx 1 ~ A. Note! The actual times might get time-shifted.
    public static final int ACCESS_DURATION = DurationUtils.duration("5m15s");
    // Using transfer reluctance is incorrect, we should use the cost from the access path
    public static final int ACCESS_COST = COST_CALCULATOR.walkCost(ACCESS_DURATION);
    public static final TestTransfer ACCESS = flex(STOP_A, ACCESS_DURATION);

    // Alternative Flex access with restricted opening hours: 09:00 - 09:50
    public static final int ACCESS_OPEN = time("09:00");
    public static final int ACCESS_CLOSE = time("09:50");
    public static final TestTransfer ACCESS_W_OPENING_HOURS = ACCESS.openingHours(ACCESS_OPEN, ACCESS_CLOSE);

    // Transfers (A ~ Walk 1m ~ B) (Used in Case B only)
    public static final int TX1_START = time("10:05:15");
    public static final int TX1_END = time("10:06:15");
    public static final int TX1_DURATION = TX1_END - TX1_START;
    public static final int TX1_COST = COST_CALCULATOR.walkCost(TX1_DURATION);

    // Wait at least 1m45s (45s BOARD_SLACK and 60s TRANSFER_SLACK)

    // Trip A (B ~ BUS L11 10:08 10:20 ~ C)
    public static final int L1_START = time("10:08");
    public static final int L1_END = time("10:20");
    public static final int L1_DURATION = L1_END - L1_START;
    public static final int L1_COST_EX_WAIT = COST_CALCULATOR.transitArrivalCost(
            false, STOP_B, ALIGHT_SLACK, L1_DURATION, STOP_C
    );

    // Wait 15s (ALIGHT_SLACK)

    // Transfers (C ~ Walk 2m ~ D) (Used in Case B only)
    public static final int TX2_START = time("10:20:15");
    public static final int TX2_END = time("10:22:15");
    public static final int TX2_DURATION = TX2_END - TX2_START;
    public static final int TX2_COST = COST_CALCULATOR.walkCost(TX2_DURATION);

    // D ~ FLEX Egress 6m tx 1 . Note! The actual times might get time-shifted.
    public static final int EGRESS_DURATION = DurationUtils.duration("6m");
    // Using transfer reluctance is incorrect, we should use the cost from the egress path
    public static final int EGRESS_COST_EX_WAIT_AND_TX_COST = COST_CALCULATOR.walkCost(EGRESS_DURATION);
    public static final TestTransfer EGRESS = flex(STOP_C, EGRESS_DURATION);
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

    public static DestinationArrival<TestTripSchedule> flex_case_A_forwardSearch() {
        return flexForwardSearch(ACCESS, EGRESS, LINE_A);
    }

    public static DestinationArrival<TestTripSchedule> flex_case_B_forwardSearch() {
        return flexForwardSearch(ACCESS, EGRESS, LINE_B);
    }

    public static DestinationArrival<TestTripSchedule> flex_case_A_w_openingHours_forwardSearch() {
        return flexForwardSearch(ACCESS_W_OPENING_HOURS, EGRESS_W_OPENING_HOURS, LINE_A);
    }

    public static DestinationArrival<TestTripSchedule> flex_case_B_w_openingHours_forwardSearch() {
        return flexForwardSearch(ACCESS_W_OPENING_HOURS, EGRESS_W_OPENING_HOURS, LINE_B);
    }

    public static DestinationArrival<TestTripSchedule> flexForwardSearch(
            RaptorTransfer accessPath,
            RaptorTransfer egressPath,
            String line
    ) {
        int time, departureTime, arrivalTime, waitTime;
        AbstractStopArrival prevArrival;

        if(LINE_A.equals(line)) {
            arrivalTime = accessPath.latestArrivalTime(L1_START - (TRANSFER_SLACK + BOARD_SLACK));
            prevArrival = new Access(accessPath.stop(), arrivalTime, ACCESS_COST, accessPath);

            int waitCost = toRaptorCost((L1_START - prevArrival.arrivalTime()) * WAIT_RELUCTANCE);
            prevArrival = new Bus(2, STOP_D, L1_END+ALIGHT_SLACK, L1_COST_EX_WAIT + waitCost, TRIP_A, prevArrival);
        }
        else {
            arrivalTime = accessPath.latestArrivalTime(TX1_START);
            prevArrival = new Access(accessPath.stop(), arrivalTime, ACCESS_COST, accessPath);
            int timeShift = TX1_START - prevArrival.arrivalTime();

            prevArrival = new Walk(1, STOP_B, TX1_START-timeShift, TX1_END-timeShift, TX1_COST, prevArrival);
            int waitCost = toRaptorCost((L1_START - prevArrival.arrivalTime()) * WAIT_RELUCTANCE);
            prevArrival = new Bus(2, STOP_C, L1_END+ALIGHT_SLACK, L1_COST_EX_WAIT + waitCost, TRIP_B, prevArrival);
            prevArrival = new Walk(2, STOP_D, TX2_START, TX2_END, TX2_COST, prevArrival);
        }

        // Egress
        time = prevArrival.arrivalTime() + TRANSFER_SLACK;
        departureTime = egressPath.earliestDepartureTime(time);
        arrivalTime = departureTime + egressPath.durationInSeconds();
        waitTime = departureTime - prevArrival.arrivalTime();
        int additionalCost =  EGRESS_COST_EX_WAIT_AND_TX_COST
                + toRaptorCost(waitTime * WAIT_RELUCTANCE + TRANSFER_COST_SEC);

        return new DestinationArrival<>(egressPath, prevArrival, arrivalTime, additionalCost);
    }


    @Test
    public void testSetup() {
        // Assert test data is configured correct
        int cost;

        // Assert all durations
        assertEquals(TX1_END - TX1_START, TX1_DURATION);
        assertEquals(L1_END - L1_START, L1_DURATION);
        assertEquals(TX2_END - TX2_START, TX2_DURATION);

        // Asset proper wait times
        int txBoardSlack = TRANSFER_SLACK + BOARD_SLACK;
        assertEquals(TX1_END + txBoardSlack, L1_START);
        assertEquals(L1_END + ALIGHT_SLACK, TX2_START);

        // Assert cost
        // The calculator is not under test here, so we assert everything is as expected
        assertEquals(63000, ACCESS_COST);
        assertEquals(12000, TX1_COST);
        assertEquals(91200, L1_COST_EX_WAIT);
        assertEquals(24000, TX2_COST);
        assertEquals(72000, EGRESS_COST_EX_WAIT_AND_TX_COST);
    }
}
