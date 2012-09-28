package org.opentripplanner.api.ws.services;

import com.vividsolutions.jts.geom.Geometry;

public interface HullService {
    public Geometry getHull();
}
