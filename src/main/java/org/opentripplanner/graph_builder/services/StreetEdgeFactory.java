package org.opentripplanner.graph_builder.services;

import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.AreaEdgeList;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.util.I18NString;

/**
 * Edge factory to build StreetEdge and AreaEdge.
 * 
 * We use a factory to be able to build either StreetEdge or StreetWithElevationEdge, depending on
 * whether you want elevation data later on or not.
 * 
 */
public interface StreetEdgeFactory {

    public StreetEdge createEdge(IntersectionVertex startEndpoint, IntersectionVertex endEndpoint,
            LineString geometry, I18NString name, double length, StreetTraversalPermission permissions,
            boolean back);

    public AreaEdge createAreaEdge(IntersectionVertex startEndpoint,
            IntersectionVertex endEndpoint, LineString geometry, I18NString name, double length,
            StreetTraversalPermission permissions, boolean back, AreaEdgeList area);

}
