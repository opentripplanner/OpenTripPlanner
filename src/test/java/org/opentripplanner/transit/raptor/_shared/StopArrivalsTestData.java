package org.opentripplanner.transit.raptor._shared;

import org.junit.Assert;
import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.transit.DefaultCostCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleSubscriptions;

import java.util.Arrays;
import java.util.List;

import static org.opentripplanner.transit.raptor.util.TimeUtils.hm2time;


/**
 * This class is used to create a journeys with stop arrivals.
 * <p>
 * It creates different data structures representing the same 'basic' trip to be used in unit-tests:
 * <pre>
 *   0. Start at 10:00
 *   1. Walk 5 minutes to Stop 1,
 *      Wait: 2 minutes board-slack
 *   2. Transit 10:07-10:35 to Stop 2
 *      Wait: 1 minute (1m alight-slack)
 *   3. Walk 3 minutes, 10:36-10:39 to Stop 3
 *      Wait: 21 minutes for transit (> 4m transfer-slack + 2m board-slack)
 *   4. Transit 11:00-11:23 to Stop 4
 *      Wait: 17 minutes for transit
 *   5. Transit 11:40-11:53 to Stop 5
 *      Wait: 1 minute alight-slack
 *   6. Walk 6 minutes to destination
 *   7. End at 12:00
 * </pre>
 * The Trip has 2 transfers, 1 connected by walking and without. The trip start at 09:53 and ends at 12:00.
 */
public class StopArrivalsTestData {

    public static final WorkerLifeCycle WORKER_LIFE_CYCLE = new LifeCycleSubscriptions();

    /** Provide a Cost Calculator for tests that dont care to much avout testing the cost */
    public static final CostCalculator COST_CALCULATOR = new DefaultCostCalculator(
        new int[] { 0, 0, 0, 0, 0},
        0,
        2.0,
        1.0,
        new LifeCycleSubscriptions()
    );

    // We use 2 minutes slack distributed by (in seconds):
    private static final int TRANSFER_SLACK = 4 * 60;
    private static final int BOARD_SLACK = 2 * 60;
    private static final int ALIGHT_SLACK = 60;

    // Some times witch should not have eny effect on tests
    public static final int VERY_EARLY = hm2time(0, 0);
    public static final int VERY_LATE = hm2time(23, 59);

    // Access
    public static final int A_START = hm2time(10,0);
    public static final int A_END = hm2time(10,3);
    // Trip 1
    public static final int T1_START = hm2time(10,5);
    public static final int T1_END = hm2time(10,35);
    // Transfers (Walk)
    public static final int W1_START = hm2time(10,36);
    public static final int W1_END = hm2time(10,39);
    // Trip 2
    public static final int T2_START = hm2time(11,0);
    public static final int T2_END = hm2time(11,23);
    // Trip 3
    public static final int T3_START = hm2time(11,40);
    public static final int T3_END = hm2time(11,52);
    // Egress
    public static final int E_START = hm2time(11,53);
    public static final int E_END = hm2time(12,0);

    public static final int TRIP_DURATION = E_END - A_START;


    private static final int STOP_1 = 1;
    private static final int STOP_2 = 2;
    private static final int STOP_3 = 3;
    private static final int STOP_4 = 4;
    private static final int STOP_5 = 5;


    public static final RaptorSlackProvider SLACK_PROVIDER = RaptorSlackProvider.defaults(
            TRANSFER_SLACK, BOARD_SLACK, ALIGHT_SLACK
    );

    private static final TestRaptorTripSchedule TRIP_1 = TestRaptorTripSchedule
            .create("T1")
            .withBoardAndAlightTimes(T1_START, T1_END)
            .withStopIndexes(STOP_1, STOP_2)
            .build();
    private static final TestRaptorTripSchedule TRIP_2 = TestRaptorTripSchedule
            .create("T2")
            .withBoardAndAlightTimes(T2_START, T2_END)
            .withStopIndexes(STOP_3, STOP_4)
            .build();
    private static final TestRaptorTripSchedule TRIP_3 = TestRaptorTripSchedule
            .create("T3")
            // The early arrival and late departure should not have any effect on tests
            .withAlightTimes(VERY_EARLY, T3_END)
            .withBoardTimes(T3_START, VERY_LATE)
            .withStopIndexes(STOP_4, STOP_5)
            .build();

