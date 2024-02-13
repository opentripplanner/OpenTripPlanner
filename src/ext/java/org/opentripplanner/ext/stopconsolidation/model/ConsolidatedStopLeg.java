package org.opentripplanner.ext.stopconsolidation.model;

import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.ScheduledTransitLegBuilder;
import org.opentripplanner.transit.model.site.StopLocation;

public class ConsolidatedStopLeg extends ScheduledTransitLeg {

  private final StopLocation from;
  private final StopLocation to;

  public ConsolidatedStopLeg(ScheduledTransitLeg original, StopLocation from, StopLocation to) {
    super(new ScheduledTransitLegBuilder<>(original));
    this.from = from;
    this.to = to;
  }

  @Override
  public Place getFrom() {
    return Place.forStop(from);
  }

  @Override
  public Place getTo() {
    return Place.forStop(to);
  }
}
