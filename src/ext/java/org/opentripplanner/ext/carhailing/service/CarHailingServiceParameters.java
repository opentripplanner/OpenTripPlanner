package org.opentripplanner.ext.carhailing.service;

public sealed interface CarHailingServiceParameters {
  record UberServiceParameters(
    String clientId,
    String clientSecret,
    String wheelchairAccessibleRideType
  )
    implements CarHailingServiceParameters {}

  record LyftServiceParameters(
    String clientId,
    String clientSecret,
    String wheelchairAccessibleRideType
  )
    implements CarHailingServiceParameters {}
}
