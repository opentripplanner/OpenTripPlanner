package org.opentripplanner.graph_builder.impl.osm;

import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.AreaEdgeList;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

import com.vividsolutions.jts.geom.LineString;

public class DefaultOSMPlainStreetEdgeFactory implements OSMPlainStreetEdgeFactory {

    @Override
    public PlainStreetEdge createEdge(OSMNode fromNode, OSMNode toNode, OSMWithTags way,
            IntersectionVertex startEndpoint, IntersectionVertex endEndpoint, LineString geometry,
            String name, double length, StreetTraversalPermission permissions, boolean back) {
        return new PlainStreetEdge(startEndpoint, endEndpoint, geometry, name, length, permissions,
                back);
    }

    @Override
    public AreaEdge createAreaEdge(OSMNode nodeI, OSMNode nodeJ,
            OSMWithTags areaEntity, IntersectionVertex startEndpoint,
            IntersectionVertex endEndpoint, LineString geometry, String name,
            double length, StreetTraversalPermission permissions,
            boolean back, AreaEdgeList area) {
        return new AreaEdge(startEndpoint, endEndpoint, geometry, name, length, permissions,
                back, area);
    }

}
