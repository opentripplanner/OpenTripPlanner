package org.opentripplanner.ext.emission.internal.csvdata.trip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ext.emission.model.TripPatternEmission;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

public class TripLegMapper {

  private String currentFeedId;
  private Map<FeedScopedId, List<StopLocation>> stopsByTripId;
  private DataImportIssueStore issueStore;

  public TripLegMapper(
    Map<FeedScopedId, List<StopLocation>> stopsByTripId,
    DataImportIssueStore issueStore
  ) {
    this.stopsByTripId = stopsByTripId;
    this.issueStore = issueStore;
  }

  public void setCurrentFeedId(String currentFeedId) {
    this.currentFeedId = currentFeedId;
  }

  public Map<FeedScopedId, TripPatternEmission> map(List<TripLegsRow> rows) {
    if (currentFeedId == null) {
      throw new IllegalStateException("currentFeedId is not set");
    }

    Map<FeedScopedId, EmissionAggregator> builders = new HashMap<>();

    for (TripLegsRow row : rows) {
      var tripId = new FeedScopedId(currentFeedId, row.tripId());
      var b = builders.computeIfAbsent(tripId, id ->
        new EmissionAggregator(tripId, stopsByTripId.get(tripId))
      );
      b.mergeEmissionForleg(row);
    }

    var map = new HashMap<FeedScopedId, TripPatternEmission>();

    for (Map.Entry<FeedScopedId, EmissionAggregator> it : builders.entrySet()) {
      var aggregator = it.getValue();
      if (aggregator.validate()) {
        map.put(it.getKey(), aggregator.build());
      } else {
        aggregator.listIssues().forEach(issueStore::add);
      }
    }
    return map;
  }
}
