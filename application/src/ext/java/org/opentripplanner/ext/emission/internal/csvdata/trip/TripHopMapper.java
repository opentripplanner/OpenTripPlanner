package org.opentripplanner.ext.emission.internal.csvdata.trip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ext.emission.model.TripPatternEmission;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

public class TripHopMapper {

  private String currentFeedId;
  private Map<FeedScopedId, List<StopLocation>> stopsByTripId;
  private DataImportIssueStore issueStore;

  public TripHopMapper(
    Map<FeedScopedId, List<StopLocation>> stopsByTripId,
    DataImportIssueStore issueStore
  ) {
    this.stopsByTripId = stopsByTripId;
    this.issueStore = issueStore;
  }

  public void setCurrentFeedId(String currentFeedId) {
    this.currentFeedId = currentFeedId;
  }

  public Map<FeedScopedId, TripPatternEmission> map(List<TripHopsRow> rows) {
    if (currentFeedId == null) {
      throw new IllegalStateException("currentFeedId is not set");
    }

    Map<FeedScopedId, EmissionAggregator> builders = new HashMap<>();

    for (TripHopsRow row : rows) {
      var tripId = new FeedScopedId(currentFeedId, row.tripId());
      var b = builders.computeIfAbsent(tripId, id ->
        new EmissionAggregator(tripId, stopsByTripId.get(tripId))
      );
      b.mergeEmissionsForHop(row);
    }

    var map = new HashMap<FeedScopedId, TripPatternEmission>();

    for (Map.Entry<FeedScopedId, EmissionAggregator> it : builders.entrySet()) {
      var builder = it.getValue();
      var emission = builder.build();
      if (emission.isPresent()) {
        map.put(it.getKey(), emission.get());
      } else {
        issueStore.addAll(builder.listIssues());
      }
    }
    return map;
  }
}
