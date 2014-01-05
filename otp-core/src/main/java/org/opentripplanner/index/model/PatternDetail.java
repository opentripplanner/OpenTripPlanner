package org.opentripplanner.index.model;

import java.util.Collection;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.edgetype.TableTripPattern;

import com.beust.jcommander.internal.Lists;

public class PatternDetail extends PatternShort {

    public String routeId;
    public Collection<StopShort> stops = Lists.newArrayList();
    
    // Include all known headsigns
    
    public PatternDetail(TableTripPattern pattern) {
        super (pattern);
        routeId = pattern.route.getId().getId();
        for (Stop stop : pattern.getStops()) {
            stops.add(new StopShort(stop));
        }
    }
    
}
