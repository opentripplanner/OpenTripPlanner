package org.opentripplanner.profile;

import java.util.List;

import lombok.Getter;

import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.profile.ProfileRouter.Ride;
import org.opentripplanner.profile.ProfileRouter.Stats;

import com.google.common.base.Joiner;
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

}
