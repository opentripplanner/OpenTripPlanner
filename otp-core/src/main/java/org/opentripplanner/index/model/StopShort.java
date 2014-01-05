package org.opentripplanner.index.model;

import java.util.Collection;
import java.util.List;

import org.onebusaway.gtfs.model.Stop;

import com.beust.jcommander.internal.Lists;

public class StopShort {

    public String agency;
    public String id;
    public String name;
    public double lat;
    public double lon;
    
    public StopShort (Stop stop) {
        agency = stop.getId().getAgencyId();
        id = stop.getId().getId();
        name = stop.getName();
        lat = stop.getLat();
        lon = stop.getLon();
    }

    public static List<StopShort> list (Collection<Stop> in) {
        List<StopShort> out = Lists.newArrayList();
        for (Stop stop : in) out.add(new StopShort(stop));
        return out;
    }    

}
