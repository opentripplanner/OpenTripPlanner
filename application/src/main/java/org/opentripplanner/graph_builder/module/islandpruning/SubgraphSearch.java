package org.opentripplanner.graph_builder.module.islandpruning;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Breadth-first search for connected subgraphs over an {@link Adjacency}. Keeps its scratch
 * arrays between searches, so that each search only costs the size of the subgraph found.
 */
class SubgraphSearch {

  private static final int NO_SUBGRAPH = -1;

  private final VertexIndex vertexIndex;
  private final int[] queue;
  /**
   * The search that last visited each vertex, avoids clearing a visited set between searches.
   */
  private final int[] visitedBy;
  private int searchId = 0;
  private int nextSubgraphId = 0;

  SubgraphSearch(VertexIndex vertexIndex) {
    this.vertexIndex = vertexIndex;
    // a vertex is enqueued at most once per search, except the start vertex which is enqueued
    // again when reached from one of its neighbours
    this.queue = new int[vertexIndex.size() + 1];
    this.visitedBy = new int[vertexIndex.size()];
  }

  /**
   * A vertex to subgraph mapping, indexed on {@link VertexIndex} ids. Each subgraph has a unique
   * id, {@link #NO_SUBGRAPH} marks vertices that do not belong to one.
   */
  static int[] newSubgraphMapping(int vertexCount) {
    int[] mapping = new int[vertexCount];
    Arrays.fill(mapping, NO_SUBGRAPH);
    return mapping;
  }

  /**
   * @param newgraphs put new subgraphs here
   * @param anchors   optional isolation mapping from a previous round
   * @param islands   final list of islands or null
   */
  int collectSubGraphs(
    Adjacency neighbours,
    int[] newgraphs,
    @Nullable int[] anchors,
    @Nullable List<Subgraph> islands
  ) {
    int count = 0;
    for (int v = 0; v < newgraphs.length; v++) {
      if (!vertexIndex.isStreetVertex(v)) {
        continue;
      }
      if (anchors != null && anchors[v] == NO_SUBGRAPH) {
        // do not start new graph generation from non-classified vertex
        continue;
      }
      // already processed
      if (newgraphs[v] != NO_SUBGRAPH) {
        continue;
      }
      if (!neighbours.hasNeighbours(v)) {
        continue;
      }
      Subgraph subgraph = computeConnectedSubgraph(neighbours, v, anchors, newgraphs);
      if (islands != null) {
        islands.add(subgraph);
      }
      count++;
    }
    return count;
  }

  /**
   * Find the subgraph connected to {@code startVertex}, which may not enter a vertex already
   * mapped in {@code alreadyMapped} or, if {@code anchors} is given, belonging to a different
   * anchor subgraph than the start vertex. Stop vertices are not marked in {@code alreadyMapped},
   * so they can be part of several subgraphs.
   */
  private Subgraph computeConnectedSubgraph(
    Adjacency neighbours,
    int startVertex,
    @Nullable int[] anchors,
    int[] alreadyMapped
  ) {
    int subgraphId = nextSubgraphId++;
    int visited = ++searchId;
    Subgraph subgraph = new Subgraph(vertexIndex);
    int anchor = anchors == null ? NO_SUBGRAPH : anchors[startVertex];

    int head = 0;
    int tail = 0;
    queue[tail++] = startVertex;
    while (head < tail) {
      int vertex = queue[head++];
      for (int i = neighbours.firstNeighbour(vertex); i < neighbours.endNeighbour(vertex); i++) {
        int neighbour = neighbours.neighbour(i);
        if (visitedBy[neighbour] == visited || alreadyMapped[neighbour] != NO_SUBGRAPH) {
          continue;
        }
        if (anchor != NO_SUBGRAPH) {
          int compare = anchors[neighbour];
          if (compare != NO_SUBGRAPH && compare != anchor) {
            // do not enter a new island
            continue;
          }
        }
        visitedBy[neighbour] = visited;
        if (!vertexIndex.isStopVertex(neighbour)) {
          alreadyMapped[neighbour] = subgraphId;
        }
        subgraph.addVertex(neighbour);
        queue[tail++] = neighbour;
      }
    }
    return subgraph;
  }
}
