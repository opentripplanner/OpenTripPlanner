package org.opentripplanner.ext.ridehailing.model;

import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.StreetLegBuilder;

/**
 * This is a special leg for ride hailing that adds information about the ride estimate like
 * price, the type of vehicle and arrival time.
 */
public class RideHailingLeg extends StreetLeg {

  private final RideEstimate estimate;
  private final RideHailingProvider provider;

  public RideHailingLeg(StreetLeg streetLeg, RideHailingProvider provider, RideEstimate estimate) {
    super(StreetLegBuilder.of(streetLeg));
    this.provider = provider;
    this.estimate = estimate;
  }

  public RideEstimate ride() {
    return estimate;
  }

  public RideHailingProvider provider() {
    return provider;
  }

  public RideEstimate rideEstimate() {
    return estimate;
  }
}
