package org.opentripplanner.ext.ridehailing.service.uber;

import java.util.List;

public record UberTripTimeEstimateResponse(List<UberTripTimeEstimate> prices) {
  public record UberTripTimeEstimate(
    String currency_code,
    int duration,
    int high_estimate,
    int low_estimate,
    String product_id,
    String display_name
  ) {}
}
