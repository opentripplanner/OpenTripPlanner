package org.opentripplanner.api.thrift.util;

import org.opentripplanner.api.thrift.definition.TravelState;
import org.opentripplanner.routing.core.State;

/**
 * Extends the Thrift TravelState for convenient construction.
 * 
 * @author avi
 * 
 */
public class TravelStateExtension extends TravelState {

    /**
     * Required for serialization.
     */
    private static final long serialVersionUID = 6666801480159263902L;

    /**
     * Construct from a State object.
     * 
     * @param e
     */
    public TravelStateExtension(State state) {
        super();

        setArrival_time(state.getTime());
        setVertex(new GraphVertexExtension(state.getVertex()));
        
        if (state.getBackEdge() != null) { 
            setBack_edge(new GraphEdgeExtension(state.getBackEdge()));
        }
    }

}