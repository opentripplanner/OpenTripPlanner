package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.graph.Graph;

public interface DummyableEdge {

    void replaceDummyVertices(Graph graph);

}
