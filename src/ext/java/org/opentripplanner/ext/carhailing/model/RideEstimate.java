package org.opentripplanner.ext.carhailing.model;

import java.time.Duration;
import org.opentripplanner.transit.model.basic.Money;

/**
 * A class to model the estimated ride time while using service from a car hailing
 * company
 */
public record RideEstimate(
  CarHailingProvider provider,
  Duration duration,
  Money minCost,
  Money maxCost,
  String rideType,
  boolean wheelchairAccessible
) {}
