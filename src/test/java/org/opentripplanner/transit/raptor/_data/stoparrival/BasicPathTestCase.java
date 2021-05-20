package org.opentripplanner.transit.raptor._data.stoparrival;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter.toOtpDomainCost;
import static org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter.toRaptorCost;
import static org.opentripplanner.util.time.DurationUtils.durationToStr;
import static org.opentripplanner.util.time.TimeUtils.time;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.DefaultCostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.path.DestinationArrival;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleSubscriptions;


/**
 * This class is used to create a journeys with stop arrivals.
 * <p>
 * It creates different data structures representing the same 'basic' trip to be used in unit-tests:
 * <pre>
 *   ~
 *   Origin 10:00:00
 *   ~ Walk 3m15s ~ A
 *   ~ BUS L11 10:04 10:35 ~ B
 *   ~ Walk 3m45s ~ C
 *   ~ BUS L21 11:00 11:23 ~ D
 *   ~ BUS L31 11:40 11:52 ~ E
 *   ~ Walk 7m45s
 *   ~ Destination 12:00
 *   Duration: 2h $8184
 * </pre>
 * The Trip has 2 transfers, 1 connected by walking and without. The trip start at 10:00 and ends
 * at 12:00, total 2 hours.
 */
public class BasicPathTestCase implements RaptorTestConstants {
    public static final String BASIC_PATH_AS_DETAILED_STRING =
            "Walk 3m15s 10:00 10:03:15 $390 "
            + "~ 1 45s ~ "
            + "BUS L11 10:04 10:35 31m $1998 "
            + "~ 2 15s ~ "
            + "Walk 3m45s 10:35:15 10:39 $450 "
            + "~ 3 21m ~ "
            + "BUS L21 11:00 11:23 23m $2640 "
            + "~ 4 17m ~ "
            + "BUS L31 11:40 11:52 12m $1776 "
            + "~ 5 15s ~ "
            + "Walk 7m45s 11:52:15 12:00 $930 "
            + "[10:00 12:00 2h $8184]";

    public static final String BASIC_PATH_AS_STRING =
        "Walk 3m15s ~ 1"
        + " ~ BUS L11 10:04 10:35 ~ 2"
        + " ~ Walk 3m45s ~ 3"
        + " ~ BUS L21 11:00 11:23 ~ 4"
        + " ~ BUS L31 11:40 11:52 ~ 5"
        + " ~ Walk 7m45s "
        + "[10:00 12:00 2h $8184]";

    private static final int BOARD_COST_SEC = 60;
    private static final int TRANSFER_COST_SEC = 120;
    private static final double[] TRANSIT_RELUCTANCE = new double[] { 1.0 };
    public static final int TRANSIT_RELUCTANCE_INDEX = 0;
    private static final double WAIT_RELUCTANCE = 0.8;
    private static final double WALK_RELUCTANCE = 2.0;

    /** Stop cost for stop NA, A, C, E .. H is zero(0), B: 30s, and D: 60s. ?=0, A=1 .. H=8 */
    private static final int[] STOP_COSTS = {0, 0, 3_000, 0, 6_000, 0, 0, 0, 0, 0};

    // Some times witch should not have eny effect on tests
    private static final int VERY_EARLY = time("00:00");
    private static final int VERY_LATE = time("23:59");

    // Access (Walk 3m15s ~ A)
    public static final int ACCESS_START = time("10:00");
    public static final int ACCESS_END = time("10:03:15");
    public static final int ACCESS_DURATION = ACCESS_END - ACCESS_START;
    public static final int ACCESS_COST = toRaptorCost(ACCESS_DURATION * WALK_RELUCTANCE);

    // Trip 1 (A ~ BUS L11 10:04 10:35 ~ B)
    public static final int L11_START = time("10:04");
    private static final int L11_END = time("10:35");
    public static final int L11_DURATION = L11_END - L11_START;
    private static final int L11_WAIT_DURATION = L11_START - ACCESS_END + ALIGHT_SLACK;
    public static final int LINE_11_COST = STOP_COSTS[STOP_A] + STOP_COSTS[STOP_B]
        + toRaptorCost(BOARD_COST_SEC + WAIT_RELUCTANCE * L11_WAIT_DURATION + L11_DURATION);

    // Transfers (B ~ Walk 3m45s ~ C)
    private static final int TX_START = time("10:35:15");
    private static final int TX_END = time("10:39:00");
    public static final int TX_DURATION = TX_END - TX_START;
    public static final int TX_COST = toRaptorCost(TX_DURATION * WALK_RELUCTANCE);

    // Trip 2 (C ~ BUS L21 11:00 11:23 ~ D)
    public static final int L21_START = time("11:00");
    private static final int L21_END = time("11:23");
    public static final int L21_DURATION = L21_END - L21_START;
    private static final int L21_WAIT_DURATION = L21_START - TX_END + ALIGHT_SLACK;
    public static final int LINE_21_COST = STOP_COSTS[STOP_C] + STOP_COSTS[STOP_D]
        + toRaptorCost(BOARD_COST_SEC + TRANSFER_COST_SEC + WAIT_RELUCTANCE * L21_WAIT_DURATION + L21_DURATION);

