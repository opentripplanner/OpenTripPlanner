package org.opentripplanner.routing.vertextype;

import lombok.Getter;
import lombok.Setter;
import org.opentripplanner.routing.graph.Graph;

public class TransitStopStreetVertex extends IntersectionVertex {

    @Getter
    @Setter
    private String stopCode;

    public TransitStopStreetVertex(Graph g, String label, double x, double y, String name, String stopCode) {
        super(g, label, x, y, name);
        this.setStopCode(stopCode);
    }
}
