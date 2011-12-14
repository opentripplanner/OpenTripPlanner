package org.opentripplanner.api.model.analysis;

import org.opentripplanner.routing.core.Vertex;

public class SimpleVertex {
    public String label;

    public double x;

    public double y;

    public SimpleVertex() {
    }

    public SimpleVertex(Vertex v) {
        x = v.getX();
        y = v.getY();
        label = v.label;
    }
}
