package org.opentripplanner.ext.ridehailing.model;

import java.time.Duration;

/**
 * Model of estimated arrival times of service from a ride hailing company
 * @param duration The estimated time it takes for the vehicle to arrive.
 */
public record ArrivalTime(
  RideHailingProvider provider,
  String rideType,
  String displayName,
  Duration duration
)
  implements Ride {}
