package org.opentripplanner.street.model.vertex;

/**
 * Marker interface for temporary vertices.
 * <p>
 * Remember to use the {@link #dispose(Vertex)} to delete the temporary vertex from the main graph
 * after use.
 * </p>
 */
public interface TemporaryVertex {
  /**
   * This method traverse the subgraph of temporary vertices, and cuts that subgraph off from the
   * main graph at each point it encounters a non-temporary vertexes. OTP then holds no references
   * to the temporary subgraph and it is garbage collected.
   * <p>
   * Note! If the {@code vertex} is NOT a TemporaryVertex the method returns. No action taken.
   * </p>
   *
   * @param vertex Vertex part of the temporary part of the graph.
   */
  static void dispose(Vertex vertex) {
    TemporaryVertexDispose.dispose(vertex);
  }
}
