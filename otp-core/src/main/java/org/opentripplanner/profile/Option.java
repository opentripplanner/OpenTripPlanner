package org.opentripplanner.profile;

import java.util.List;

import lombok.Getter;

import org.opentripplanner.profile.ProfileRouter.Ride;
import org.opentripplanner.profile.ProfileRouter.Stats;

import com.google.common.collect.Lists;

public class Option {

    @Getter List<Segment> segments = Lists.newLinkedList();
    @Getter int finalWalkTime;
    @Getter Stats stats;
    
    public Option (Ride ride, int finalWalkTime) {
        stats = new Stats();
        while (ride != null) {
            Segment segment = new Segment(ride); 
            segments.add(0, segment);
            stats.add(segment.walkTime);
            stats.add(segment.waitStats);
            stats.add(segment.rideStats);
            ride = ride.previous;
        }
        this.finalWalkTime = finalWalkTime;
        stats.add(finalWalkTime);
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("routes ");
        //Joiner.on(", ").join(X);
        for (Segment segment : segments) {
            sb.append(segment.routeShortName);
            sb.append(" ");
        }
        sb.append("via ");
        for (Segment segment : segments.subList(0, segments.size() - 1)) {
            sb.append(segment.toName);
            sb.append(" ");
        }
        return sb.toString();
    }

}
