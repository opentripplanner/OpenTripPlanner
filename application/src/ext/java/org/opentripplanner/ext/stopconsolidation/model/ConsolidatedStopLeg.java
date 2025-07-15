package org.opentripplanner.ext.stopconsolidation.model;

import java.util.Objects;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLegBuilder;

public class ConsolidatedStopLeg extends ScheduledTransitLeg {

  private final Place from;
  private final Place to;

  ConsolidatedStopLeg(ConsolidatedStopLegBuilder builder) {
    super(builder);
    this.from = Objects.requireNonNull(builder.from());
    this.to = Objects.requireNonNull(builder.to());
  }

  public static ConsolidatedStopLegBuilder of(ScheduledTransitLeg stl) {
    return new ConsolidatedStopLegBuilder(stl);
  }

  @Override
  public ScheduledTransitLegBuilder copyOf() {
    return new ConsolidatedStopLegBuilder(this);
  }

  @Override
  public Place from() {
    return from;
  }

  @Override
  public Place to() {
    return to;
  }
}
