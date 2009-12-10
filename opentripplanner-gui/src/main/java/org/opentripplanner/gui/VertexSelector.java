package org.opentripplanner.gui;

import java.util.List;

import org.opentripplanner.routing.core.Vertex;

public interface VertexSelector {

    public void verticesSelected(List<Vertex> selected);
}
