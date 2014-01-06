package org.opentripplanner.profile;

import java.util.List;

import lombok.Getter;

import org.opentripplanner.profile.ProfileData.Pattern;
import org.opentripplanner.profile.ProfileRouter.Ride;
import org.opentripplanner.profile.ProfileRouter.Stats;

import com.beust.jcommander.internal.Lists;

public class Segment {

    @Getter int walkDist;
    @Getter Stats waitStats;

    @Getter String route;
    @Getter String from;
    @Getter String to;
    @Getter String fromName;
    @Getter String toName;
    @Getter String routeShortName;
    @Getter String routeLongName;
    @Getter Stats  stats;
    @Getter List<String> patterns = Lists.newArrayList();

    public Segment (Ride ride) {
        route = ride.route.getId().getId();
        routeShortName = ride.route.getShortName();
        routeLongName = ride.route.getLongName();
        from = ride.from.getId().getId();
        to = ride.to.getId().getId();
        fromName = ride.from.getName();
        toName = ride.to.getName();
        stats  = ride.stats;
        for (Pattern pattern : ride.patterns) {
            patterns.add(pattern.patternId);
        }
        walkDist = ride.dist;
        waitStats = characterizeTransfer(ride);
    }
    
    /** 
     * Distances can be stored in rides, including the first and last distance. 
     * But waits must be calculated from full sets of patterns, which are not known until a round is over.
     */
    public Stats characterizeTransfer (Ride ride) {
        Ride prev = ride.previous;
        Stats stats = new Stats();
        if (prev != null) {
            stats.min = 500;
            stats.avg = 700;
            stats.max = 900;
        }
        return stats;
    }

}
