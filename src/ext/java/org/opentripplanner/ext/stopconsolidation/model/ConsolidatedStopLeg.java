package org.opentripplanner.ext.stopconsolidation.model;

import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.ScheduledTransitLegBuilder;
import org.opentripplanner.transit.model.site.StopLocation;

public class ConsolidatedStopLeg extends ScheduledTransitLeg {

  private final StopLocation fromName;
  private final StopLocation toName;

  public ConsolidatedStopLeg(ScheduledTransitLeg original, StopLocation fromName, StopLocation toName) {
    super(new ScheduledTransitLegBuilder<>(original));
    this.fromName= fromName;
    this.toName = toName;
  }

  @Override
  public Place getFrom() {
    return Place.forStop(fromName);
  }

  @Override
  public Place getTo() {
    return Place.forStop(toName);
  }
}
