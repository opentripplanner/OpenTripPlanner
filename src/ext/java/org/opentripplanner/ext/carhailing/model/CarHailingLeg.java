package org.opentripplanner.ext.carhailing.model;

import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.StreetLegBuilder;

public class CarHailingLeg extends StreetLeg {

  private final CarHailingRide ride;
  private final CarHailingProvider company;

  public CarHailingLeg(StreetLeg streetLeg, CarHailingProvider company, CarHailingRide ride) {
    super(StreetLegBuilder.of(streetLeg));
    this.company = company;
    this.ride = ride;
  }
}
