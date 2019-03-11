package org.opentripplanner.routing.services;

import org.opentripplanner.routing.graph.Graph;


/**
 * Factory for StreetVertexIndexServices.
 * 
 * @author avi
 */
public interface StreetVertexIndexFactory {
    
    /**
     * Returns a new StreetVertexIndexService for this graph.
     * @param g
     * @return
     */
    public StreetVertexIndexService newIndex(Graph g);
}
