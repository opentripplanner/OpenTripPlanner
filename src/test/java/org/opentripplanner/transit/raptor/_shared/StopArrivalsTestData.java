package org.opentripplanner.transit.raptor._shared;

import org.opentripplanner.transit.raptor.api.TestRaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.rangeraptor.transit.CostCalculator;

import java.util.Arrays;
import java.util.List;

import static org.opentripplanner.transit.raptor.api.TestRaptorTripSchedule.createTripScheduleUseingArrivalTimes;
import static org.opentripplanner.transit.raptor.util.TimeUtils.hm2time;


/**
 * This class is used to create a few journeys with stop arrivals.
 * <p/>
 * It creates different data structures representing the same 'basic' trip:
 * <p/>
 * {@code
 *   Walk 5:00 > Stop 1 > Transit 10:00-10:35 > Stop 2 > Walk 3:00 > Stop 3 > Transit 11:00-11:23 > Stop 4
 *   > Transit 11:40-11:53 > Stop 5 > Walk 7:00
 * }
 * <p/>
 * The Trip has 2 transfers, 1 connected by walking and without. The trip start at 09:53 and ends at 12:00.
 */
public class StopArrivalsTestData {

    private static final int T0945 = hm2time(9,45);
    private static final int T0950 = hm2time(9,50);
    private static final int T0953 = hm2time(9,53);
    private static final int T0955 = hm2time(9,55);
    private static final int T0958 = hm2time(9,58);
    private static final int T1000 = hm2time(10,0);
    private static final int T1035 = hm2time(10,35);
    private static final int T1038 = hm2time(10,38);
    private static final int T1100 = hm2time(11,0);
    private static final int T1123 = hm2time(11,23);
    private static final int T1140 = hm2time(11,40);
    private static final int T1153 = hm2time(11,53);
    private static final int T1200 = hm2time(12,0);
    private static final int T1202 = hm2time(12,2);
    private static final int T1203 = hm2time(12,3);
    private static final int T1210 = hm2time(12,10);

    private static final int STOP_1 = 1;
    private static final int STOP_2 = 2;
    private static final int STOP_3 = 3;
    private static final int STOP_4 = 4;
    private static final int STOP_5 = 5;

    public static final int BOARD_SLACK = 120;

    private static final TestRaptorTripSchedule TRIP_1 = createTripScheduleUseingArrivalTimes(T1000, T1035);
    private static final TestRaptorTripSchedule TRIP_2 = createTripScheduleUseingArrivalTimes(T1100, T1123);
    private static final TestRaptorTripSchedule TRIP_3 = createTripScheduleUseingArrivalTimes(T1140, T1153);

    /**
     * Create a list of stop arrivals with a destination arrival at the end like this:
     * <ol>
     *     <li> > Leave origin at 09:53 (the access leg is time shifted from 09:45 to match the transit board time)
     *     <li> Walk 5 minutes
     *     <li> > Stop 1 - wait 2 minutes
     *     <li> Bus 10:00 - 10:35
     *     <li> > Stop 2
     *     <li> Walk 3 minutes
     *     <li> > Stop 3 - wait 2 + 20 minutes
     *     <li> Bus 11:00 - 11:23
     *     <li> > Stop 4 - wait 2 + 15 minutes
     *     <li> Bus 11:40 - 11:53
     *     <li> > Stop 5
     *     <li> Walk 7 minutes
     *     <li> > Arrive at destination 12:00
     * </ol>
     */
    public static Egress basicTripByForwardSearch() {
        AbstractStopArrival arrival;
        arrival = new Access(STOP_1, T0945, T0950);
        arrival = new Bus(1, STOP_2, T1000, T1035, TRIP_1, arrival);
        arrival = new Walk(1, STOP_3, T1035, T1038, arrival);
        arrival = new Bus(2, STOP_4, T1100, T1123, TRIP_2, arrival);
        arrival = new Bus(3, STOP_5, T1140, T1153, TRIP_3, arrival);
        return new Egress(T1153, T1200, arrival);
    }

    /**
     * This is the same itinerary as {@link #basicTripByForwardSearch()}, as found by a reverse search:
     * <ol>
     *     <li> > Leave destination at 12:00
     *     <li> Walk 7 minutes
     *     <li> > Stop 5
     *     <li> Bus 11:53 - 11:40
     *     <li> > Stop 4 - wait 2 + 15 minutes
     *     <li> Bus 11:23 - 11:00
     *     <li> > Stop 3 - wait 2 minutes
     *     <li> Walk 3 minutes
     *     <li> > Stop 2  - wait 20 minutes
     *     <li> Bus 10:35 - 10:00
     *     <li> > Stop 1 - wait 2 minutes
     *     <li> Walk 5 minutes
     *     <li> > Arrive at origin 09:53
     * </ol>
     *
     * Note! The 2 minute wait (boardSlack) is added to the alight time, not prior to boarding.
     * This is done so to add the board slack in the same place as in the forward search.
     * <p/>
     * Tip! Think of this journey as traveling back in time and from destination to origin (search above).
     */
    public static Egress basicTripByReverseSearch() {
        AbstractStopArrival arrival;
        // The Access will be time-shifted to match the transit boarding;
        // hence the actual departure and arrival time will be 12:00 and 11:53
        arrival = new Access(STOP_5, T1210, T1203);
        // Board slack is subtracted from the arrival time to get the latest possible
        arrival = new Bus(1, STOP_4, T1153, T1140 - BOARD_SLACK, TRIP_3, arrival);
        arrival = new Bus(2, STOP_3, T1123, T1100 - BOARD_SLACK, TRIP_2, arrival);
        arrival = new Walk(2, STOP_2, T1038, T1035, arrival);
        arrival = new Bus(3, STOP_1, T1035, T1000 - BOARD_SLACK, TRIP_1, arrival);
        return new Egress(T0958, T0953, arrival);
    }

    /**
     * Both {@link #basicTripByForwardSearch()} and {@link #basicTripByReverseSearch()} should return the same trip,
     * here returned as a path.
     */
    public static Path<TestRaptorTripSchedule> basicTripAsPath() {
        PathLeg<TestRaptorTripSchedule> leg6 = new EgressPathLeg<>(STOP_5, T1153, T1200);
        PathLeg<TestRaptorTripSchedule> leg5 = new TransitPathLeg<>(STOP_4, T1140, STOP_5, T1153, TRIP_3, leg6);
        PathLeg<TestRaptorTripSchedule> leg4 = new TransitPathLeg<>(STOP_3, T1100, STOP_4, T1123, TRIP_2, leg5);
        PathLeg<TestRaptorTripSchedule> leg3 = new TransferPathLeg<>(STOP_2, T1035, STOP_3, T1038, leg4.asTransitLeg());
        PathLeg<TestRaptorTripSchedule> leg2 = new TransitPathLeg<>(STOP_1, T1000, STOP_2, T1035, TRIP_1, leg3);
        AccessPathLeg<TestRaptorTripSchedule> leg1 = new AccessPathLeg<>(T0953, STOP_1, T0958, leg2.asTransitLeg());

        return new Path<>(leg1, T1200, 2, CostCalculator.toOtpDomainCost(6_000));
    }

    public static List<Integer> basicTripStops() {
        return Arrays.asList(STOP_1, STOP_2, STOP_3, STOP_4, STOP_5);
    }

}

