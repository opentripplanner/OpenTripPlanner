package org.opentripplanner.ext.flex;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.routing.alertpatch.TransitAlert;

public class FlexibleTransitLegBuilder {

  private FlexTripEdge flexTripEdge;
  private ZonedDateTime startTime;
  private ZonedDateTime endTime;
  private int generalizedCost;
  private Set<TransitAlert> transitAlerts = new HashSet<>();
  private List<FareProductUse> fareProducts = new ArrayList<>();

  FlexibleTransitLegBuilder() {}

  FlexibleTransitLegBuilder(FlexibleTransitLeg original) {
    flexTripEdge = original.flexTripEdge();
    startTime = original.startTime();
    endTime = original.endTime();
    generalizedCost = original.generalizedCost();
    transitAlerts = original.listTransitAlerts();
    fareProducts = original.fareProducts();
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

  public Set<TransitAlert> alerts() {
    return transitAlerts;
  }

  public FlexibleTransitLegBuilder withFareProducts(List<FareProductUse> allUses) {
    this.fareProducts = List.copyOf(allUses);
    return this;
  }

  public List<FareProductUse> fareProducts() {
    return fareProducts;
  }

  public FlexibleTransitLeg build() {
    return new FlexibleTransitLeg(this);
  }
}
