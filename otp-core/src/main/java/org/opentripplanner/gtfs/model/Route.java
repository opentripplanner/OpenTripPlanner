package org.opentripplanner.gtfs.model;

import java.util.Collection;
import java.util.List;

public class Route extends GtfsEntity {

    private static final long serialVersionUID = 1L;
    
    @Required public String route_id;
    @Required public String route_type;
    public String agency_id;
    public String route_desc;
    public String route_url;
    public String route_color;
    public String route_text_color;
    public String route_short_name;
    public String route_long_name;

    // GTFS spec requires either long or short name,
    // hence this overridden method.
    @Override
    public List<Error> checkRequiredFields(Collection<String> present) {
        List<Error> errors = super.checkRequiredFields(present);
        boolean ok = present.contains("route_short_name") || present.contains("route_long_name");
        if (!ok)
            errors.add(new Error("must contain one of..."));
        return errors;
    }

    @Override
    public String getKey() {
        return route_id;
    }
    
}