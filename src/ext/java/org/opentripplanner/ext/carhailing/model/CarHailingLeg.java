package org.opentripplanner.ext.carhailing.model;

import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.StreetLegBuilder;

public class CarHailingLeg extends StreetLeg {

  private final RideEstimate ride;
  private final CarHailingProvider provider;

  public CarHailingLeg(StreetLeg streetLeg, CarHailingProvider provider, RideEstimate ride) {
    super(StreetLegBuilder.of(streetLeg));
    this.provider = provider;
    this.ride = ride;
  }

  public RideEstimate ride() {
    return ride;
  }

  public CarHailingProvider provider() {
    return provider;
  }
}
