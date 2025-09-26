package org.opentripplanner.ext.empiricaldelay.internal.graphbuilder;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.ext.empiricaldelay.internal.model.DelayAtStopDto;
import org.opentripplanner.ext.empiricaldelay.internal.model.TripDelaysDto;
import org.opentripplanner.ext.empiricaldelay.model.EmpiricalDelay;
import org.opentripplanner.ext.empiricaldelay.model.TripDelays;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class TripDelaysMapper {

  private final Map<FeedScopedId, List<FeedScopedId>> stopIdsByTripId;
  private final DataImportIssueStore issueStore;
  private final Deduplicator deduplicator;

  TripDelaysMapper(
    Map<FeedScopedId, List<FeedScopedId>> stopIdsByTripId,
    DataImportIssueStore issueStore,
    Deduplicator deduplicator
  ) {
    this.stopIdsByTripId = stopIdsByTripId;
    this.issueStore = issueStore;
    this.deduplicator = deduplicator;
  }

  Optional<TripDelays> map(TripDelaysDto trip) {
    var builder = TripDelays.of(trip.tripId());
    for (String serviceId : trip.serviceIds()) {
      var delayAtStops = trip.delaysSortedForServiceId(serviceId);
      var delays = map(trip.tripId(), serviceId, delayAtStops);
      if (delays != null) {
        builder.with(serviceId, delays);
      }
    }
    return Optional.ofNullable(builder.build());
  }

  @Nullable
  private TIntObjectHashMap<EmpiricalDelay> map(
    FeedScopedId tripId,
    String serviceId,
    List<DelayAtStopDto> delayDtos
  ) {
    var map = new TIntObjectHashMap<EmpiricalDelay>();
    var stopIds = stopIdsByTripId.get(tripId);

    if (stopIds == null) {
      addDelayTripNotFoundIssue(tripId, serviceId);
      return null;
    }
    int i = 0;
    var skippedStops = new ArrayList<FeedScopedId>();

    for (DelayAtStopDto delay : delayDtos) {
      while (i < stopIds.size() && !delay.stopId().equals(stopIds.get(i))) {
        skippedStops.add(stopIds.get(i));
        ++i;
      }
      if (i == stopIds.size()) {
        addStopMissmatchIssue(tripId, serviceId, delay);
        return null;
      }
      var value = deduplicator.deduplicateObject(EmpiricalDelay.class, delay.empiricalDelay());
      map.put(i, value);
      ++i;
    }
    // Add skiped stops at the end
    for (; i < stopIds.size(); ++i) {
      skippedStops.add(stopIds.get(i));
    }

    if (!skippedStops.isEmpty()) {
      addSkippedStopIssue(tripId, serviceId, skippedStops);
    }

    deduplicator.deduplicateObject(TIntObjectHashMap.class, map);
    return map;
  }

  private void addDelayTripNotFoundIssue(FeedScopedId tripId, String serviceId) {
    issueStore.add(
      "EmpiricalDelayTripNotFound",
      "Trip pattern not found for trip. Trip: %s, ServiceId: %s.",
      tripId,
      serviceId
    );
  }

  private void addStopMissmatchIssue(FeedScopedId tripId, String serviceId, DelayAtStopDto delay) {
    issueStore.add(
      "EmpiricalDelayStopMissmatch",
      "The stop sequence is wrong or the stop is not in the trip pattern. TripId: %s, ServiceId: %s, delay: %s",
      tripId,
      serviceId,
      delay
    );
  }

  private void addSkippedStopIssue(
    FeedScopedId tripId,
    String serviceId,
    List<FeedScopedId> stops
  ) {
    issueStore.add(
      "EmpiricalDelayMissingStops",
      "There is no empirical delay data for listed stops. Trip: %s, ServiceId: %s, Missing stops: %s",
      tripId,
      serviceId,
      stops
    );
  }
}
