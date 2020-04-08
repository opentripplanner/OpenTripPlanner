package org.opentripplanner.index.model;

import com.beust.jcommander.internal.Lists;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TripPattern;

import java.util.Collection;

public class ApiPatternDetail extends PatternShort {

    /* Maybe these should just be lists of IDs only, since there are stops and trips subendpoints. */
    public FeedScopedId routeId;
    public Collection<ApiStopShort> stops = Lists.newArrayList();
    public Collection<ApiTripShort> trips = Lists.newArrayList();
    
    // Include all known headsigns
    
    public ApiPatternDetail(TripPattern pattern) {
        super (pattern);
        routeId = pattern.route.getId();
        stops = ApiStopShort.list(pattern.getStops());
        trips = ApiTripShort.list(pattern.getTrips());
    }
    
}
