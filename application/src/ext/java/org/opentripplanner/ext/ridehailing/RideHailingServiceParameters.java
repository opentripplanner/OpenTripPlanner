package org.opentripplanner.ext.ridehailing;

import java.util.List;

/**
 * Configuration for ride hailing services.
 */
public record RideHailingServiceParameters(
  String clientId,
  String clientSecret,
  String wheelchairProductId,
  List<String> bannedProductIds
) {}
