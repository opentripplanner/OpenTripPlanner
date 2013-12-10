package org.opentripplanner.profile;

import java.util.Set;

import lombok.Getter;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.profile.ProfileData.Pattern;
import org.opentripplanner.profile.ProfileRouter.Ride;
import org.opentripplanner.profile.ProfileRouter.Stats;

import com.beust.jcommander.internal.Sets;

public class Segment {

    @Getter String route;
    @Getter String from;
    @Getter String to;
    @Getter String fromName;
    @Getter String toName;
    @Getter String qualifier;
    @Getter String routeShortName;
    @Getter String routeLongName;
    @Getter Stats  stats;
    @Getter Set<String> stops = Sets.newHashSet();
    
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
            boolean onboard = false;
            for (Stop stop : pattern.stops) {
                if (!onboard) {
                    if (stop == ride.from) onboard = true;
                    else continue;
                }
                stops.add(stop.getId().getId());
                if (stop == ride.to) break;
            }
        }
    }
    
}
