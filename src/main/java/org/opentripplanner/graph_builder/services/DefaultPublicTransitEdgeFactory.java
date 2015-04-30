package org.opentripplanner.graph_builder.services;

import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PublicTransitEdge;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

/**
 * Created by mabu on 18.3.2015.
 */
public class DefaultPublicTransitEdgeFactory {

    public PublicTransitEdge createEdge(IntersectionVertex startEndpoint, IntersectionVertex endEndpoint,
                                        LineString geometry, String name, double length, TraverseMode publicTransitType,
                                        boolean back) {
        PublicTransitEdge pte = new PublicTransitEdge(startEndpoint, endEndpoint, geometry, name, length, publicTransitType, back);
        return pte;
    }
}
