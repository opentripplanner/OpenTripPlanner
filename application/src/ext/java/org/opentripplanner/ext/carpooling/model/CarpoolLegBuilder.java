package org.opentripplanner.ext.carpooling.model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.alertpatch.TransitAlert;

public class CarpoolLegBuilder {

  private ZonedDateTime startTime;
  private ZonedDateTime endTime;
  private int generalizedCost;
  private Set<TransitAlert> transitAlerts = new HashSet<>();
  private List<FareProductUse> fareProducts = new ArrayList<>();
  private Emission emissionPerPerson;
  private Place from;
  private Place to;
  private LineString geometry;
  private double distanceMeters;

  CarpoolLegBuilder() {}

  CarpoolLegBuilder(CarpoolLeg original) {
    startTime = original.startTime();
    endTime = original.endTime();
    generalizedCost = original.generalizedCost();
    transitAlerts = original.listTransitAlerts();
    fareProducts = original.fareProducts();
    emissionPerPerson = original.emissionPerPerson();
    from = original.from();
    to = original.to();
    geometry = original.legGeometry();
    distanceMeters = original.distanceMeters();
  }

  public CarpoolLegBuilder withStartTime(ZonedDateTime startTime) {
    this.startTime = startTime;
    return this;
  }

  public ZonedDateTime startTime() {
    return startTime;
  }

  public CarpoolLegBuilder withEndTime(ZonedDateTime endTime) {
    this.endTime = endTime;
    return this;
  }

  public ZonedDateTime endTime() {
    return endTime;
  }

  public CarpoolLegBuilder withGeneralizedCost(int generalizedCost) {
    this.generalizedCost = generalizedCost;
    return this;
  }

  public int generalizedCost() {
    return generalizedCost;
  }

  public CarpoolLegBuilder withAlerts(Collection<TransitAlert> alerts) {
    this.transitAlerts = Set.copyOf(alerts);
    return this;
  }

  public Set<TransitAlert> alerts() {
    return transitAlerts;
  }

  public CarpoolLegBuilder withFareProducts(List<FareProductUse> allUses) {
    this.fareProducts = List.copyOf(allUses);
    return this;
  }

  public List<FareProductUse> fareProducts() {
    return fareProducts;
  }

  public CarpoolLegBuilder withEmissionPerPerson(Emission emissionPerPerson) {
    this.emissionPerPerson = emissionPerPerson;
    return this;
  }

  public Emission emissionPerPerson() {
    return emissionPerPerson;
  }

  public CarpoolLegBuilder withFrom(Place from) {
    this.from = from;
    return this;
  }

  public Place from() {
    return from;
  }

  public CarpoolLegBuilder withTo(Place to) {
    this.to = to;
    return this;
  }

  public Place to() {
    return to;
  }

  public CarpoolLegBuilder withGeometry(LineString geometry) {
    this.geometry = geometry;
    return this;
  }

  public LineString geometry() {
    return geometry;
  }

  public CarpoolLegBuilder withDistanceMeters(double distanceMeters) {
    this.distanceMeters = distanceMeters;
    return this;
  }

  public double distanceMeters() {
    return distanceMeters;
  }

  public CarpoolLeg build() {
    return new CarpoolLeg(this);
  }
}
