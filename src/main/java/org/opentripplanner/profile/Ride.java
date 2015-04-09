package org.opentripplanner.profile;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


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
    Stats rideStats;   // filled in only once the ride is complete (has all PatternRides).
    Stats waitStats;   // filled in only once the ride is complete (has all PatternRides).
    Stats accessStats; // min and max time to reach this ride from the previous one, or from the origin point on the first ride
    int accessDist;    // meters from the previous ride, or from the origin point on the first ride

    int dlb = 0;
    int dub = Integer.MAX_VALUE;
    int pathLength = 0;

    /** Construct a partial ride (resulting from a transfer, waiting to be completed). */
    // TODO add additional parameter for access stats, then recalc bounds in the constructor.
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
        this.accessStats = other.accessStats;
        this.accessDist = other.accessDist;
        // Bounds will be recomputed when stats are computed.
    }

    /** Extend this incomplete ride to the given stop, creating a container for PatternRides. */
    public Ride extendTo(StopCluster toStopCluster) {
        return new Ride(this, toStopCluster);
    }

    public String toString() {
        return String.format("Ride from %s to %s (%d patterns on routes %s)", from, to, patternRides.size(), getRoutes());
    }

    /** Output this entire chain of rides. */
    public void dumpRideChain() {
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

    /**
     * Calculate length and upper and lower bounds on duration for the chain of rides ending with this one.
     * This should be called whenever the ride/wait/access stats for the ride are updated.
     * We can't call it in the constructor or update methods because access stats are set after construction,
     * and wait stats are sometimes null.
     */
    public void recomputeBounds() {
        dlb = 0;
        dub = 0;
        pathLength = 0;
        Ride ride = this;
        if (ride.to == null) {
            // This is an unfinished ride that just came off the queue.
            // It is the result of a transfer. It has access time, but not wait or ride time yet.
            dlb += ride.accessStats.min;
            dub += ride.accessStats.max;
            ride = ride.previous;
        }
        while (ride != null) {
            pathLength += 1;
            dlb += ride.rideStats.min;
            dlb += ride.waitStats.min;
            dlb += ride.accessStats.min;
            dub += ride.rideStats.max;
            dub += ride.waitStats.max;
            dub += ride.accessStats.max;
            ride = ride.previous;
        }
    }

    /**
     * Create a compound Stats for all the constituent PatternRides of this Ride.
     * This should not be called until all PatternRides have been added to this Ride.
     * There are two separate Stats objects: The rideStats includes the time spent on the patterns themselves.
     * The waitStats capture the time spent waiting to board those patterns (transfer or initial boarding).
     */
    public void calcStats(TimeWindow window, double walkSpeed) {
        /* Stats for the ride on transit. */
        List<Stats> stats = Lists.newArrayList();
        for (PatternRide patternRide : patternRides) {
            stats.add(patternRide.stats);
        }
        rideStats = new Stats(stats);
        /* Stats for the wait between the last ride and this one, NOT including walk time. */
        waitStats = calcStatsForFreqs(window);
        // Only try schedule-based boarding if there were no non-exact frequency entries.
        // FIXME there is an assumption here that there are only frequency or non-frequency entries in a PatternRide
        if (waitStats == null) {
            if (previous == null) {
                // If there is no previous ride, assume uniformly distributed arrival times.
                waitStats = calcStatsForBoarding(window);
            } else {
                // There is a previous ride, so account for arrival and departure times before and after the transfer.
                waitStats = calcStatsForTransfer(window, walkSpeed);
            }
        }
    }

    /* Maybe store transfer distances by stop pair, and look them up. */
    /**
     * @param arrivals find arrival times rather than departure times for this Ride.
     * @return a list of sorted departure or arrival times within the window.
     * FIXME this is a hot spot in execution, about 50 percent of runtime.
     */
    public TIntList getSortedStoptimes (TimeWindow window, boolean arrivals) {
        // Using Lists because we don't know the length in advance
        TIntList times = new TIntArrayList();
        // TODO include exact-times frequency trips along with non-frequency trips
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
        times.sort();
        return times;
    }

    /** Calculate the wait time stats for boarding all (non-exact) frequency entries in this Ride. */
    private Stats calcStatsForFreqs(TimeWindow window) {
        Stats stats = new Stats(); // all stats fields are initialized to zero
        stats.num = 0; // the total number of seconds that headway boarding is possible
        for (PatternRide patternRide : patternRides) {
            for (FrequencyEntry freq : patternRide.pattern.scheduledTimetable.frequencyEntries) {
                if (freq.exactTimes) {
                    LOG.error("Exact times not yet supported in profile routing.");
                    return null;
                }
                int overlap = window.overlap(freq.startTime, freq.endTime, freq.tripTimes.serviceCode);
                if (overlap > 0) {
                    if (freq.headway > stats.max) stats.max = freq.headway;
                    // weight the average of each headway by the number of seconds it is valid
                    stats.avg += (freq.headway / 2) * overlap;
                    stats.num += overlap;
                }
            }
        }
        if (stats.num == 0) return null;
        /* Some frequency entries were added to the stats. */
        stats.avg /= stats.num;
        return stats;
    }

    /**
     * Produce stats about boarding an initial Ride, which has no previous ride.
     * This assumes arrival times are uniformly distributed during the window.
     * The Ride must contain some trips, and the window must have a positive duration.
     */
    public Stats calcStatsForBoarding(TimeWindow window) {
        Stats stats = new Stats ();
        stats.min = 0; // You can always arrive just before a train departs.
        TIntList departures = getSortedStoptimes(window, false);
        int last = window.from;
        double avgAccumulated = 0.0;
        /* All departures in the list are known to be running and within the window. */
        for (TIntIterator it = departures.iterator(); it.hasNext();) {
            int dep = it.next();
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
        TIntList arrivals = previous.getSortedStoptimes(window, true);
        TIntList departures = this.getSortedStoptimes(window, false);
        List<Integer> waits = Lists.newArrayList();
        TIntIterator departureIterator = departures.iterator();
        int departure = departureIterator.next();
        ARRIVAL : for (TIntIterator arrivalsIterator = arrivals.iterator(); arrivalsIterator.hasNext();) {
            int arrival = arrivalsIterator.next();
            // On transfers the access stats should have max=min=avg
            // We use the min, which would be best if min != max since it should only relax the bounds somewhat.
            int boardTime = arrival + accessStats.min + ProfileRouter.SLACK;
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
