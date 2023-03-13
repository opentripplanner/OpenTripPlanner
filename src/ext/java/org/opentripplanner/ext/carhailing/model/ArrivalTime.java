package org.opentripplanner.ext.carhailing.model;

import java.time.Duration;

// A class to model estimated arrival times of service from a car hailing company
public record ArrivalTime(
  CarHailingProvider provider,
  String productId,
  String displayName,
  Duration estimatedDuration,
  boolean wheelchairAccessible
) {}
