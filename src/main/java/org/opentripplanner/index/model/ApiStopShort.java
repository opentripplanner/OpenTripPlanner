package org.opentripplanner.index.model;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;

import java.util.Collection;
import java.util.List;

public class ApiStopShort {

    public FeedScopedId id;
    public String code;
    public String name;
    public double lat;
    public double lon;
    public String url;
    public String cluster;

    /** Distance to the stop when it is returned from a location-based query. */
    @JsonInclude(Include.NON_NULL) public Integer dist;
    
    public ApiStopShort(Stop stop) {
        id = stop.getId();
        code = stop.getCode();
        name = stop.getName();
        lat = stop.getLat();
        lon = stop.getLon();
        url = stop.getUrl();
        // parentStation may be missing on the stop returning null.
        cluster = stop.getParentStation() != null ? stop.getParentStation().getId().getId() : null; // TODO harmonize these names, maybe use "station" everywhere
    }

    /** @param distance in integral meters, to avoid serializing a bunch of decimal places. */
    public ApiStopShort(Stop stop, int distance) {
        this(stop);
        this.dist = distance;
    }

    public static List<ApiStopShort> list (Collection<Stop> in) {
        List<ApiStopShort> out = Lists.newArrayList();
        for (Stop stop : in) out.add(new ApiStopShort(stop));
        return out;
    }    

}
