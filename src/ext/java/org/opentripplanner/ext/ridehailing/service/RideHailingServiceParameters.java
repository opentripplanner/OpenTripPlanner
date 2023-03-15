package org.opentripplanner.ext.ridehailing.service;

public sealed interface RideHailingServiceParameters {
  record UberServiceParameters(
    String clientId,
    String clientSecret,
    String wheelchairAccessibleRideType
  )
    implements RideHailingServiceParameters {}

  record LyftServiceParameters(
    String clientId,
    String clientSecret,
    String wheelchairAccessibleRideType
  )
    implements RideHailingServiceParameters {}
}
