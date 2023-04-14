package org.opentripplanner.ext.ridehailing.service.uber;

import java.util.List;

public record UberArrivalEstimateResponse(List<UberArrivalEstimate> times) {
  public record UberArrivalEstimate(
    String display_name,
    int estimate,
    String localized_display_name,
    String product_id
  ) {}
}
