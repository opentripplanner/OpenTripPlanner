package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.routing.graph.Vertex;

public class PrunedIslandStop implements DataImportIssue {

    public static final String FMT = "Stop %s was mapped to a pruned sub graph";

    final Vertex vertex;

    public PrunedIslandStop(Vertex vertex){
        this.vertex = vertex;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, vertex.getLabel());
    }

    @Override
    public String getHTMLMessage() {
        return String.format(FMT, vertex.getLabel());
    }

    @Override
    public Vertex getReferencedVertex() {
        return vertex;
    }
}
