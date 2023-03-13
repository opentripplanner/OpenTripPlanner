package org.opentripplanner.ext.carhailing.model;

import java.time.Duration;
import org.opentripplanner.transit.model.basic.Money;

public record CarHailingRide(
  String displayName,
  String productId,
  Money minPrice,
  Money maxPrice,
  Duration estimatedArrival
) {}
