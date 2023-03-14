package org.opentripplanner.ext.carhailing.model;

import javax.annotation.Nullable;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.StreetLegBuilder;

public class CarHailingLeg extends StreetLeg {

  private final RideEstimate estimate;
  private final CarHailingProvider provider;

  public CarHailingLeg(StreetLeg streetLeg, CarHailingProvider provider, RideEstimate estimate) {
    super(StreetLegBuilder.of(streetLeg));
    this.provider = provider;
    this.estimate = estimate;
  }

  public RideEstimate ride() {
    return estimate;
  }

  public CarHailingProvider provider() {
    return provider;
  }

  @Nullable
  @Override
  public RideEstimate carHailingEstimate() {
    return estimate;
  }
}
