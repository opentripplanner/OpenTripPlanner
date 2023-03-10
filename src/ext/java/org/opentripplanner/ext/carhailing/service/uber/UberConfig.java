package org.opentripplanner.ext.carhailing.service.uber;

public record UberConfig(
  String clientId,
  String clientSecret,
  String wheelchairAccessibleRideType
) {}
