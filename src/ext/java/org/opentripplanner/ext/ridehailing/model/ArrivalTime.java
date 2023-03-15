package org.opentripplanner.ext.ridehailing.model;

import java.time.Duration;

// A class to model estimated arrival times of service from a car hailing company
public record ArrivalTime(
  RideHailingProvider provider,
  String productId,
  String displayName,
  Duration estimatedDuration,
  boolean wheelchairAccessible
) {}
