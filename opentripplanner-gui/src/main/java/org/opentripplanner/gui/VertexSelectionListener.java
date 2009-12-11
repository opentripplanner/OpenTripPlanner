package org.opentripplanner.gui;

import java.util.List;

import org.opentripplanner.routing.core.Vertex;

/**
 * Some sort of vertices has been selected.
 *
 */
public interface VertexSelectionListener {

    public void verticesSelected(List<Vertex> selected);
}
