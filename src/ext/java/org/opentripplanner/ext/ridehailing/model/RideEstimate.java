package org.opentripplanner.ext.ridehailing.model;

import java.time.Duration;
import org.opentripplanner.transit.model.basic.Money;

/**
 * A class to model the estimated ride time while using service from a car hailing
 * company
 * @param arrival The duration it takes for the vehicle to arrive.
 */
public record RideEstimate(
  RideHailingProvider provider,
  Duration arrival,
  Money minPrice,
  Money maxPrice,
  String rideType,
  String productName
)
  implements Ride {}
