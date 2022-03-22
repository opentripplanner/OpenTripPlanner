package org.opentripplanner.graph_builder.module;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check the every vertex and edge in the graph to make sure the edge lists and from/to
 * members are coherent, and that there are no edgeless vertices. Primarily intended for debugging.
 */
public class GraphCoherencyCheckerModule implements GraphBuilderModule {


    /** An set of ids which identifies what stages this graph builder provides (i.e. streets, elevation, transit) */
    public List<String> provides() {
        return Collections.emptyList();
    }

    /** A list of ids of stages which must be provided before this stage */
    public List<String> getPrerequisites() {
        return List.of("streets");
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(GraphCoherencyCheckerModule.class);

    @Override
    public void buildGraph(
            Graph graph,
            HashMap<Class<?>, Object> extra,
            DataImportIssueStore issueStore
    ) {
        boolean coherent = true;
        LOG.info("checking graph coherency...");
        for (Vertex v : graph.getVertices()) {
            if (v.getOutgoing().isEmpty() && v.getIncoming().isEmpty()) {
                // This is ok for island transit stops etc. Log so that the type can be checked
                issueStore.add("VertexWithoutEdges", "vertex %s has no edges", v);
            }
            for (Edge e : v.getOutgoing()) {
                if (e.getFromVertex() != v) {
                    issueStore.add("InvalidEdge", "outgoing edge of %s: from vertex %s does not match", v, e);
                    coherent = false;
                }
                if (e.getToVertex() == null) {
                    issueStore.add("InvalidEdge", "outgoing edge has no to vertex %s", e);
                    coherent = false;
                }
            }
            for (Edge e : v.getIncoming()) {
                if (e.getFromVertex() == null) {
                    issueStore.add("InvalidEdge", "incoming edge has no from vertex %s", e);
                    coherent = false;
                }
                if (e.getToVertex() != v) {
                    issueStore.add("InvalidEdge", "incoming edge of %s: to vertex %s does not match", v, e);
                    coherent = false;
                }
            }
        }
        LOG.info("edge lists and from/to members are {}coherent.", coherent ? "": "not ");
    }

    @Override
    public void checkInputs() {
        //No inputs other than the graph itself
    }
    
}
