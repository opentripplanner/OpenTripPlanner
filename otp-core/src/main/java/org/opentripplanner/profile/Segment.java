package org.opentripplanner.profile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

import org.opentripplanner.profile.ProfileRouter.PatternRide;
import org.opentripplanner.profile.ProfileRouter.Ride;
import org.opentripplanner.profile.ProfileRouter.Stats;
import org.opentripplanner.routing.trippattern.TripTimes;

import com.beust.jcommander.internal.Lists;

public class Segment {

    public static class SegmentPattern implements Comparable<SegmentPattern> {
        public String patternId;
        public int fromIndex;
        public int toIndex;
        public int nTrips;
        //public Stats stats;
        public SegmentPattern (PatternRide patternRide) {
            this.patternId = patternRide.pattern.getCode();
            this.fromIndex = patternRide.fromIndex;
            this.toIndex   = patternRide.toIndex;
            this.nTrips    = patternRide.pattern.getNumScheduledTrips();
            //this.stats     = patternRide.stats;
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
        walkTime = (int) (ride.getTransferDistance() / WALK_SPEED);
        waitStats = statsForBoarding (ride);
    }

    /* Maybe store transfer distances by stop pair, and look them up. */
    
    public int[] getSortedDepartures(Ride ride) {
        int nTimes = 0;
        for (PatternRide patternRide : ride.patternRides) {
            nTimes += patternRide.pattern.getNumScheduledTrips();
        }
        int[] departures = new int[nTimes];
        int i = 0;
        for (PatternRide patternRide : ride.patternRides) {
            for (TripTimes tt : patternRide.pattern.getScheduledTimetable().getTripTimes()) {
                departures[i++] = tt.getDepartureTime(patternRide.fromIndex);
            }
        }
        Arrays.sort(departures);
        return departures;
    }
    
    /**
     * For the initial Ride, where there is no previous ride.
     * Or more generally, for boarding at any time.
     * Assumes random arrival time during the day.
     */
    public Stats statsForBoarding(Ride ride) {
        Stats stats = new Stats ();
        stats.min = 0;
        int[] departures = getSortedDepartures(ride);
        int last = -1;
        double avgAccumulated = 0.0;
        for (int dep : departures) {
            if (last > 0) {
                int maxWait = dep - last;
                if (maxWait > stats.max) stats.max = maxWait;
                /* Weight the average of each interval by the number of seconds it contains. */
                avgAccumulated += (maxWait / 2.0) * maxWait; 
                stats.num += maxWait;
            }
            last = dep;
            continue;
        }
        stats.avg = (int) (avgAccumulated / stats.num);        
        return stats;
    }

    /**
     * Calculates Stats for the transfer to the given ride from the previous ride. 
     * This should only be called after all PatternRides have been added to the ride.
     * 
     * Distances can be stored in rides, including the first and last distance. But waits must be
     * calculated from full sets of patterns, which are not known until a round is over.
     */
//    public Stats statsForTransfer (Ride ride) {
//        Ride prev = ride.previous;
//        /* If there is no previous ride, use a different method. */
//        if (prev == null) return statsForBoarding (ride); 
//        int[] departures = getSortedDepartures(ride);
//        int[] arrivals   = getSortedArrivals(prev);
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
    
}
