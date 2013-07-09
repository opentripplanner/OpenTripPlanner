package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.StreetVertexIndexFactory;
import org.opentripplanner.routing.services.StreetVertexIndexService;

/**
 * Default implementation. Simply returns an instance of StreetVertexIndexServiceImpl.
 * @author avi
 */
public class DefaultStreetVertexIndexFactory implements StreetVertexIndexFactory {

    @Override
    public StreetVertexIndexService newIndex(Graph g) {
        return new StreetVertexIndexServiceImpl(g);
    }
}
