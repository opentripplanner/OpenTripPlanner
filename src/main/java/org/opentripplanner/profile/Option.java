package org.opentripplanner.profile;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.opentripplanner.index.model.RouteShort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This is a response model class which holds data that will be serialized and returned to the client.
 * It is not used internally in routing.
 */
public class Option {

    private static final Logger LOG = LoggerFactory.getLogger(Option.class);

    public List<Segment> transit;
    public List<StreetSegment> access;
    public List<StreetSegment> egress;
    public Stats stats = new Stats();
    public String summary;
    public List<DCFareCalculator.Fare> fares;
    // The fares are outside the transit segments because a fare can apply to multiple segments so there is no one-to-one
    // correspondance. For example, when you transfer from one subway to another and pay one fare for the two segments.

    public Option (Ride tail, Collection<StopAtDistance> accessPaths, Collection<StopAtDistance> egressPaths) {
        access = StreetSegment.list(accessPaths);
        egress = StreetSegment.list(egressPaths);
        // Include access and egress times across all modes in the overall travel time statistics for this option.
        // FIXME In the event that there is only access, N will still be 1 which is strange.
        stats.add(access);
        stats.add(egress);
        List<Ride> rides = Lists.newArrayList();
        // Chase back-pointers to get a reversed sequence of rides
        for (Ride ride = tail; ride != null; ride = ride.previous) {
            rides.add(ride);
        }
        if ( ! rides.isEmpty()) {
            Collections.reverse(rides);
            // The access times have already been calculated separately, avoid double-inclusion by zeroing them out here
            rides.get(0).accessStats = new Stats();
            rides.get(0).accessDist = 0;
            // Make a transit segment for each ride in order
            transit = Lists.newArrayList();
            for (Ride ride : rides) {
                Segment segment = new Segment(ride);
                transit.add(segment);
                stats.add(segment.walkTime);
                if(segment.waitStats != null) stats.add(segment.waitStats);
                stats.add(segment.rideStats);
            }
        }
        // Really should be one per segment, with transfers to the same operator having a price of 0.
        fares = DCFareCalculator.calculateFares(rides);
        summary = generateSummary();
    }

    /** Make a human readable text summary of this option. */
    public String generateSummary() {
        if (transit == null || transit.isEmpty()) {
            return "Non-transit options";
        }
        List<String> vias = Lists.newArrayList();
        List<String> routes = Lists.newArrayList();
        for (Segment segment : transit) {
            List<String> routeShortNames = Lists.newArrayList();
            for (RouteShort rs : segment.routes) {
                String routeName = rs.shortName == null ? rs.longName : rs.shortName;
                routeShortNames.add(routeName);
            }
            routes.add(Joiner.on("/").join(routeShortNames));
            vias.add(segment.toName);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("routes ");
        sb.append(Joiner.on(", ").join(routes));
        if (!vias.isEmpty()) vias.remove(vias.size() - 1);
        if (!vias.isEmpty()) {
            sb.append(" via ");
            sb.append(Joiner.on(", ").join(vias));
        }
        return sb.toString();
    }

    public static enum SortOrder {
        MIN, AVG, MAX;
    }

    public static class MinComparator implements Comparator<Option> {
        @Override
        public int compare(Option one, Option two) {
            return one.stats.min - two.stats.min;
        }
    }

    public static class AvgComparator implements Comparator<Option> {
        @Override
        public int compare(Option one, Option two) {
            return one.stats.avg - two.stats.avg;
        }
    }

    public static class MaxComparator implements Comparator<Option> {
        @Override
        public int compare(Option one, Option two) {
            return one.stats.max - two.stats.max;
        }
    }

    /**
     * Rides or transfers may contain no patterns after applying time window.
     * Return true if this Option contains any transit rides that contain zero active patterns.
     */
    public boolean hasEmptyRides() {
        for (Segment seg : transit) {
            if (seg.rideStats.num == 0 || seg.waitStats.num == 0) {
                return true;
            }
        }
        return false;
    }

}
