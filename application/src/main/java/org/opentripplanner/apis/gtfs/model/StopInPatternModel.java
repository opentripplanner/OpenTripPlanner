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
) {}
