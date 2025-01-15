package org.opentripplanner.ext.stopconsolidation.model;

import java.util.Objects;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.ScheduledTransitLegBuilder;
import org.opentripplanner.transit.model.site.StopLocation;

public class ConsolidatedStopLegBuilder extends ScheduledTransitLegBuilder<ConsolidatedStopLegBuilder> {
  private final ScheduledTransitLeg original;
  private Place from;
  private Place to;

  public ConsolidatedStopLegBuilder(ScheduledTransitLeg leg) {
    super(leg);
    original = Objects.requireNonNull(leg);
  }

  public ConsolidatedStopLegBuilder(ConsolidatedStopLeg consolidatedStopLeg) {
    super(consolidatedStopLeg);
    this.original = consolidatedStopLeg.original();
    this.from = consolidatedStopLeg.getFrom();
    this.to = consolidatedStopLeg.getTo();
  }

  public ScheduledTransitLeg original() {
    return original;
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