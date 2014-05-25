package org.opentripplanner.profile;

import java.util.Comparator;
import java.util.List;

import lombok.Getter;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.api.resource.PlanGenerator;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.spt.GraphPath;

public class Option {

    public List<Segment> segments = Lists.newLinkedList();
    public int finalWalkTime;
    public Stats stats;
    public String summary;
    public List<WalkStep> walkSteps;

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

    /** A constructor for an option that includes only a street mode, not transit. */
    public Option (State state) {
        stats = new Stats();
        int time = (int) state.getElapsedTimeSeconds();
        stats.add(time);
        // this might not work if there is a transition to another mode
        TraverseMode mode = state.getNonTransitMode();
        if (mode == TraverseMode.WALK) this.finalWalkTime = time;
        summary = mode.toString();
        GraphPath path = new GraphPath(state, false);
        walkSteps = PlanGenerator.generateWalkSteps(path.states.toArray(new State[0]), null);
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
