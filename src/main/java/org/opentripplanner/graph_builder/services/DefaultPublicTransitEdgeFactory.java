package org.opentripplanner.graph_builder.services;

import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PublicTransitEdge;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

/**
 * Created by mabu on 18.3.2015.
 */
public class DefaultPublicTransitEdgeFactory {

    public PublicTransitEdge createEdge(int fwdId, IntersectionVertex startEndpoint, IntersectionVertex endEndpoint,
                                        LineString geometry, String name, double length, TraverseMode publicTransitType,
                                        boolean back, long osmID) {
        return new PublicTransitEdge(back ? fwdId : 0, osmID, startEndpoint, endEndpoint, geometry,
                name, length, publicTransitType, back);
    }
}
