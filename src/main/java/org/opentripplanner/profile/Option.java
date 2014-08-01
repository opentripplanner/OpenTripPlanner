package org.opentripplanner.profile;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Option {

    public List<Segment> segments = Lists.newArrayList();
    public List<StreetSegment> access;
    public List<StreetSegment> egress;
    public Stats stats;
    public String summary;
    public List<WalkStep> walkSteps;
    public List<DCFareCalculator.Fare> fares;

    public Option (Ride tail, Collection<StopAtDistance> accessPaths, Collection<StopAtDistance> egressPaths) {
        stats = new Stats();
        List<Ride> rides = Lists.newArrayList();
        for (Ride ride = tail; ride != null; ride = ride.previous) {
            rides.add(ride);
        }
        Collections.reverse(rides);
        access = StreetSegment.list(accessPaths);
        egress = StreetSegment.list(egressPaths);
        for (Ride ride : rides) {
            Segment segment = new Segment(ride);
            segments.add(segment);
            stats.add(segment.walkTime);
            if(segment.waitStats != null) stats.add(segment.waitStats);
            stats.add(segment.rideStats);
        }
        // Really should be one per segment, with transfers to the same operator having a price of 0.
        fares = null; //DCFareCalculator.calculateFares(rides);
        // TODO stats.add(finalWalkTime);
        //summary = generateSegmentSummary();
    }


    /** A constructor for an option that includes only a street mode, not transit. */
    public Option (State state) {
        stats = new Stats();
        int time = (int) state.getElapsedTimeSeconds();
        stats.add(time);
        // this might not work if there is a transition to another mode
        TraverseMode mode = state.getNonTransitMode();
        summary = mode.toString();
        access = Lists.newArrayList(new StreetSegment(state));
    }

    public String generateSegmentSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("routes ");
        List<String> routeShortNames = Lists.newArrayList();
        List<String> vias = Lists.newArrayList();
        for (Segment segment : segments) {
            String routeName = segment.routeShortName == null 
                    ? segment.routeLongName : segment.routeShortName;
            routeShortNames.add(routeName);
            vias.add(segment.toName);
        }
        if (!vias.isEmpty()) vias.remove(vias.size() - 1);
        sb.append(Joiner.on(", ").join(routeShortNames));
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
     */
    public boolean hasEmptyRides() {
        for (Segment seg : segments) {
            if (seg.rideStats.num == 0 || seg.waitStats.num == 0) {
                return true;
            }
        }
        return false;
    }

}
