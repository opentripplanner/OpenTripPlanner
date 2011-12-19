package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.graph.AbstractEdge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitVertex;

/*
 * An edge in the GTFS layer
 */
public abstract class TransitEdge extends AbstractEdge {

    public TransitEdge(TransitVertex v1, TransitVertex v2) {
        super(v1, v2);
    }

    /* get line, route, pattern */
    
}
