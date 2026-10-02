package org.opentripplanner.graph_builder.module.islandpruning;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Ids follow the iteration order of the collection given to the constructor. Vertices not in that
 * collection are given a new id the first time they are looked up.
 */
class VertexIndex {

  private static final int NO_ID = -1;

  private final TObjectIntMap<Vertex> ids;
  private final List<Vertex> vertices;
  private boolean[] streetVertex;
  private boolean[] stopVertex;

  VertexIndex(Collection<Vertex> vertices) {
    this.ids = new TObjectIntHashMap<>(vertices.size() * 2, 0.5f, NO_ID);
    this.vertices = new ArrayList<>(vertices.size());
    this.streetVertex = new boolean[vertices.size()];
    this.stopVertex = new boolean[vertices.size()];
    for (Vertex v : vertices) {
      idOf(v);
    }
  }

  /** Returns the id of the given vertex, assigning a new one if it does not have one yet. */
  int idOf(Vertex v) {
    int id = ids.get(v);
    if (id != NO_ID) {
      return id;
    }
    id = vertices.size();
    ids.put(v, id);
    vertices.add(v);
    if (id >= streetVertex.length) {
      int newLength = Math.max(16, streetVertex.length * 2);
      streetVertex = Arrays.copyOf(streetVertex, newLength);
      stopVertex = Arrays.copyOf(stopVertex, newLength);
    }
    streetVertex[id] = v instanceof StreetVertex;
    stopVertex[id] = v instanceof TransitStopVertex;
    return id;
  }

  Vertex vertex(int id) {
    return vertices.get(id);
  }

  int size() {
    return vertices.size();
  }

  boolean isStreetVertex(int id) {
    return streetVertex[id];
  }

  boolean isStopVertex(int id) {
    return stopVertex[id];
  }
}
