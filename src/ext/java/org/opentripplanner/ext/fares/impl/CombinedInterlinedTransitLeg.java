package org.opentripplanner.ext.fares.impl;

import static org.opentripplanner.model.plan.Itinerary.UNKNOWN;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.FareZone;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * This is a fake leg that combines two interlined legs for the purpose of fare calculation.
 * <p>
 * We pretend that two legs are a single one so that you will not be charged twice.
 */
class CombinedInterlinedTransitLeg implements TransitLeg {

  private final TransitLeg first;
  private final TransitLeg second;

  public CombinedInterlinedTransitLeg(TransitLeg first, TransitLeg second) {
    this.first = first;
    this.second = second;
  }

  public Agency getAgency() {
    return first.getAgency();
  }

  @Nonnull
  @Override
  public TransitMode getMode() {
    return first.getMode();
  }

  @Nonnull
  @Override
  public Route getRoute() {
    return first.getRoute();
  }

  @Nonnull
  @Override
  public Trip getTrip() {
    return first.getTrip();
  }

  @Override
  public ZonedDateTime getStartTime() {
    return first.getStartTime();
  }

  @Override
  public ZonedDateTime getEndTime() {
    return second.getStartTime();
  }

  @Override
  public double getDistanceMeters() {
    return first.getDistanceMeters() + second.getDistanceMeters();
  }

  @Override
  public Place getFrom() {
    return first.getFrom();
  }

  @Override
  public Place getTo() {
    return second.getTo();
  }

  @Override
  public List<StopArrival> getIntermediateStops() {
    return ListUtils.combine(first.getIntermediateStops(), second.getIntermediateStops());
  }

  @Override
  public LineString getLegGeometry() {
    return null;
  }

  @Override
  public int getGeneralizedCost() {
    if (first.getGeneralizedCost() == UNKNOWN) {
      return second.getGeneralizedCost();
    }
    if (second.getGeneralizedCost() == UNKNOWN) {
      return first.getGeneralizedCost();
    }
    return first.getGeneralizedCost() + second.getGeneralizedCost();
  }

  @Override
  public Set<FareZone> getFareZones() {
    Set<FareZone> fareZones = first.getFareZones();
    fareZones.addAll(second.getFareZones());

    return fareZones;
  }

  @Override
  public void setFareProducts(List<FareProductUse> products) {}

  @Override
  public List<FareProductUse> fareProducts() {
    return List.of();
  }

  /**
   * The two legs that this combined leg originally consisted of.
   */
  public List<Leg> originalLegs() {
    return List.of(first, second);
  }
}
