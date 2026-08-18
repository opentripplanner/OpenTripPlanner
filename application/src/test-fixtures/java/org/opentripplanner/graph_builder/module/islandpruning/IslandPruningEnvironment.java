package org.opentripplanner.graph_builder.module.islandpruning;

import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.graph.summary.GraphSummarizer;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Builds a {@link Graph} from a set of already-constructed vertices (and their connecting edges),
 * and runs {@link IslandPruningModule} against it.
 */
public class IslandPruningEnvironment {

  private final Graph graph;

  private IslandPruningEnvironment(Graph graph) {
    this.graph = graph;
  }

  public static IslandPruningEnvironment of(Vertex... vertices) {
    var graph = new Graph();
    for (Vertex vertex : vertices) {
      graph.addVertex(vertex);
    }
    return new IslandPruningEnvironment(graph);
  }

  /**
   * Runs {@link IslandPruningModule} with the given parameters and returns a summarizer over the
   * resulting graph.
   */
  public GraphSummarizer prune(IslandPruningParameters parameters) {
    IslandPruningUtils.prune(graph, parameters);
    return new GraphSummarizer(graph);
  }
}
