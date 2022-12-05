package org.opentripplanner.visualizer;

import java.util.List;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * An interface allowing a map UI element to report that the user has selected vertices.
 */
public interface VertexSelectionListener {
  void verticesSelected(List<Vertex> selected);
}
