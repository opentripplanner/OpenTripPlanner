package org.opentripplanner.ext.fares.impl;

import static org.opentripplanner.model.plan.Itinerary.UNKNOWN;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.LegCallTime;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.ScheduledTransitLegBuilder;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.FareZone;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.collection.ListUtils;

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

  @Override
  public TransitMode getMode() {
    return first.getMode();
  }

  @Override
  public Route getRoute() {
    return first.getRoute();
  }

  @Override
  public Trip getTrip() {
    return first.getTrip();
  }

  @Override
  public LegCallTime start() {
    return first.start();
  }

  @Override
  public LegCallTime end() {
    return second.end();
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
  @Nullable
  public LineString getLegGeometry() {
    return null;
  }

  @Override
  public Set<TransitAlert> getTransitAlerts() {
    return Set.of();
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
  public List<FareProductUse> fareProducts() {
    return List.of();
  }

  /**
   * The two legs that this combined leg originally consisted of.
   */
  public List<Leg> originalLegs() {
    return List.of(first, second);
  }

  @Override
  public TransitLeg decorateWithAlerts(Set<TransitAlert> alerts) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TransitLeg decorateWithFareProducts(List<FareProductUse> fares) {
    throw new UnsupportedOperationException();
  }
}
