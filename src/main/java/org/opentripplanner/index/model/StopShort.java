package org.opentripplanner.index.model;

import java.util.Collection;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class StopShort {

    public AgencyAndId id;
    public String code;
    public String name;
    public double lat;
    public double lon;
    public String cluster;

    /** Distance to the stop when it is returned from a location-based query. */
    @JsonInclude(Include.NON_NULL) public Integer dist;
    
    public StopShort (Stop stop) {
        id = stop.getId();
        code = stop.getCode();
        name = stop.getName();
        lat = stop.getLat();
        lon = stop.getLon();
        cluster = stop.getParentStation(); // TODO harmonize these names, maybe use "station" everywhere
    }

    /** @param distance in integral meters, to avoid serializing a bunch of decimal places. */
    public StopShort(Stop stop, int distance) {
        this(stop);
        this.dist = distance;
    }

    public static List<StopShort> list (Collection<Stop> in) {
        List<StopShort> out = Lists.newArrayList();
        for (Stop stop : in) out.add(new StopShort(stop));
        return out;
    }    

}
