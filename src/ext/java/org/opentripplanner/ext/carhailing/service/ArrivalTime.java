package org.opentripplanner.ext.carhailing.service;

import java.time.Duration;

// A class to model estimated arrival times of service from a car hailing company
public record ArrivalTime(
  CarHailingCompany company,
  String productId,
  String displayName,
  Duration estimatedSeconds,
  boolean wheelchairAccessible
) {}
