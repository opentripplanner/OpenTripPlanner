package org.opentripplanner.visualizer;

import org.opentripplanner.routing.graph.Vertex;

import java.util.List;

/**
 * An interface allowing a map UI element to report
 * that the user has selected vertices.
 *
 */
public interface VertexSelectionListener {
    public void verticesSelected(List<Vertex> selected);
}
