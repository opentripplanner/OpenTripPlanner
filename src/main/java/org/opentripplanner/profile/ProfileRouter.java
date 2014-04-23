package org.opentripplanner.profile;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.joda.time.LocalDate;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.param.LatLon;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.GraphIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class ProfileRouter {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileRouter.class);
    static final double WALK_SPEED = 1.4; // m/sec
    static final double WALK_DISTANCE = 500; // meters
    static final int SLACK = 60; // sec
    static final Stop fakeTargetStop = new Stop();
    
    static {
        fakeTargetStop.setId(new AgencyAndId("FAKE", "TARGET"));
    }

    private GraphIndex index;

    public ProfileRouter(GraphIndex index) {
        this.index = index;
    }

    Multimap<Stop, Ride> rides = ArrayListMultimap.create();
    Map<TripPattern, StopAtDistance> fromStops, toStops;
    Set<Ride> targetRides = Sets.newHashSet();
    TimeWindow window;

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
     * PatternRide may be null, indicating an empty PatternRide with no trips.
     * In this case no Ride is created or updated, and this method returns null.
     * A new set of rides is produced in each round.
     * @return the resulting Ride object, whether it was new or and existing one we merged into. 
     */
    // TODO: rename this
    private Ride addRide (PatternRide pr) {
        /* Catch empty PatternRides with no trips. */
        if (pr == null) return null;
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
    private boolean hasTransfers(Stop stop, TripPattern pattern) {
        for (ProfileTransfer tr : index.transfersForStop.get(stop)) {
            if (tr.tp1 == pattern) return true;
        }
        return false;
    }
    
    /* Maybe don't even try to accumulate stats and weights on the fly, just enumerate options. */
    
    /**
     * Complete a partial PatternRide (with only pattern and beginning stop) which was enqueued in the last round.
     * This is done by scanning through the Pattern, creating rides to all downstream stops that are near the
     * destination or have relevant transfers.
     */
    private void makeRides (PatternRide pr) {
        List<Stop> stops = pr.pattern.getStops();
        Stop targetStop = null;
        if (toStops.containsKey(pr.pattern)) {
            /* This pattern has a stop near the destination. Retrieve it. */
            targetStop = toStops.get(pr.pattern).stop; 
        }
        for (int s = pr.fromIndex + 1; s < stops.size(); ++s) {
            Stop stop = stops.get(s);
            if (targetStop != null && targetStop == stop) {
                Ride ride = addRide(pr.extendToIndex(s, window));
                if (ride != null) targetRides.add(ride);
                // Can we break out here if ride is null? How could later stops have trips?
            } else if (hasTransfers(stop, pr.pattern)) {
                if ( ! pathContainsStop(pr.previous, stop)) { // move this check outside the conditionals?
                    addRide(pr.extendToIndex(s, window)); // safely ignores empty PatternRides
                }
            }
        }
    }
    
    // TimeWindow could actually be resolved and created in the caller, which does have access to the profiledata.
    public ProfileResponse route (ProfileRequest req) {

        /* Set to 2 until we have better pruning. There are a lot of 3-combinations. */
        final int ROUNDS = 2;
        int finalRound = ROUNDS - 1;
        int penultimateRound = ROUNDS - 2;
        fromStops = index.closestPatterns(req.from.lon, req.from.lat, WALK_DISTANCE);
        toStops   = index.closestPatterns(req.to.lon, req.to.lat, WALK_DISTANCE);
        LOG.info("from stops: {}", fromStops);
        LOG.info("to stops: {}", toStops);
        this.window = new TimeWindow (req.fromTime, req.toTime, index.servicesRunning(req.date));
        /* Our per-round work queue is actually a set, because transferring from a group of patterns
         * can generate the same PatternRide many times. FIXME */
        Set<PatternRide> queue = Sets.newHashSet();
        /* Enqueue one or more PatternRides for each pattern/stop near the origin. */
        for (Entry<TripPattern, StopAtDistance> entry : fromStops.entrySet()) {
            TripPattern pattern = entry.getKey();
            StopAtDistance sd = entry.getValue();
            for (int i = 0; i < pattern.getStops().size(); ++i) {
                if (pattern.getStops().get(i) == sd.stop) {
                    /* Pseudo-transfer from null indicates first leg. */
                    ProfileTransfer xfer = new ProfileTransfer(null, pattern, null, sd.stop, sd.distance);
                    if (req.modes.contains(pattern.mode)) {
                        queue.add(new PatternRide(pattern, i, null, xfer));
                    }
                    /* Do not break in case stop appears more than once in the same pattern. */
                }
            }
        }
        /* One round per ride, as in RAPTOR. */
        for (int round = 0; round < ROUNDS; ++round) {
            LOG.info("ROUND {}", round);
            for (PatternRide pr : queue) {
                makeRides(pr);
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
                    for (ProfileTransfer tr : index.transfersForStop.get(ride.to)) {
                        // LOG.info("  TRANSFER {}", tr);
                        if (round == penultimateRound && !toStops.containsKey(tr.tp2)) continue;
                        if (ride.containsPattern(tr.tp1)) {
                            if (pathContainsRoute(ride, tr.tp2.route)) continue;
                            if (tr.s1 != tr.s2 && pathContainsStop(ride, tr.s2)) continue;
                            // enqueue transfer result state
                            for (int i = 0; i < tr.tp2.getStops().size(); ++i) {
                                if (tr.tp2.getStops().get(i) == tr.s2) {
                                    if (req.modes.contains(tr.tp2.mode)) {
                                        queue.add(new PatternRide(tr.tp2, i, ride, tr));
                                    }
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
            Option option = new Option (ride, dist, window); // TODO Convert distance to time.
            if ( ! option.hasEmptyRides()) options.add(option); 
        }
        return new ProfileResponse(options, req.orderBy, req.limit);
    }

}