    // Trip 3 (D ~ BUS L31 11:40 11:52 ~ E)
    public static final int L31_START = time("11:40");
    private static final int L31_END = time("11:52");
    public static final int L31_DURATION = L31_END - L31_START;
    private static final int L31_WAIT_DURATION = L31_START - (L21_END + ALIGHT_SLACK) + ALIGHT_SLACK;
    public static final int LINE_31_COST = STOP_COSTS[STOP_D] + STOP_COSTS[STOP_E]
        + toRaptorCost(BOARD_COST_SEC + TRANSFER_COST_SEC + WAIT_RELUCTANCE * L31_WAIT_DURATION + L31_DURATION);

    // Egress (E ~ Walk 7m45s ~ )
    public static final int EGRESS_START = time("11:52:15");
    public static final int EGRESS_END = time("12:00");
    public static final int EGRESS_DURATION = EGRESS_END - EGRESS_START;
    public static final int EGRESS_COST = toRaptorCost(EGRESS_DURATION * WALK_RELUCTANCE);

    public static final int TRIP_DURATION = EGRESS_END - ACCESS_START;

    private static final RaptorTransfer ACCESS = walk(STOP_B, ACCESS_DURATION);
    private static final RaptorTransfer EGRESS = walk(STOP_F, EGRESS_DURATION);

    public static final String LINE_11 = "L11";
    public static final String LINE_21 = "L21";
    public static final String LINE_31 = "L31";

    private static final TestTripSchedule TRIP_1 = TestTripSchedule
        .schedule(pattern(LINE_11, STOP_A, STOP_B))
        .times(L11_START, L11_END)
        .build();
    private static final TestTripSchedule TRIP_2 = TestTripSchedule
        .schedule(pattern(LINE_21, STOP_C, STOP_D))
        .times(L21_START, L21_END)
        .build();
    private static final TestTripSchedule TRIP_3 = TestTripSchedule
        .schedule(pattern(LINE_31, STOP_D, STOP_E))
        // The early arrival and late departure should not have any effect on tests
        .arrivals(VERY_EARLY, L31_END)
        .departures(L31_START, VERY_LATE)
        .build();


    public static final CostCalculator<TestTripSchedule> COST_CALCULATOR = new DefaultCostCalculator<>(
            BOARD_COST_SEC,
            TRANSFER_COST_SEC,
            WALK_RELUCTANCE,
            WAIT_RELUCTANCE,
            STOP_COSTS,
            TRANSIT_RELUCTANCE
    );

    public static final RaptorSlackProvider SLACK_PROVIDER =
        RaptorSlackProvider.defaultSlackProvider(TRANSFER_SLACK, BOARD_SLACK, ALIGHT_SLACK);

    public static final int TOTAL_COST = ACCESS_COST + LINE_11_COST + TX_COST + LINE_21_COST
        + LINE_31_COST + EGRESS_COST;

    /** Wait time between trip L11 and L21 including slack */
    public static final int WAIT_TIME_L11_L21 = L21_START - L11_END - TX_DURATION;

    /** Wait time between trip L21 and L31 including slack */
    public static final int WAIT_TIME_L21_L31 = L31_START - L21_END;

    public static WorkerLifeCycle lifeCycle() {
        return new LifeCycleSubscriptions();
    }

    public static DestinationArrival<TestTripSchedule> basicTripByForwardSearch() {
        AbstractStopArrival prevArrival;
        prevArrival = new Access(STOP_A, ACCESS_START, ACCESS_END, ACCESS_COST);
        prevArrival = new Bus(1, STOP_B, L11_END, LINE_11_COST, TRIP_1, prevArrival);
        prevArrival = new Walk(1, STOP_C, TX_START, TX_END, TX_COST,  prevArrival);
        prevArrival = new Bus(2, STOP_D, L21_END, LINE_21_COST, TRIP_2, prevArrival);
        prevArrival = new Bus(3, STOP_E, L31_END, LINE_31_COST, TRIP_3, prevArrival);
        Egress egress = new Egress(EGRESS_START, EGRESS_END, EGRESS_COST, prevArrival);
        return new DestinationArrival<>(
                walk(egress.previous().stop(), egress.durationInSeconds()),
                egress.previous(),
                egress.arrivalTime(),
                egress.additionalCost()
        );
    }

