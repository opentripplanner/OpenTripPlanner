package org.opentripplanner.graph_builder.module;

import java.util.Collection;
import java.util.stream.Collectors;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitModel;

public class RouteToCentroidStationIdsValidator implements GraphBuilderModule {

  private final DataImportIssueStore issueStore;
  private final Collection<FeedScopedId> transitRouteToStationCentroid;
  private final TransitModel transitModel;

  public RouteToCentroidStationIdsValidator(
    DataImportIssueStore issueStore,
    Collection<FeedScopedId> transitRouteToStationCentroid,
    TransitModel transitModel
  ) {
    this.issueStore = issueStore;
    this.transitRouteToStationCentroid = transitRouteToStationCentroid;
    this.transitModel = transitModel;
  }

  private void validate() {
    var stationIds = transitModel
      .getStopModel()
      .listStations()
      .stream()
      .map(AbstractTransitEntity::getId)
      .collect(Collectors.toSet());
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
