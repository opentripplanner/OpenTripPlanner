package org.opentripplanner.index.model;

import com.beust.jcommander.internal.Lists;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;

import java.util.Collection;
import java.util.List;

public class ApiRouteShort {

    public FeedScopedId id;
    public String shortName;
    public String longName;
    public String mode;
    public String color;
    public String agencyName;

    public ApiRouteShort(Route route) {
        id = route.getId();
        shortName = route.getShortName();
        longName = route.getLongName();
        mode = GtfsLibrary.getTraverseMode(route).toString();
        color = route.getColor();
        agencyName = route.getAgency().getName();
    }

    public static List<ApiRouteShort> list (Collection<Route> in) {
        List<ApiRouteShort> out = Lists.newArrayList();
        for (Route route : in) out.add(new ApiRouteShort(route));
        return out;
    }

}
