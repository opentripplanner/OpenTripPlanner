package org.opentripplanner.graph_builder.module.islandpruning;

import com.carrotsearch.hppc.IntArrayList;
import java.util.PrimitiveIterator;

/**
 * Undirected vertex adjacency of the street graph, keyed on {@link VertexIndex} ids and stored in
 * compressed sparse row form: the neighbours of vertex {@code v} are
 * {@code targets[offsets[v]] .. targets[offsets[v + 1] - 1]}. Neighbours are not deduplicated,
 * which is fine for the breadth-first searches in {@link IslandPruningModule}.
 */
class Adjacency {

  private final int[] offsets;
  private final int[] targets;

  /**
   * @param vertexCount the number of vertex ids
   * @param edgeLists   lists of interleaved {@code (from, to)} id pairs; each pair is added in both
   *                    directions
   */
  Adjacency(int vertexCount, IntArrayList... edgeLists) {
    offsets = new int[vertexCount + 1];
    for (var edges : edgeLists) {
      for (int i = 0; i < edges.size(); i++) {
        offsets[edges.get(i) + 1]++;
      }
    }
    for (int v = 0; v < vertexCount; v++) {
      offsets[v + 1] += offsets[v];
    }
    targets = new int[offsets[vertexCount]];
    int[] next = new int[vertexCount];
    System.arraycopy(offsets, 0, next, 0, vertexCount);
    for (var edges : edgeLists) {
      for (int i = 0; i < edges.size(); i += 2) {
        int from = edges.get(i);
        int to = edges.get(i + 1);
        targets[next[from]++] = to;
        targets[next[to]++] = from;
      }
    }
  }

  boolean hasNeighbours(int v) {
    return offsets[v] != offsets[v + 1];
  }

  /** The ids of the vertices adjacent to vertex {@code v}. */
  PrimitiveIterator.OfInt neighbours(int v) {
    return new PrimitiveIterator.OfInt() {
      private int i = offsets[v];
      private final int end = offsets[v + 1];

      @Override
      public boolean hasNext() {
        return i < end;
      }

      @Override
      public int nextInt() {
        return targets[i++];
      }
    };
  }
}
