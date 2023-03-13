package org.opentripplanner.ext.carhailing.service.uber;

import org.opentripplanner.ext.carhailing.service.CarHailingServiceParameters;

public record UberServiceParameters(
  String clientId,
  String clientSecret,
  String wheelchairAccessibleRideType
)
  implements CarHailingServiceParameters {}
