package org.opentripplanner.graph_builder.module.stopconnectivity;

import java.time.Duration;
import java.util.Objects;
import org.opentripplanner.astar.strategy.DurationTerminationStrategy;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.IsolatedStop;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;

public class StopConnectivityModule implements GraphBuilderModule {
  public static final Duration DURATION = Duration.ofMinutes(10);
  private final Graph graph;
  private final DataImportIssueStore issueStore;

  public StopConnectivityModule(Graph graph, DataImportIssueStore issueStore) {
    this.graph = graph;
    this.issueStore = issueStore;
  }

  @Override
  public void buildGraph() {

    var issues = graph
      .getVerticesOfType(TransitStopVertex.class)
      .parallelStream()
      .map(stop -> {
        var spt = StreetSearchBuilder.of()
          .withPreStartHook(() -> {})
          .withRequest(StreetSearchRequest.of().withMode(StreetMode.WALK).build())
          .withFrom(stop)
          .withTerminationStrategy(new DurationTerminationStrategy(DURATION.plusMinutes(1)))
          .getShortestPathTree();

        var isIsolated = spt
          .getAllStates()
          .stream()
          .noneMatch(s -> s.getElapsedTimeSeconds() > DURATION.toSeconds());

        if (isIsolated) {
          return new IsolatedStop(stop);
        } else {
          return null;
        }
      })
      .filter(Objects::nonNull)
      .toList();

    issues.forEach(issueStore::add);
  }
}
