package org.opentripplanner.graph_builder.module.stopconnectivity;

import java.time.Duration;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.astar.strategy.DurationTerminationStrategy;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.IsolatedStop;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopConnectivityModule implements GraphBuilderModule {
  public static final Duration DURATION = Duration.ofMinutes(10);
  private final Graph graph;
  private final DataImportIssueStore issueStore;


  private static final Logger LOG = LoggerFactory.getLogger(StopConnectivityModule.class);

  public StopConnectivityModule(Graph graph, DataImportIssueStore issueStore) {
    this.graph = graph;
    this.issueStore = issueStore;
  }

  @Override
  public void buildGraph() {
    var progress = ProgressTracker.track("Stop connectivity analysis", 5000, graph.getVerticesOfType(TransitStopVertex.class).size());
    LOG.info(progress.startMessage());
    var issues = graph
      .getVerticesOfType(TransitStopVertex.class)
      .parallelStream()
      .map(stop -> {
        if(stop.isFerryStop()){
          return checkFerryStop(stop, progress);
        }
        else {
          return checkWalkingConnection(stop, progress);
        }
      })
      .filter(Objects::nonNull)
      .toList();

    issues.forEach(issueStore::add);

    LOG.info(progress.completeMessage());
  }

  private static IsolatedStop checkFerryStop(TransitStopVertex stop, ProgressTracker progress) {
    progress.step(i -> LOG.info(i));
    if(stop.isConnectedToGraph()){
      return null;
    }
    else {
      return new IsolatedStop(stop);
    }
  }

  @Nullable
  private static IsolatedStop checkWalkingConnection(TransitStopVertex stop, ProgressTracker progress) {
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

    progress.step(i -> LOG.info(i));
    if (isIsolated) {
      return new IsolatedStop(stop);
    } else {
      return null;
    }
  }
}
