package org.opentripplanner.routing.edgetype;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.graph.Vertex;

public class PoiLinkEdge extends FreeEdge {

    private static final long serialVersionUID = 3925814840369402222L;

    public PoiLinkEdge(Vertex from, Vertex to) {
        super(from, to);
    }

    public LineString getGeometry() {
        Coordinate[] coordinates = new Coordinate[] { fromv.getCoordinate(), tov.getCoordinate()};
        return GeometryUtils.getGeometryFactory().createLineString(coordinates);
    }

}
