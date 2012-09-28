package org.opentripplanner.analyst.core;

import java.util.List;

import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

public interface GeometryIndexService {
    @SuppressWarnings("rawtypes")
    List query(Envelope env);
    @SuppressWarnings("rawtypes")
    List queryPedestrian(Envelope env);

    BoundingBox getBoundingBox(CoordinateReferenceSystem crs);
}
