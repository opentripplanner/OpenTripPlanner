package org.opentripplanner.ext.flex;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.routing.alertpatch.TransitAlert;

public class FlexibleTransitLegBuilder {
  private FlexTripEdge flexTripEdge;
  private ZonedDateTime startTime;
  private ZonedDateTime endTime;
  private int generalizedCost;
  private Set<TransitAlert> transitAlerts = new HashSet<>();

  public FlexibleTransitLegBuilder() {}

  public FlexibleTransitLegBuilder(FlexibleTransitLeg original) {
    flexTripEdge = original.flexTripEdge();
    startTime = original.getStartTime();
    endTime = original.getEndTime();
    generalizedCost = original.getGeneralizedCost();
    transitAlerts = new LinkedHashSet<>(original.getTransitAlerts());
  }


  public FlexibleTransitLegBuilder withFlexTripEdge(FlexTripEdge flexTripEdge) {
    this.flexTripEdge = flexTripEdge;
    return this;
  }
  public FlexTripEdge flexTripEdge() {
    return flexTripEdge;
  }

  public FlexibleTransitLegBuilder withStartTime(ZonedDateTime startTime) {
    this.startTime = startTime;
    return this;
  }

  public ZonedDateTime startTime() {
    return startTime;
  }

  public FlexibleTransitLegBuilder withEndTime(ZonedDateTime endTime) {
    this.endTime = endTime;
    return this;
  }

  public ZonedDateTime endTime() {
    return endTime;
  }

  public FlexibleTransitLegBuilder withGeneralizedCost(int generalizedCost) {
    this.generalizedCost = generalizedCost;
    return this;
  }

  public int generalizedCost() {
    return generalizedCost;
  }

  public FlexibleTransitLegBuilder withAlerts(Collection<TransitAlert> alerts) {
    this.transitAlerts = Set.copyOf(alerts);
    return this;
  }

  public Set<TransitAlert> transitAlerts() {
    return transitAlerts;
  }

  public FlexibleTransitLeg build() {
    return new FlexibleTransitLeg(this);
  }


}