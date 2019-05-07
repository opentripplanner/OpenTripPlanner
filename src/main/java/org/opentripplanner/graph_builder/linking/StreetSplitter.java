package org.opentripplanner.graph_builder.linking;

import org.opentripplanner.routing.graph.Vertex;

/**
 * This interface allows passing a SimpleStreetSplitter object to the Graph without making Graph depend on
 * SimpleStreetSplitter (avoiding circular dependency).
 */
public interface StreetSplitter {
    boolean linkToClosestWalkableEdge (Vertex vertex, boolean destructiveSplitting);
}
