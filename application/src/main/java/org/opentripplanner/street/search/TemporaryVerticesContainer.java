package org.opentripplanner.street.search;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;

/**
 * This class contains temporary vertices and edges that are used in A-Star searches. After they
 * are no longer needed, this class removes the temporary vertices and edges. It implements
 * AutoCloseable and the cleanup is automatically done with a try-with-resources statement.
 */
public class TemporaryVerticesContainer implements AutoCloseable {

  private final List<DisposableEdgeCollection> tempEdges = new ArrayList<>();


  /**
   * Tear down this container, removing any temporary edges from the "permanent" graph objects. This
   * enables all temporary objects for garbage collection.
   */
  @Override
  public void close() {
    this.tempEdges.forEach(DisposableEdgeCollection::disposeEdges);
  }
}
