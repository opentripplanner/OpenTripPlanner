package org.opentripplanner.index.model;

import com.google.common.collect.Lists;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.List;

/**
 * Some stopTimes all in the same pattern.
 */
public class StopTimesInPattern {

    /**
     * Pattern which these arrival-departures are associated with.
     */
    public PatternShort pattern;

    /**
     * Route which these arrival-departures are associated with.
     */
    public RouteShort route;

    /**
     * List of upcoming arrival/departures.
     */
    public List<TripTimeShort> times = Lists.newArrayList();

    public StopTimesInPattern(TripPattern pattern) {
        this.pattern = new PatternShort(pattern);
        this.route = new RouteShort(pattern.route);
    }

}
