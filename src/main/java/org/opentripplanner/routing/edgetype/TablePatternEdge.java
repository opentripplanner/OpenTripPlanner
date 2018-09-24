package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.OnboardVertex;
import org.opentripplanner.routing.vertextype.TransitVertex;

/**
 * A superclass for general trip pattern related edges
 * @author novalis
 *
 */
public abstract class TablePatternEdge extends Edge implements PatternEdge {

    private static final long serialVersionUID = 1L;

    public TablePatternEdge(TransitVertex fromv, TransitVertex tov) {
        super(fromv, tov);
    }

    public TripPattern getPattern() {
        return ((OnboardVertex)fromv).getTripPattern();
    }

    public abstract int getStopIndex();

}
