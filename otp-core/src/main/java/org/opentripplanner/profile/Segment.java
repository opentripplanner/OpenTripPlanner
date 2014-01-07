package org.opentripplanner.profile;

import java.util.Collections;
import java.util.List;

import lombok.Getter;

import org.opentripplanner.profile.ProfileRouter.PatternRide;
import org.opentripplanner.profile.ProfileRouter.Ride;
import org.opentripplanner.profile.ProfileRouter.Stats;

import com.beust.jcommander.internal.Lists;

public class Segment {

    public static class SegmentPattern implements Comparable<SegmentPattern> {
        public String patternId;
        public int fromIndex;
        public int toIndex;
        public int nTrips;
        public SegmentPattern (PatternRide patternRide) {
            this.patternId = patternRide.pattern.patternId;
            this.fromIndex = patternRide.fromIndex;
            this.toIndex   = patternRide.toIndex;
            this.nTrips    = patternRide.pattern.nTrips;
        }
        @Override
        public int compareTo (SegmentPattern other) {
            return other.nTrips - this.nTrips;
        }
    }
    
    public static final double WALK_SPEED = 1.0; // m/sec
    public static final int SLACK = 60; // seconds

    @Getter int walkTime;
    @Getter Stats waitStats;

    @Getter String route;
    @Getter String from;
    @Getter String to;
    @Getter String fromName;
    @Getter String toName;
    @Getter String routeShortName;
    @Getter String routeLongName;
    @Getter Stats rideStats;
    @Getter List<SegmentPattern> segmentPatterns = Lists.newArrayList();

    public Segment (Ride ride) {
        route = ride.route.getId().getId();
        routeShortName = ride.route.getShortName();
        routeLongName = ride.route.getLongName();
        from = ride.from.getId().getId();
        to = ride.to.getId().getId();
        fromName = ride.from.getName();
        toName = ride.to.getName();
        rideStats = ride.getStats();
        for (PatternRide patternRide : ride.patternRides) {
            segmentPatterns.add(new SegmentPattern(patternRide));
        }
        Collections.sort(segmentPatterns);
        waitStats = characterizeTransfer(ride);
    }

    /* Maybe store transfer distances by stop pair, and look them up. */
    
    /** Side effect: sets walkTime as well. */
    public Stats characterizeTransfer (Ride ride) {
        Stats ret = new Stats();
        ret.min=200;
        ret.avg=400;
        ret.max=600;
        walkTime = (int) (ride.getTransferDistance() / WALK_SPEED);
        return ret;
    }
    
    /** 
     * Distances can be stored in rides, including the first and last distance. 
     * But waits must be calculated from full sets of patterns, which are not known until a round is over.
     */
//    public Stats characterizeTransferNew (Ride ride) {
//        Ride prev = ride.previous;
//        List<Integer> departures = ride.getSortedDepartures();
//        List<Integer> arrivals;
//        if (prev != null) { 
//            /* We have no arrivals because there is no previous ride. Find max wait, min is 0. */
//            arrivals = prev.getSortedArrivals(); // index is different in each pattern but stop is known.
//        } else {
//            arrivals = departures;
//        }
//        
//        int walkTime = (int) (ride.xfer.distance / WALK_SPEED);
//        List<Integer> waits = Lists.newArrayList();
//        
//        Iterator<Integer> departureIterator = departures.iterator(); 
//        int departure = departureIterator.next();
//        ARRIVAL : for (int arrival : arrivals) {
//            int boardTime = arrival + walkTime + SLACK;
//            while (departure <= boardTime) {
//                if (!departureIterator.hasNext()) break ARRIVAL;
//                departure = departureIterator.next();
//            }
//            waits.add(departure - boardTime);
//        }
//        return new Stats (waits);
//    }
//    
}
