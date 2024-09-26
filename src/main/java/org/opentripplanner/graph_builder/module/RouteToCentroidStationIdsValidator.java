package org.opentripplanner.graph_builder.module;

import java.util.Collection;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class RouteToCentroidStationIdsValidator implements GraphBuilderModule {

  private final DataImportIssueStore issueStore;
  private final Collection<FeedScopedId> transitRouteToStationCentroid;
  private final Collection<FeedScopedId> stationIds;

  public RouteToCentroidStationIdsValidator(
    DataImportIssueStore issueStore,
    Collection<FeedScopedId> transitRouteToStationCentroid,
    Collection<FeedScopedId> stationIds
  ) {
    this.issueStore = issueStore;
    this.transitRouteToStationCentroid = transitRouteToStationCentroid;
    this.stationIds = stationIds;
  }

  private void validate() {
    transitRouteToStationCentroid
      .stream()
      .filter(id -> !stationIds.contains(id))
      .forEach(id ->
        issueStore.add(
          "UnknownStationId",
          "Config parameter 'transitRouteToStationCentroid' specified a station that does not exist: %s",
          id
        )
      );
  }

  @Override
  public void buildGraph() {
    validate();
  }
}
