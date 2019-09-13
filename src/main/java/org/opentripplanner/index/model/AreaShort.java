package org.opentripplanner.index.model;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

public class AreaShort {
    public FeedScopedId areaId;
    public EncodedPolylineBean polygon;

    public AreaShort(FeedScopedId areaId, Geometry polygon) {
        this.areaId = areaId;
        this.polygon = PolylineEncoder.createEncodings(polygon.getBoundary());
    }
}
