package org.opentripplanner.ext.empiricaldelay.internal.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ext.empiricaldelay.model.EmpiricalDelay;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TripDelaysDto {

  private final FeedScopedId tripId;
  private final Map<String, List<DelayAtStopDto>> delaysForServiceId = new HashMap<>();

  public TripDelaysDto(FeedScopedId tripId) {
    this.tripId = tripId;
  }

  public FeedScopedId tripId() {
    return tripId;
  }

  public void addDelay(
    String calendarServiceId,
    int sequence,
    FeedScopedId stopId,
    EmpiricalDelay empiricalDelay
  ) {
    var list = delaysForServiceId.computeIfAbsent(calendarServiceId, id -> new ArrayList<>());
    list.add(new DelayAtStopDto(sequence, stopId, empiricalDelay));
  }

  public Iterable<String> serviceIds() {
    return delaysForServiceId.keySet();
  }

  /**
   * Note! The list is sorted on sequence number before it is returned.
   */
  public List<DelayAtStopDto> delaysSortedForServiceId(String serviceId) {
    return delaysForServiceId
      .get(serviceId)
      .stream()
      .sorted(Comparator.comparingInt(DelayAtStopDto::sequence))
      .toList();
  }
}
