package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.routing.graph.Vertex;

public class IsolatedStop implements DataImportIssue {

    public static final String FMT = "Unable to link stop %s to the street graph";

    final Vertex vertex;

    public IsolatedStop(Vertex vertex){
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
