package org.opentripplanner.ext.ridehailing;

/**
 * Interface and concrete configurations for ride hailing services.
 */
public sealed interface RideHailingServiceParameters {
  record UberServiceParameters(
    String clientId,
    String clientSecret,
    String wheelchairAccessibleRideType
  )
    implements RideHailingServiceParameters {}
}
