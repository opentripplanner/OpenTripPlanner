package org.opentripplanner.routing.linking;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains temporary vertices and edges that are used in A-Star searches. After they
 * are no longer needed, this class removes the temporary vertices and edges. It implements
 * AutoCloseable and the cleanup is automatically done with a try-with-resources statement.
 */
public class TemporaryVerticesContainer implements AutoCloseable {

  private final List<DisposableEdgeCollection> tempEdgeCollections = new ArrayList<>();

  public void addEdgeCollection(DisposableEdgeCollection collection) {
    if (!collection.isEmpty()) {
      this.tempEdgeCollections.add(collection);
    }
  }

  /**
   * Tear down this container, removing any temporary edges from the "permanent" graph objects. This
   * enables all temporary objects for garbage collection.
   */
  @Override
  public void close() {
    this.tempEdgeCollections.forEach(DisposableEdgeCollection::disposeEdges);
  }
}
