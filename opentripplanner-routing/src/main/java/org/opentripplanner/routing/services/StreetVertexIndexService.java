package org.opentripplanner.routing.services;

import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Coordinate;

public interface StreetVertexIndexService {
    public Vertex getClosestVertex(Coordinate location);
}