    static {
        // Assert test data is configured correct
        Assert.assertEquals(A_END + BOARD_SLACK, T1_START);
        Assert.assertEquals(T3_END + ALIGHT_SLACK, E_START);
    }


    public static Egress basicTripByForwardSearch() {
        AbstractStopArrival prevArrival;
        int timeShift = 3 * 60;
        prevArrival = new Access(STOP_1, A_START-timeShift, A_END-timeShift);
        prevArrival = new Bus(1, STOP_2, T1_START, T1_END + ALIGHT_SLACK, TRIP_1, prevArrival);
        prevArrival = new Walk(1, STOP_3, W1_START, W1_END, prevArrival);
        prevArrival = new Bus(2, STOP_4, T2_START, T2_END + ALIGHT_SLACK, TRIP_2, prevArrival);
        prevArrival = new Bus(3, STOP_5, T3_START, T3_END + ALIGHT_SLACK, TRIP_3, prevArrival);
        return new Egress(E_START, E_END, prevArrival);
    }

    /**
     * This is the same itinerary as {@link #basicTripByForwardSearch()}, as found by a reverse search:
     */
    public static Egress basicTripByReverseSearch() {
        AbstractStopArrival arrival;
        int timeShift = 3 * 60;
        arrival = new Access(STOP_5, E_END+timeShift, E_START+timeShift);
        // Board slack is subtracted from the arrival time to get the latest possible
        arrival = new Bus(1, STOP_4, T3_END, T3_START - BOARD_SLACK, TRIP_3, arrival);
        arrival = new Bus(2, STOP_3, T2_END, T2_START - BOARD_SLACK, TRIP_2, arrival);
        arrival = new Walk(2, STOP_2, W1_END, W1_START, arrival);
        arrival = new Bus(3, STOP_1, T1_END, T1_START - BOARD_SLACK, TRIP_1, arrival);
        return new Egress(A_END, A_START, arrival);
    }

    /**
     * Both {@link #basicTripByForwardSearch()} and {@link #basicTripByReverseSearch()} should return the same trip,
     * here returned as a path.
     */
    public static Path<TestRaptorTripSchedule> basicTripAsPath() {
        PathLeg<TestRaptorTripSchedule> leg6 = new EgressPathLeg<>(null, STOP_5, E_START, E_END);
        TransitPathLeg<TestRaptorTripSchedule> leg5 = new TransitPathLeg<>(
                STOP_4, T3_START, STOP_5, T3_END, TRIP_3, leg6
        );
        TransitPathLeg<TestRaptorTripSchedule> leg4 = new TransitPathLeg<>(
                STOP_3, T2_START, STOP_4, T2_END, TRIP_2, leg5
        );
        PathLeg<TestRaptorTripSchedule> leg3 = new TransferPathLeg<>(
                STOP_2, W1_START, STOP_3, W1_END, leg4.asTransitLeg()
        );
        TransitPathLeg<TestRaptorTripSchedule> leg2 = new TransitPathLeg<>(
                STOP_1, T1_START, STOP_2, T1_END, TRIP_1, leg3
        );
        AccessPathLeg<TestRaptorTripSchedule> leg1 = new AccessPathLeg<>(
                null, STOP_1, A_START, A_END, leg2.asTransitLeg()
        );

        return new Path<>(1, leg1, RaptorCostConverter.toOtpDomainCost(6_000));
    }

    public static List<Integer> basicTripStops() {
        return Arrays.asList(STOP_1, STOP_2, STOP_3, STOP_4, STOP_5);
    }

}

