package org.opentripplanner.ext.carhailing.service.uber;

public record UberServiceParameters(
  String clientId,
  String clientSecret,
  String wheelchairAccessibleRideType
) {}
