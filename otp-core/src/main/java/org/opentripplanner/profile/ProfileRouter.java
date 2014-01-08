package org.opentripplanner.profile;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.profile.ProfileData.StopAtDistance;
import org.opentripplanner.profile.ProfileData.Transfer;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Sets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

@RequiredArgsConstructor
public class ProfileRouter {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileRouter.class);
    private static Stop fakeTargetStop = new Stop();
    static {
        fakeTargetStop.setId(new AgencyAndId("FAKE", "TARGET"));
    }
    @NonNull ProfileData data;
    Multimap<Stop, Ride> rides = ArrayListMultimap.create();
    Map<TableTripPattern, StopAtDistance> fromStops, toStops;
    Set<Ride> targetRides = Sets.newHashSet();
    
    public static class Stats implements Cloneable {
        @Getter int min = 0;
        @Getter int avg = 0;
        @Getter int max = 0;
        @Getter int num = 0;
        
        public Stats () { }
        public Stats (Stats other) {
            if (other != null) {
                this.min = other.min;
                this.avg = other.avg;
                this.max = other.max;
            }
        }
        public Stats (TableTripPattern pattern, int hop0, int hop1) {
            min = Integer.MAX_VALUE;
            num = 0;
            for (TripTimes tripTimes : pattern.getScheduledTimetable().getTripTimes()) {
                int depart = tripTimes.getDepartureTime(hop0);
                int arrive = tripTimes.getArrivalTime(hop1);
                int t = arrive - depart;
                if (t < min) min = t;
                if (t > max) max = t;
                avg += t;            
                ++num;
            }
            avg /= num;
        }        
        public void add(Stats s) {
            min += s.min;
            max += s.max;
            avg += (avg + s.avg) / 2; // TODO rethink
            num = 0; // it's poorly defined here
        }
        public void add(int x) {
            min += x;
            avg += x;
            max += x;
            num = 0; // it's poorly defined here
        }
        public void merge (Stats other) {
            if (other.min < min) min = other.min;
            if (other.max > max) max = other.max;
            avg = (avg * num + other.avg * other.num) / (num + other.num); // TODO should be float math
        }
        /** Build a composite Stats out of a bunch of other Stats. */
        public Stats (Collection<Stats> stats) {
            min = Integer.MAX_VALUE;
            num = 0;
            for (Stats other : stats) {
                if (other.min < min) min = other.min;
                if (other.max > max) max = other.max;
                avg += other.avg * other.num;
                num += other.num;
            }
            avg /= num; // TODO should perhaps be float math
        }
        public void dump() {
            System.out.printf("min %d avg %d max %d\n", min, avg, max);
        }
    }
    
    /** 
     * A ride on a single pattern, one or more of which are included in a Ride.
     * The distinction is made because stop indexes may be different on every pattern of a route.
     * 
     * When a ride is unfinished (waiting in the queue) its toIndex is -1 and stats is null.
     * 
     * Hash code and equals allow adding the same PatternRide to a set multiple times 
     * coming from different transfers.
     */
    @EqualsAndHashCode(exclude="xfer")
    public static class PatternRide {
        TableTripPattern pattern; // uses identity hash code
        int fromIndex;
        int toIndex = -1;
        Ride previous;
        Transfer xfer; // how did we get here
        Stats stats = null;
        public PatternRide (TableTripPattern pattern, int fromIndex, Ride previous, Transfer xfer) {
            this.pattern   = pattern;
            this.fromIndex = fromIndex;
            this.previous  = previous;
            this.xfer      = xfer;
        }
        /* Complete an unfinished ride as a new object. */
        public PatternRide extendToIndex(int toIndex) {
            /* Copy most fields from this unfinished ride. */
            PatternRide ret = new PatternRide(pattern, fromIndex, previous, xfer);
            /* Set the other fields to complete the ride. */
            ret.toIndex = toIndex;
            /* TODO: we do not need to save these stats, this can be a method. */
            ret.stats = new Stats(ret.pattern, ret.fromIndex, ret.toIndex - 1); 
            return ret;
        }
        public Stop getFromStop() {
            return pattern.getStops().get(fromIndex);
        }
        public Stop getToStop() {
            return pattern.getStops().get(toIndex);            
        }
        public boolean finished () {
            return toIndex >= 0 && stats != null;
        }
        public String toString () {
            return String.format("%s from %d, prev is %s", pattern.getCode(), fromIndex, previous);
        }
    }
    
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
    public static class Ride {

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

        public boolean containsPattern(TableTripPattern pattern) {
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
        
    }

    private boolean pathContainsRoute(Ride ride, Route route) {
        while (ride != null) {
            if (ride.route == route) return true;
            ride = ride.previous;
        }
        return false;
    }

    private boolean pathContainsStop(Ride ride, Stop stop) {
        while (ride != null) {
            if (ride.from == stop) return true;
            if (ride.to   == stop) return true;
            ride = ride.previous;
        }
        return false;
    }
    
    /**
     * Adds a new PatternRide to the PatternRide's destination stop. 
     * If a Ride already exists there on the same route, and with the same previous Ride, 
     * then the new PatternRide is grouped into that existing Ride. Otherwise a new Ride is created.
     * @return the resulting Ride object, whether it was new or and existing one we merged into. 
     */
    private Ride addRide (PatternRide pr) {
        //LOG.info("new patternride: {}", pr);
        /* Check if the new PatternRide merges into an existing Ride. */
        for (Ride ride : rides.get(pr.getToStop())) {
            if (ride.previous == pr.previous && 
                ride.route    == pr.pattern.route &&
                ride.to       == pr.getToStop()) { // FIXME: how could it not be equal? To-stops should always match in our rides collections
                ride.patternRides.add(pr);
                return ride;
            }
        }
        /* The new ride does not merge into any existing one. */
        Ride ride = new Ride(pr);
        rides.put(pr.getToStop(), ride);
        return ride;
    }

    /**
     * @return true if the given stop has at least one transfer from the given pattern.
     */
    private boolean hasTransfers(Stop stop, TableTripPattern pattern) {
        for (Transfer tr : data.transfersForStop.get(stop)) {
            if (tr.tp1 == pattern) return true;
        }
        return false;
    }
    
    /* Don't even try to accumulate stats and weights on the fly, just enumerate options. */
    
    /**
     * Scan through this pattern, creating rides to all downstream stops that have relevant transfers.
     */
    private void makeRides(PatternRide pr, Multimap<Stop, Ride> rides) {
        List<Stop> stops = pr.pattern.getStops();
        Stop targetStop = null;
        if (toStops.containsKey(pr.pattern)) {
            /* This pattern has a stop near the destination. Retrieve it. */
            targetStop = toStops.get(pr.pattern).stop; 
        }
        for (int s = pr.fromIndex; s < stops.size(); ++s) {
            Stop stop = stops.get(s);
            if (targetStop != null && targetStop == stop) {
                targetRides.add(addRide(pr.extendToIndex(s)));
            } else if (hasTransfers(stop, pr.pattern)) {
                if ( ! pathContainsStop(pr.previous, stop)) {
                    addRide(pr.extendToIndex(s));
                }
            }
        }
    }
    
    public Collection<Option> route (double fromLat, double fromLon, double toLat, double toLon) {
        /* Set to 2 until we have better pruning. There are a lot of 3-combinations. */
        final int ROUNDS = 2;
        int finalRound = ROUNDS - 1;
        int penultimateRound = ROUNDS - 2;
        fromStops = data.closestPatterns(fromLon, fromLat);
        toStops =   data.closestPatterns(toLon, toLat);
        LOG.info("from stops: {}", fromStops);
        LOG.info("to stops: {}", toStops);
        /* Our work queue is actually a set, because transferring from a group of patterns
         * can generate the same PatternRide many times. FIXME */
        Set<PatternRide> queue = Sets.newHashSet();
        /* Enqueue one or more QRides for each pattern/stop near the origin. */
        for (Entry<TableTripPattern, StopAtDistance> entry : fromStops.entrySet()) {
            TableTripPattern pattern = entry.getKey();
            StopAtDistance sd = entry.getValue();
            for (int i = 0; i < pattern.getStops().size(); ++i) {
                if (pattern.getStops().get(i) == sd.stop) {
                    /* Pseudo-transfer from null indicates first leg. */
                    Transfer xfer = new Transfer(null, pattern, null, sd.stop, sd.distance);
                    queue.add(new PatternRide(pattern, i, null, xfer));
                    /* Do not break in case stop appears more than once in the same pattern. */
                }
            }
        }
        /* One round per ride, as in RAPTOR. */
        for (int round = 0; round < ROUNDS; ++round) {
            LOG.info("ROUND {}", round);
            for (PatternRide pr : queue) {
                makeRides(pr, rides);
            }
            LOG.info("number of rides: {}", rides.size());
            /* Check rides reaching the targets */
//            Set<Stop> uniqueStops = Sets.newHashSet();
//            for (StopAtDistance sad : toStops.values()) {
//                uniqueStops.add(sad.stop);
//            }
//            for (Entry<Pattern, StopAtDistance> entry : toStops.entrySet()) {
//                for (Ride ride : rides.get(entry.getValue().stop)) {
//                    if (ride.patterns.contains(entry.getKey())) ride.dump();
//                }
//            }
            /* Build a new queue for the next round by transferring from patterns in rides */
            if (round != finalRound) {
                queue.clear();
                /* Rides is cleared at the end of each round. */
                for (Ride ride : rides.values()) {
                    // LOG.info("RIDE {}", ride);
                    for (Transfer tr : data.transfersForStop.get(ride.to)) {
                        // LOG.info("  TRANSFER {}", tr);
                        if (round == penultimateRound && !toStops.containsKey(tr.tp2)) continue;
                        if (ride.containsPattern(tr.tp1)) {
                            if (pathContainsRoute(ride, tr.tp2.route)) continue;
                            if (tr.s1 != tr.s2 && pathContainsStop(ride, tr.s2)) continue;
                            // enqueue transfer result state
                            for (int i = 0; i < tr.tp2.getStops().size(); ++i) {
                                if (tr.tp2.getStops().get(i) == tr.s2) {
                                    queue.add(new PatternRide(tr.tp2, i, ride, tr));
                                    /* Do not break, stop can appear in pattern more than once. */
                                }
                            }
                        }
                    }
                }
                LOG.info("number of new queue states: {}", queue.size());
                rides.clear();
            }
        }
        List<Option> options = Lists.newArrayList();
        for (Ride ride : targetRides) {
            /* We alight from all patterns in a ride at the same stop. */
            int dist = toStops.get(ride.patternRides.get(0).pattern).distance; 
            options.add(new Option (ride, dist)); // TODO Convert distance to time.
        }
        return options;
    }
    
}
