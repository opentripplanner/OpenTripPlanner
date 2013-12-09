package org.opentripplanner.profile;

import java.util.List;

import lombok.Getter;

import org.opentripplanner.profile.ProfileRouter.Ride;

import com.google.common.collect.Lists;

public class Option {

    @Getter
    List<Segment> segments = Lists.newLinkedList();
    
    public Option (Ride ride) {
        while (ride != null) {
            segments.add(0, new Segment(ride));
            ride = ride.previous;
        }
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
