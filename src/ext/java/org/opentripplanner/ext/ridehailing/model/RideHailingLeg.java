package org.opentripplanner.ext.ridehailing.model;

import javax.annotation.Nullable;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.StreetLegBuilder;

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

  @Nullable
  public RideEstimate rideEstimate() {
    return estimate;
  }
}
