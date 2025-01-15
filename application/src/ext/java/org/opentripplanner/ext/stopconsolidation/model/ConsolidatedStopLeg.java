package org.opentripplanner.ext.stopconsolidation.model;

import java.util.Objects;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.ScheduledTransitLegBuilder;
import org.opentripplanner.transit.model.site.StopLocation;

public class ConsolidatedStopLeg extends ScheduledTransitLeg {

  private final Place from;
  private final Place to;
  private final ScheduledTransitLeg original;

  ConsolidatedStopLeg(ConsolidatedStopLegBuilder builder) {
    super(builder);
    this.from = Objects.requireNonNull(builder.from());
    this.to = Objects.requireNonNull(builder.to());
    this.original = Objects.requireNonNull(builder.original());
  }

  @Override
  public Place getFrom() {
    return from;
  }

  @Override
  public Place getTo() {
    return to;
  }

  @Override
  public ScheduledTransitLegBuilder copy() {
    return new ConsolidatedStopLegBuilder(this);
  }

  ScheduledTransitLeg original() {
    return original;
  }
}
