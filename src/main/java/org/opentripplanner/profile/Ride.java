package org.opentripplanner.profile;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Lists;


/** 
 * A Ride is defined by its from-stop and to-stop, as well as the previous ride (from which it transfers).
 * It serves as a container for multiple PatternRides, all of which connect that same pair of Stops.
 * A Ride may be unfinished, which means it does not yet have a destination and contains only unfinished PatternRides.
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
    final StopCluster from;
    final StopCluster to;
    final Ride previous;
    final List<PatternRide> patternRides = Lists.newArrayList();
    Stats rideStats; // filled in only once the ride is complete (has all PatternRides).
    Stats waitStats; // filled in only once the ride is complete (has all PatternRides).
    int accessTime;  // minimum time to reach this ride from the previous one, or from the origin point on the first ride
    int accessDist;  // meters from the previous ride, or from the origin point on the first ride

    public Ride (StopCluster from, Ride previous) {
        this.from = from;
        this.to = null; // this is a "partial ride" waiting to be completed.
        this.previous = previous;
    }

    /** Construct a partial copy with no PatternRides or Stats and the given arrival StopCluster. */
    public Ride (Ride other, StopCluster to) {
        this.from = other.from;
        this.to = to;
        this.previous = other.previous;
        this.accessTime = other.accessTime;
        this.accessDist = other.accessDist;
    }

    /** Extend this incomplete ride to the given stop, creating a container for PatternRides. */
    public Ride extendTo(StopCluster toStopCluster) {
        return new Ride(this, toStopCluster);
    }

    public String toString() {
        return String.format("Ride from %s to %s (%d patterns on routes %s)", from, to, patternRides.size(), getRoutes());
    }

    public void dump() {
        List<Ride> rides = Lists.newLinkedList();
        Ride ride = this;
        while (ride != null) {
            rides.add(0, ride);
            ride = ride.previous;
        }
        LOG.info("Path from {} to {}", rides.get(0).from, rides.get(rides.size() - 1).to);
        for (Ride r : rides) LOG.info("  {}", r.toString());
    }

    public Multimap<Route, PatternRide> getPatternRidesByRoute() {
        Multimap<Route, PatternRide> ret = HashMultimap.create();
        for (PatternRide pr : patternRides) ret.put(pr.pattern.route, pr);
        return ret;
    }

    public Set<Route> getRoutes() {
        Set<Route> routes = Sets.newHashSet();
        for (PatternRide ride : patternRides) routes.add(ride.pattern.route);
        return routes;
    }

    public boolean containsPattern(TripPattern pattern) {
        for (PatternRide patternRide : patternRides) {
            if (patternRide.pattern == pattern) return true;
        }
        return false;
    }

    public int pathLength() {
        int length = 0;
        Ride ride = this;
        while (ride != null) {
            length += 1;
            ride = ride.previous;
        }
        return length;
    }

    public boolean pathContainsRoute(Route route) {
        // Linear search, could use sets if this proves to be time consuming
        Ride ride = this;
        while (ride != null) {
            for (PatternRide pr : patternRides) {
                if (pr.pattern.route == route) return true;
            }
            ride = ride.previous;
        }
        return false;
    }

    // TODO rename _cluster_
    public boolean pathContainsStop(StopCluster stopCluster) {
        Ride ride = this;
        while (ride != null) {
            if (ride.from == stopCluster || ride.to == stopCluster) return true;
            ride = ride.previous;
        }
        return false;
    }

    /** Return a lower bound on the duration of this option. */
    public int durationLowerBound() {
        int ret = 0;
        Ride ride = this;
        while (ride != null) {
            ret += ride.rideStats.min;
            ret += ride.waitStats.min;
            ret += ride.accessTime; // minimum access time to first ride, or transfer walking time on subsequent ones.
            ride = ride.previous;
        }
        return ret;
    }

    /** Return an upper bound on the duration of this option. */
    public int durationUpperBound() {
        int ret = 0;
        Ride ride = this;
        while (ride != null) {
            ret += ride.rideStats.max;
            ret += ride.waitStats.max;
            ret += ride.accessTime; // FIXME using fastest access mode when calculating upper bound time... is this OK?
            ride = ride.previous;
        }
        return ret;
    }

    /**
     * Create a compound Stats for all the constituent PatternRides of this Ride.
     * This should not be called until all PatternRides have been added to this Ride.
     */
    public void calcStats(TimeWindow window, double walkSpeed) {
        /* Stats for the ride on transit. */
        List<Stats> stats = Lists.newArrayList();
        for (PatternRide patternRide : patternRides) {
            stats.add(patternRide.stats);
        }
        this.rideStats = new Stats(stats);
        /* Stats for the wait between the last ride and this one, NOT including walk time. */
        this.waitStats = this.calcStatsForTransfer(window, walkSpeed);
    }

    /* Maybe store transfer distances by stop pair, and look them up. */
    /**
     * @param arrivals find arrival times rather than departure times for this Ride.
     * @return a list of sorted departure or arrival times within the window.
     * FIXME this is a hot spot in execution, about 50 percent of runtime.
     */
    public List<Integer> getSortedStoptimes (TimeWindow window, boolean arrivals) {
        // Using Lists because we don't know the length in advance
        List<Integer> times = Lists.newArrayList();
        // TODO include: non-frequency trips; exact-times frequency trips
        // non-exact (headway-based) frequency trips will be handled elsewhere since they don't have specific boarding times.
        for (PatternRide patternRide : patternRides) {
            for (TripTimes tt : patternRide.pattern.scheduledTimetable.tripTimes) {
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
    public Stats calcStatsForTransfer (TimeWindow window, double walkSpeed) {
        /* If there is no previous ride, assume uniformly distributed arrival times. */
        if (previous == null) return this.statsForBoarding (window); 
        List<Integer> arrivals = previous.getSortedStoptimes(window, true);
        List<Integer> departures = this.getSortedStoptimes(window, false);
        List<Integer> waits = Lists.newArrayList();
        Iterator<Integer> departureIterator = departures.iterator(); 
        int departure = departureIterator.next();
        ARRIVAL : for (int arrival : arrivals) {
            int boardTime = arrival + accessTime + ProfileRouter.SLACK;
            while (departure <= boardTime) {
                if (!departureIterator.hasNext()) break ARRIVAL;
                departure = departureIterator.next();
            }
            waits.add(departure - boardTime);
        }
        /* Waits list may be empty if no transfers are possible. */
        if (waits.isEmpty()) return null; // Impossible to make this transfer.
        return new Stats (waits);
    }

    /**  @return the stop at which the rider would board the chain of Rides this Ride belongs to. */
    public StopCluster getAccessStopCluster() {
        Ride ride = this;
        while (ride.previous != null) {
            ride = ride.previous;
        }
        return ride.from;
    }


    /** @return the stop from which the rider will walk to the final destination, assuming this is the final Ride in a chain. */
    public StopCluster getEgressStopCluster() {
        return this.to;
    }

}
