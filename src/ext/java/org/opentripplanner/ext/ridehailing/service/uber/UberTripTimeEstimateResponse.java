package org.opentripplanner.ext.ridehailing.service.uber;

import java.util.List;

public record UberTripTimeEstimateResponse(List<UberTripTimeEstimate> prices) {
  public static class UberTripTimeEstimate {

    public String currency_code;
    public int duration;
    public int high_estimate;
    public int low_estimate;
    public String product_id;
    public String display_name;
  }
}
