package org.opentripplanner.ext.ridehailing.service.lyft;

import java.util.List;

public record LyftArrivalEstimateResponse(List<LyftArrivalEstimate> eta_estimates){
  public record LyftArrivalEstimate(int eta_seconds, String display_name, String ride_type) {}
}
