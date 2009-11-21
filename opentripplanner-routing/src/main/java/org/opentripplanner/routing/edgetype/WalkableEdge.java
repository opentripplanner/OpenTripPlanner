package org.opentripplanner.routing.edgetype;

import com.vividsolutions.jts.geom.LineString;

public interface WalkableEdge {

    public LineString getGeometry();

}
