package org.opentripplanner.index.model;

import java.util.Collection;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.gtfs.GtfsLibrary;

import com.beust.jcommander.internal.Lists;

public class RouteShort {

    public AgencyAndId id;
    public String shortName;
    public String longName;
    public String desc;
    public String mode;
    public String color;
    public String textColor;
    public String agencyName;
    public String agencyId;
    public String url;
    public int schoolOnly;

    public RouteShort (Route route) {
        id = route.getId();
        shortName = route.getShortName();
        longName = route.getLongName();
        desc = route.getDesc();
        mode = GtfsLibrary.getTraverseMode(route).toString();
        color = route.getColor();
        textColor = route.getTextColor();
        agencyName = route.getAgency().getName();
        agencyId = route.getAgency().getId();
        url = route.getUrl();
        schoolOnly = route.getSchoolOnly();
    }

    public static List<RouteShort> list (Collection<Route> in) {
        List<RouteShort> out = Lists.newArrayList();
        for (Route route : in) out.add(new RouteShort(route));
        return out;
    }

}
