package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.vertextype.IntersectionVertex;

import com.vividsolutions.jts.geom.LineString;

public class AreaEdge extends PlainStreetEdge {
    private static final long serialVersionUID = 6761687673982054612L;
    private AreaEdgeList area;

    public AreaEdge(IntersectionVertex startEndpoint,
            IntersectionVertex endEndpoint, LineString geometry, String name,
            double length, StreetTraversalPermission permissions, boolean back,
            float carSpeed, AreaEdgeList area) {
        super(startEndpoint, endEndpoint, geometry, name, length, permissions, back, carSpeed);
        this.area = area;
        area.addEdge(this);
    }

    public AreaEdgeList getArea() {
        return area;
    }
    
    public int detach() {
        area.removeEdge(this);
        return super.detach();
    }

    public void setArea(AreaEdgeList area) {
        this.area = area;
    }
}
