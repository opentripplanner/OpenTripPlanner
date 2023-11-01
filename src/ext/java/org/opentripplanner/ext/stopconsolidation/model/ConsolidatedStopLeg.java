package org.opentripplanner.ext.stopconsolidation.model;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.ScheduledTransitLegBuilder;

public class ConsolidatedStopLeg extends ScheduledTransitLeg {

  private final I18NString fromName;
  private final I18NString toName;

  public ConsolidatedStopLeg(ScheduledTransitLeg original, I18NString fromName, I18NString toName) {
    super(new ScheduledTransitLegBuilder<>(original));
    this.fromName = fromName;
    this.toName = toName;
  }

  @Override
  public Place getFrom() {
    return Place.forStop(super.getFrom().stop, fromName);
  }

  @Override
  public Place getTo() {
    return Place.forStop(super.getTo().stop, toName);
  }
}
