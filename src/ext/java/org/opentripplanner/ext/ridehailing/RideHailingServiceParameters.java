package org.opentripplanner.ext.ridehailing;

public sealed interface RideHailingServiceParameters {
  record UberServiceParameters(
    String clientId,
    String clientSecret,
    String wheelchairAccessibleRideType
  )
    implements RideHailingServiceParameters {}
}
