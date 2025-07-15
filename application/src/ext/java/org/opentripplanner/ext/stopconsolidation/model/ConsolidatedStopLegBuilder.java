package org.opentripplanner.ext.stopconsolidation.model;

import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLegBuilder;
import org.opentripplanner.transit.model.site.StopLocation;

public class ConsolidatedStopLegBuilder
  extends ScheduledTransitLegBuilder<ConsolidatedStopLegBuilder> {

  private Place from;
  private Place to;

  ConsolidatedStopLegBuilder(ScheduledTransitLeg leg) {
    super(leg);
  }

  ConsolidatedStopLegBuilder(ConsolidatedStopLeg consolidatedStopLeg) {
    super(consolidatedStopLeg);
    this.from = consolidatedStopLeg.from();
    this.to = consolidatedStopLeg.to();
  }

  public ConsolidatedStopLegBuilder withFrom(StopLocation stop) {
    this.from = Place.forStop(stop);
    return this;
  }

  public Place from() {
    return from;
  }

  public ConsolidatedStopLegBuilder withTo(StopLocation stop) {
    this.to = Place.forStop(stop);
    return this;
  }

  public Place to() {
    return to;
  }

  @Override
  public ConsolidatedStopLeg build() {
    return new ConsolidatedStopLeg(this);
  }
}
