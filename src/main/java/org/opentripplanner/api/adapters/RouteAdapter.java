package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.model.Route;

public class RouteAdapter extends XmlAdapter<RouteType, Route> {

    @Override
    public Route unmarshal(RouteType arg) throws Exception {
        throw new UnsupportedOperationException(
                "We presently serialize Route as RouteType, and thus cannot deserialize them");
    }

    @Override
    public RouteType marshal(Route arg) throws Exception {
        if (arg == null) {
            return null;
        }
        return new RouteType(arg);
    }

}