    /**
     * This is the same itinerary as {@link #basicTripByForwardSearch()}, as found by a reverse
     * search:
     */
    public static DestinationArrival<TestTripSchedule> basicTripByReverseSearch() {
        AbstractStopArrival nextArrival;
        nextArrival = new Access(STOP_E, EGRESS_END, EGRESS_START, EGRESS_COST);
        // Board slack is subtracted from the arrival time to get the latest possible
        nextArrival = new Bus(1, STOP_D, L31_START, LINE_31_COST, TRIP_3, nextArrival);
        nextArrival = new Bus(2, STOP_C, L21_START, LINE_21_COST, TRIP_2, nextArrival);
        nextArrival = new Walk(2, STOP_B, TX_END, TX_START, TX_COST, nextArrival);
        nextArrival = new Bus(3, STOP_A, L11_START, LINE_11_COST, TRIP_1, nextArrival);
        Egress egress = new Egress(ACCESS_END, ACCESS_START, ACCESS_COST, nextArrival);
        return new DestinationArrival<>(
                walk(egress.previous().stop(), egress.durationInSeconds()),
                egress.previous(),
                egress.arrivalTime(),
                egress.additionalCost()
        );
    }

    /**
     * Both {@link #basicTripByForwardSearch()} and {@link #basicTripByReverseSearch()} should
     * return the same trip, here returned as a path.
     */
    public static Path<TestTripSchedule> basicTripAsPath() {
        PathLeg<TestTripSchedule> leg6 = new EgressPathLeg<>(
                EGRESS, STOP_E, EGRESS_START, EGRESS_END, toOtpDomainCost(EGRESS_COST)
        );
        TransitPathLeg<TestTripSchedule> leg5 = new TransitPathLeg<>(
            STOP_D, L31_START, STOP_E, L31_END, toOtpDomainCost(LINE_31_COST), TRIP_3, leg6
        );
        TransitPathLeg<TestTripSchedule> leg4 = new TransitPathLeg<>(
            STOP_C, L21_START, STOP_D, L21_END, toOtpDomainCost(LINE_21_COST), TRIP_2, leg5
        );
        var transfer = new RaptorTransfer() {
            @Override public int stop() { return STOP_C; }
            @Override public int durationInSeconds() { return TX_END - TX_START; }
        };
        PathLeg<TestTripSchedule> leg3 = new TransferPathLeg<>(
            STOP_B, TX_START, STOP_C, TX_END, toOtpDomainCost(TX_COST), transfer, leg4.asTransitLeg()
        );
        TransitPathLeg<TestTripSchedule> leg2 = new TransitPathLeg<>(
            STOP_A, L11_START, STOP_B, L11_END, toOtpDomainCost(LINE_11_COST), TRIP_1, leg3
        );
        AccessPathLeg<TestTripSchedule> leg1 = new AccessPathLeg<>(
            ACCESS, STOP_A, ACCESS_START, ACCESS_END, toOtpDomainCost(ACCESS_COST), leg2.asTransitLeg()
        );
        return new Path<>(1, leg1, toOtpDomainCost(TOTAL_COST));
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
            "Access: " + durationToStr(ACCESS_DURATION)
                + ", Line 11: " + durationToStr(L11_DURATION)
                + " (wait " + durationToStr(L11_WAIT_DURATION) + ")"
                + ", Tx: " + durationToStr(TX_DURATION)
                + ", Line 21: " + durationToStr(L21_DURATION)
                + " (wait " + durationToStr(L21_WAIT_DURATION) + ")"
                + ", Line 31: " + durationToStr(L31_DURATION)
                + " (wait " + durationToStr(L31_WAIT_DURATION) + ")"
                + ", Egress: " + durationToStr(EGRESS_DURATION)
            ,
            durationToStr(TRIP_DURATION),
            durationToStr(
                ACCESS_DURATION
                    + L11_DURATION + L11_WAIT_DURATION
                    + TX_DURATION
                    + L21_DURATION + L21_WAIT_DURATION
                    + L31_DURATION + L31_WAIT_DURATION
                    + EGRESS_DURATION
            )
        );

        // The calculator is not under test here, so we assert everything is as expected
        assertEquals(ACCESS_COST, COST_CALCULATOR.walkCost(ACCESS_DURATION));
        assertEquals(
            LINE_11_COST,
            COST_CALCULATOR.transitArrivalCost(
                true, STOP_A, L11_WAIT_DURATION, L11_DURATION, TRANSIT_RELUCTANCE_INDEX, STOP_B
            )
        );
        assertEquals(TX_COST, COST_CALCULATOR.walkCost(TX_DURATION));
        assertEquals(
            LINE_21_COST,
            COST_CALCULATOR.transitArrivalCost(
                false, STOP_C, L21_WAIT_DURATION, L21_DURATION, TRANSIT_RELUCTANCE_INDEX, STOP_D
            )
        );
        assertEquals(
            LINE_31_COST,
            COST_CALCULATOR.transitArrivalCost(
                false, STOP_D, L31_WAIT_DURATION, L31_DURATION, TRANSIT_RELUCTANCE_INDEX, STOP_E
            )
        );
        assertEquals(EGRESS_COST, COST_CALCULATOR.walkCost(EGRESS_DURATION));

        assertEquals(
            BASIC_PATH_AS_STRING,
            basicTripAsPath().toString()
        );
    }
}

