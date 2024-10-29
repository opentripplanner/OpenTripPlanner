package org.opentripplanner.apis.gtfs.model;

import org.opentripplanner.model.PickDrop;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;

public record StopInPatternModel(
  StopLocation stop,
  TripPattern pattern,
  int indexInPattern,
  PickDrop pickupType,
  PickDrop dropoffType
) {
  public static StopInPatternModel fromPatternAndIndex(TripPattern pattern, int indexInPattern) {
    return new StopInPatternModel(
      pattern.getStop(indexInPattern),
      pattern,
      indexInPattern,
      pattern.getBoardType(indexInPattern),
      pattern.getAlightType(indexInPattern)
    );
  }
}
