package org.opentripplanner.index.model;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class StopShort {

    public FeedScopedId id;
    public String code;
    public String name;
    public double lat;
    public double lon;
    public String url;
    public String cluster;

    /** Optional extra stop information that may be included */
    @JsonInclude(Include.NON_NULL) public Collection<StopTimesInPattern> stopTimes;
    @JsonInclude(Include.NON_NULL) public Set<Route> routes;

    /** Distance to the stop when it is returned from a location-based query. */
    @JsonInclude(Include.NON_NULL) public Integer dist;
    
    public StopShort (Stop stop) {
        id = stop.getId();
        code = stop.getCode();
        name = stop.getName();
        lat = stop.getLat();
        lon = stop.getLon();
        url = stop.getUrl();
        cluster = stop.getParentStation(); // TODO harmonize these names, maybe use "station" everywhere
    }
    public StopShort(Stop stop, Collection<StopTimesInPattern> stopTimes, Set<Route> routesForStop) {
        this(stop);
        this.stopTimes = stopTimes;
        this.routes = routesForStop;
    }

    /** @param distance in integral meters, to avoid serializing a bunch of decimal places. */
    public StopShort(Stop stop, int distance) {
        this(stop);
        this.dist = distance;
    }
    public StopShort(Stop stop, int distance, Collection<StopTimesInPattern> stopTimes, Set<Route> routesForStop) {
        this(stop, distance);
        this.stopTimes = stopTimes;
        this.routes = routesForStop;
    }

    public static List<StopShort> list (Collection<Stop> in) {
        List<StopShort> out = Lists.newArrayList();
        for (Stop stop : in) out.add(new StopShort(stop));
        return out;
    }    

}
