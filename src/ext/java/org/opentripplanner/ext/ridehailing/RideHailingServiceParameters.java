package org.opentripplanner.ext.ridehailing;

/**
 * Interface and concrete configurations for ride hailing services.
 */
public record RideHailingServiceParameters(
  String clientId,
  String clientSecret,
  String wheelchairAccessibleRideType
) {}
