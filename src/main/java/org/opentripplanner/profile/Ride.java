package org.opentripplanner.profile;

import java.util.Collections;
import java.util.List;

import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Iterator;
import com.google.common.collect.Lists;


/** 
 * A ride from a specific stop to another, on one or more patterns that are all on the same route. 
 * Multi-pattern rides are formed by merging subsequent rides into existing ones.
 * 
 * TODO this could just be a RouteRide key mapped to lists of patternRides with the same characteristics.
 * its constructor would pull out the right fields.
 * 
 * TODO if patternRides can be added later, and stats and transfers are computed after the fact,
 * then maybe we no longer need a round-based approach.
 * We need to put _groups_ of patternRides into the queue, grouped by back ride and beginning stop.
 */
public class Ride {

    private static final Logger LOG = LoggerFactory.getLogger(Ride.class);

    /* Here we store stop objects rather than indexes. The start and end indexes in the Ride's 
     * constituent PatternRides should correspond to these same stops. */
    final Stop from;
    final Stop to;
    final Route route;
    final Ride previous;
    final List<PatternRide> patternRides = Lists.newArrayList();

    public Ride (PatternRide pr) {
        this.from     = pr.getFromStop();
        this.to       = pr.getToStop();
        this.route    = pr.pattern.route;
        this.previous = pr.previous;
        this.patternRides.add(pr);
    }
    
    public String toStringVerbose() {
        return String.format(
            "ride route %s from %s to %s (%d patterns)",
            route.getLongName(), from.getName(), to.getName(), patternRides.size()
        );
    }

    public String toString() {
        return String.format(
            "route %3s from %4s to %4s (%d)",
            route.getShortName(), from.getCode(), to.getCode(), patternRides.size()
        );
    }
    
    public void dump() {
        List<Ride> rides = Lists.newLinkedList();
        Ride ride = this;
        while (ride != null) {
            rides.add(0, ride);
            ride = ride.previous;
        }
        LOG.info("Path from {} to {}", rides.get(0).from, rides.get(rides.size() - 1).to);
        for (Ride r : rides) LOG.info("  {}", r);                
    }

    public boolean containsPattern(TripPattern pattern) {
        for (PatternRide patternRide : patternRides) {
            if (patternRide.pattern == pattern) return true;
        }
        return false;
    }

    /** 
     * All transfers are between the same stops, so of the same length.
     * @return the distance walked from the end of the previous ride to the beginning of this ride.
     */
    public double getTransferDistance() {
        return patternRides.get(0).xfer.distance;
    }

    /** Create Stats for all the constituent PatternRides of this Ride. */
    public Stats getStats() {
        List<Stats> stats = Lists.newArrayList();
        for (PatternRide patternRide : patternRides) {
            stats.add(patternRide.stats);
        }
        return new Stats(stats);
    }

    
    /* Maybe store transfer distances by stop pair, and look them up. */
    /**
     * @param arrivals find arrival times rather than departure times for this Ride.
     * @return a list of sorted departure or arrival times within the window.
     */
    public List<Integer> getSortedStoptimes (TimeWindow window, boolean arrivals) {
        // Using Lists because we don't know the length in advance
        List<Integer> times = Lists.newArrayList();
        for (PatternRide patternRide : patternRides) {
            for (TripTimes tt : patternRide.pattern.getScheduledTimetable().getTripTimes()) {
                if (window.servicesRunning.get(tt.serviceCode)) {
                    int t = arrivals ? tt.getArrivalTime(patternRide.toIndex)
                                     : tt.getDepartureTime(patternRide.fromIndex);
                    if (window.includes(t)) times.add(t);
                }
            }
        }
        Collections.sort(times);
        return times;
    }

    /**
     * Produce stats about boarding an initial Ride, which has no previous ride.
     * This assumes arrival times are uniformly distributed during the window.
     * The Ride must contain some trips, and the window must have a positive duration.
     */
    public Stats statsForBoarding(TimeWindow window) {
        Stats stats = new Stats ();
        stats.min = 0; // You can always arrive just before a train departs.
        List<Integer> departures = getSortedStoptimes(window, false);
        int last = window.from;
        double avgAccumulated = 0.0;
        /* All departures in the list are known to be running and within the window. */
        for (int dep : departures) {
            int maxWait = dep - last;
            if (maxWait > stats.max) stats.max = maxWait;
            /* Weight the average of each interval by the number of seconds it contains. */
            avgAccumulated += (maxWait / 2.0) * maxWait; 
            stats.num += maxWait;
            last = dep;
        }
        if (stats.num > 0) {
            stats.avg = (int) (avgAccumulated / stats.num);
        }
        return stats;
    }

    
    /**
     * Calculates Stats for the transfer to the given ride from the previous ride. 
     * This should only be called after all PatternRides have been added to the ride.
     * Distances can be stored in rides, including the first and last distance. But waits must be
     * calculated from full sets of patterns, which are not known until a round is over.
     */
    public Stats statsForTransfer (TimeWindow window, double walkSpeed) {
        /* If there is no previous ride, assume uniformly distributed arrival times. */
        if (previous == null) return this.statsForBoarding (window); 
        List<Integer> departures = getSortedStoptimes(window, false);
        List<Integer> arrivals   = getSortedStoptimes(window, true);
        int walkTime = (int) (getTransferDistance() / walkSpeed);
        List<Integer> waits = Lists.newArrayList();        
        Iterator<Integer> departureIterator = departures.iterator(); 
        int departure = departureIterator.next();
        ARRIVAL : for (int arrival : arrivals) {
            int boardTime = arrival + walkTime + ProfileRouter.SLACK;
            while (departure <= boardTime) {
                if (!departureIterator.hasNext()) break ARRIVAL;
                departure = departureIterator.next();
            }
            waits.add(departure - boardTime);
        }
        /* Waits list may be empty if no transfers are possible. Stats constructor handles this. */
        return new Stats (waits); 
    }

}
