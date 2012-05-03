package org.opentripplanner.analyst.core;

import java.util.List;

import org.geotools.geometry.Envelope2D;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.routing.graph.Vertex;

import com.vividsolutions.jts.geom.Envelope;

public interface GeometryIndexService {
    @SuppressWarnings("rawtypes")
    List query(Envelope env);
    @SuppressWarnings("rawtypes")
    List queryPedestrian(Envelope env);

    Vertex getNearestPedestrianStreetVertex(double lon, double lat);

    BoundingBox getBoundingBox(CoordinateReferenceSystem crs);
}
