package org.opentripplanner.api.ws.impl;

import org.opentripplanner.api.ws.services.HullService;

import com.vividsolutions.jts.geom.Geometry;

public class StoredHullService implements HullService {

    private Geometry hull;

    public StoredHullService(Geometry hull) {
        this.hull = hull;
    }
    
    @Override
    public Geometry getHull() {
        return hull;
    }

}
