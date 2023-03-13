package org.opentripplanner.ext.carhailing.service.model;

import java.time.Duration;
import org.opentripplanner.transit.model.basic.Money;

/**
 * A class to model the estimated ride time while using service from a car hailing
 * company
 */
public record RideEstimate(
  CarHailingCompany company,
  Duration duration,
  Money maxCost,
  Money minCost,
  String rideType,
  boolean wheelchairAccessible
) {}
