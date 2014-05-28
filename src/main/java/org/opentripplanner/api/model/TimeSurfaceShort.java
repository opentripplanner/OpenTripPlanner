package org.opentripplanner.api.model;

import com.google.common.collect.Lists;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.routing.core.RoutingRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API data model object briefly describing a time surface.
 */
public class TimeSurfaceShort {

    public int id;
    public Map<String, String> params;

    public TimeSurfaceShort(TimeSurface surface) {
        this.id = surface.id;
        this.params = surface.params;
    }

    public static List<TimeSurfaceShort> list (Collection<TimeSurface> in) {
        List<TimeSurfaceShort> out = Lists.newArrayList();
        for (TimeSurface surface : in) {
            out.add(new TimeSurfaceShort(surface));
        }
        return out;
    }

}
