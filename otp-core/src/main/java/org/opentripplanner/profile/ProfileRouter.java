package org.opentripplanner.profile;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.profile.ProfileData.Pattern;
import org.opentripplanner.profile.ProfileData.StopAtDistance;
import org.opentripplanner.profile.ProfileData.Transfer;
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
    Map<Pattern, StopAtDistance> fromStops, toStops;
    Set<Ride> targetRides = Sets.newHashSet();
    
    public static class Stats {
        int min = 0;
        int avg = 0;
        int max = 0;
        public Stats (Stats other) {
            this.min = other.min;
            this.avg = other.avg;
            this.max = other.max;
        }
    }
    
    @RequiredArgsConstructor
    public static class State {
        final Pattern ptp;
        final Stop stop;
        final State back;
    }
    
    @AllArgsConstructor
    public static class QRide {
        Stop from;
        Pattern pattern;
        Ride previous;
    }
    
    public static class Ride {
        @Getter Stop from;
        @Getter Stop to;
        @Getter Route route;
        List<Pattern> patterns = Lists.newArrayList();
        Ride previous;
        public Ride (QRide qr, Stop to) {
            this.from = qr.from;
            this.route = qr.pattern.route;
            this.previous = qr.previous;
            this.patterns.add(qr.pattern);
            this.to = to;
        }
        public String toStringVerbose() {
            return String.format(
                "ride route %s from %s to %s (%d patterns)",
                route.getLongName(), from.getName(), to.getName(), patterns.size()
            );
        }
        public String toString() {
            return String.format(
                "route %3s from %4s to %4s (%d)",
                route.getShortName(), from.getCode(), to.getCode(), patterns.size()
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
    }

    public static class Round {
        
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

    /** @return the ride object (whether it was new or and existing one we merged into) */
    private Ride addRide (QRide qr, Stop stop) {
        for (Ride ride : rides.get(stop)) {
            if (ride.previous == qr.previous && 
                ride.route == qr.pattern.route &&
                ride.to == stop) { 
                // to-stops should always match in our rides collections
                /* The new ride merges into an existing one. */
                ride.patterns.add(qr.pattern);
                return ride;
            }
        }
        /* The new ride does not merge into any existing one. */
        Ride ride = new Ride(qr, stop);
        rides.put(stop, ride);
        return ride;
    }

    private boolean hasTransfers(Stop stop, Pattern pattern) {
        for (Transfer tr : data.transfersForStop.get(stop)) {
            if (tr.tp1 == pattern) {
                return true;
            }
        }
        return false;
    }

    /* don't even try to accumulate numbers on the fly, just enumerate options. */
    /**
     * Scan through this pattern, creating rides to all downstream stops that have relevant transfers.
     */
    private void makeRides(QRide qr, Multimap<Stop, Ride> rides) {
        List<Stop> stops = qr.pattern.stops;
        Stop targetStop = null;
        if (toStops.containsKey(qr.pattern)) {
            targetStop = toStops.get(qr.pattern).stop; // this pattern has a stop near the destination
        }
        boolean boarded = false;
        for (int s = 0; s < stops.size(); ++s) {
            Stop stop = stops.get(s);
            if (!boarded) {
                if (stop == qr.from) boarded = true;
                continue;
            }
            if (targetStop != null && targetStop == stop) {
                targetRides.add(addRide(qr, stop));
            } else if (hasTransfers(stop, qr.pattern)) {
                if ( ! pathContainsStop(qr.previous, stop)) addRide(qr, stop);
            }
        }
    }
    
    public Iterable<Ride> route (double fromLat, double fromLon, double toLat, double toLon) {
        final int ROUNDS = 2;
        int finalRound = ROUNDS - 1;
        int penultimateRound = ROUNDS - 2;
        fromStops = data.closestPatterns(fromLon, fromLat);
        toStops =   data.closestPatterns(toLon, toLat);
        LOG.info("from stops: {}", fromStops);
        LOG.info("to stops: {}", toStops);
        List<QRide> queue = Lists.newArrayList();
        for (Entry<Pattern, StopAtDistance> entry : fromStops.entrySet()) {
            queue.add(new QRide(entry.getValue().stop, entry.getKey(), null));
        }
        for (int round = 0; round < ROUNDS; ++round) {
            LOG.info("ROUND {}", round);
            for (QRide qr : queue) {
                makeRides(qr, rides);
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
            /* build a new queue for the next round by transferring from patterns in rides */
            if (round != finalRound) {
                queue.clear();
                for (Ride ride : rides.values()) {
                    //LOG.info("{}", ride);
                    for (Transfer tr : data.transfersForStop.get(ride.to)) {
                        if (round == penultimateRound && !toStops.containsKey(tr.tp2)) continue;
                        if (ride.patterns.contains(tr.tp1)) {
                            if (pathContainsRoute(ride, tr.tp2.route)) continue;
                            if (tr.s1 != tr.s2 && pathContainsStop(ride, tr.s2)) continue;
                            queue.add(new QRide(tr.s2, tr.tp2, ride));
                        }
                    }
                }
                LOG.info("number of new queue states: {}", queue.size());
                rides.clear();
            }
        }
        return targetRides;
    }
    
}
