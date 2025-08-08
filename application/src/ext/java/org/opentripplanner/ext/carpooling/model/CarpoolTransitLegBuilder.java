package org.opentripplanner.ext.carpooling.model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.routing.alertpatch.TransitAlert;

public class CarpoolTransitLegBuilder {

  private ZonedDateTime startTime;
  private ZonedDateTime endTime;
  private int generalizedCost;
  private Set<TransitAlert> transitAlerts = new HashSet<>();
  private List<FareProductUse> fareProducts = new ArrayList<>();
  private Emission emissionPerPerson;

  CarpoolTransitLegBuilder() {}

  CarpoolTransitLegBuilder(CarpoolTransitLeg original) {
    startTime = original.startTime();
    endTime = original.endTime();
    generalizedCost = original.generalizedCost();
    transitAlerts = original.listTransitAlerts();
    fareProducts = original.fareProducts();
    emissionPerPerson = original.emissionPerPerson();
  }

  public CarpoolTransitLegBuilder withStartTime(ZonedDateTime startTime) {
    this.startTime = startTime;
    return this;
  }

  public ZonedDateTime startTime() {
    return startTime;
  }

  public CarpoolTransitLegBuilder withEndTime(ZonedDateTime endTime) {
    this.endTime = endTime;
    return this;
  }

  public ZonedDateTime endTime() {
    return endTime;
  }

  public CarpoolTransitLegBuilder withGeneralizedCost(int generalizedCost) {
    this.generalizedCost = generalizedCost;
    return this;
  }

  public int generalizedCost() {
    return generalizedCost;
  }

  public CarpoolTransitLegBuilder withAlerts(Collection<TransitAlert> alerts) {
    this.transitAlerts = Set.copyOf(alerts);
    return this;
  }

  public Set<TransitAlert> alerts() {
    return transitAlerts;
  }

  public CarpoolTransitLegBuilder withFareProducts(List<FareProductUse> allUses) {
    this.fareProducts = List.copyOf(allUses);
    return this;
  }

  public List<FareProductUse> fareProducts() {
    return fareProducts;
  }

  public CarpoolTransitLegBuilder withEmissionPerPerson(Emission emissionPerPerson) {
    this.emissionPerPerson = emissionPerPerson;
    return this;
  }

  public Emission emissionPerPerson() {
    return emissionPerPerson;
  }

  public CarpoolTransitLeg build() {
    return new CarpoolTransitLeg(this);
  }
}
