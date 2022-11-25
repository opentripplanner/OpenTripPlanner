package org.opentripplanner.graph_builder.module;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;

class Subgraph {

  private final Set<Vertex> streetVertexSet;
  private final Set<Vertex> stopsVertexSet;

  Subgraph() {
    streetVertexSet = new HashSet<>();
    stopsVertexSet = new HashSet<>();
  }

  void addVertex(Vertex vertex) {
    if (vertex instanceof TransitStopVertex) {
      stopsVertexSet.add(vertex);
    } else {
      streetVertexSet.add(vertex);
    }
  }

  boolean contains(Vertex vertex) {
    return (streetVertexSet.contains(vertex) || stopsVertexSet.contains(vertex));
  }

  int streetSize() {
    return streetVertexSet.size();
  }

  int stopSize() {
    return stopsVertexSet.size();
  }

  Vertex getRepresentativeVertex() {
    //TODO this is not very smart but good enough at the moment
    return streetVertexSet.iterator().next();
  }

  Iterator<Vertex> streetIterator() {
    return streetVertexSet.iterator();
  }

  Iterator<Vertex> stopIterator() {
    return stopsVertexSet.iterator();
  }
}
