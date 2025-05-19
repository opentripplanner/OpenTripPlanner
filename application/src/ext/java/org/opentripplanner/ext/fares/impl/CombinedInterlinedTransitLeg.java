package org.opentripplanner.ext.fares.impl;

import static org.opentripplanner.model.plan.Itinerary.UNKNOWN;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.model.plan.leg.LegCallTime;
import org.opentripplanner.model.plan.leg.StopArrival;
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

  public Agency agency() {
    return first.agency();
  }

  @Override
  public TransitMode mode() {
    return first.mode();
  }

  @Override
  public Route route() {
    return first.route();
  }

  @Override
  public Trip trip() {
    return first.trip();
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
  public ZonedDateTime startTime() {
    return first.startTime();
  }

  @Override
  public ZonedDateTime endTime() {
    return second.startTime();
  }

  @Override
  public double distanceMeters() {
    return first.distanceMeters() + second.distanceMeters();
  }

  @Override
  public Place from() {
    return first.from();
  }

  @Override
  public Place to() {
    return second.to();
  }

  @Override
  public List<StopArrival> listIntermediateStops() {
    return ListUtils.combine(first.listIntermediateStops(), second.listIntermediateStops());
  }

  @Override
  @Nullable
  public LineString legGeometry() {
    return null;
  }

  @Override
  public Set<TransitAlert> listTransitAlerts() {
    return Set.of();
  }

  @Override
  public int generalizedCost() {
    if (first.generalizedCost() == UNKNOWN) {
      return second.generalizedCost();
    }
    if (second.generalizedCost() == UNKNOWN) {
      return first.generalizedCost();
    }
    return first.generalizedCost() + second.generalizedCost();
  }

  @Override
  public Set<FareZone> fareZones() {
    Set<FareZone> fareZones = first.fareZones();
    fareZones.addAll(second.fareZones());

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
