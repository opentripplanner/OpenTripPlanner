package org.opentripplanner.profile;

import java.util.Comparator;
import java.util.List;

import lombok.Getter;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;

public class Option {

    @Getter List<Segment> segments = Lists.newLinkedList();
    @Getter int finalWalkTime;
    @Getter Stats stats;
    @Getter String summary;

    public Option (Ride ride, int finalWalkTime, TimeWindow window, double walkSpeed) {
        stats = new Stats();
        while (ride != null) {
            Segment segment = new Segment (ride, window, walkSpeed);
            segments.add(0, segment);
            stats.add(segment.walkTime);
            stats.add(segment.waitStats);
            stats.add(segment.rideStats);
            ride = ride.previous;
        }
        this.finalWalkTime = finalWalkTime;
        stats.add(finalWalkTime);
        summary = generateSegmentSummary();
    }

    public Option (State state) {
        stats = new Stats();
        int time = (int) state.getElapsedTimeSeconds();
        stats.add(time);
        this.finalWalkTime = time;
        summary = state.getNonTransitMode().toString(); // this might not work if there is a transition to another mode
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
