package org.opentripplanner.graph_builder.module.islandpruning;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Assigns a dense int id to every vertex, so that {@link IslandPruningModule} can keep its
 * per-vertex state (adjacency, subgraph membership) in plain arrays. At country scale the graph
 * has millions of vertices and hash map lookups keyed on {@link Vertex} dominate the run time,
 * array access is much cheaper.
 * <p>
 * Ids follow the iteration order of the collection given to the constructor. The index is
 * immutable: looking up a vertex that is not in that collection is an error.
 */
class VertexIndex {

  private static final int NO_ID = -1;

  private final TObjectIntMap<Vertex> ids;
  private final List<Vertex> vertices;
  private final BitSet streetVertex;
  private final BitSet stopVertex;

  VertexIndex(Collection<Vertex> graphVertices) {
    this.ids = new TObjectIntHashMap<>(graphVertices.size(), 0.5f, NO_ID);
    this.vertices = new ArrayList<>(graphVertices.size());
    this.streetVertex = new BitSet(graphVertices.size());
    this.stopVertex = new BitSet(graphVertices.size());
    for (Vertex v : graphVertices) {
      addVertex(v);
    }
  }

  int idOf(Vertex v) {
    int id = ids.get(v);
    if (id == NO_ID) {
      throw new IllegalArgumentException("Vertex %s is not in the index".formatted(v));
    }
    return id;
  }

  Vertex vertex(int id) {
    return vertices.get(id);
  }

  int size() {
    return vertices.size();
  }

  boolean isStreetVertex(int id) {
    return streetVertex.get(id);
  }

  boolean isStopVertex(int id) {
    return stopVertex.get(id);
  }

  private void addVertex(Vertex v) {
    int id = vertices.size();
    ids.put(v, id);
    vertices.add(v);
    streetVertex.set(id, v instanceof StreetVertex);
    stopVertex.set(id, v instanceof TransitStopVertex);
  }
}
