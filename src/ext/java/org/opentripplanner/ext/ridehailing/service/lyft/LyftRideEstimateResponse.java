package org.opentripplanner.ext.ridehailing.service.lyft;

import java.util.List;

public record LyftRideEstimateResponse(List<LyftRideEstimate> cost_estimates) {
  public static record LyftRideEstimate(

    String currency,
    int estimated_cost_cents_max,
    int estimated_cost_cents_min,
    int estimated_duration_seconds,
    String ride_type
  )
  {}
}
