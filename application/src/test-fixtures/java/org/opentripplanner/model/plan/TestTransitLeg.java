package org.opentripplanner.model.plan;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.leg.LegCallTime;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Many methods in this class throw {@link NotImplementedException}. Please implement them when
 * you need them.
 */
public class TestTransitLeg implements TransitLeg {

  private final StopLocation from;
  private final StopLocation to;
  private final ZonedDateTime startTime;
  private final ZonedDateTime endTime;
  private final Trip trip;

  public TestTransitLeg(TestTransitLegBuilder builder) {
    this.from = builder.from;
    this.to = builder.to;
    this.startTime = builder.startTime;
    this.endTime = builder.endTime;
    this.trip = builder.trip;
  }

  @Override
  public Agency agency() {
    return trip.getRoute().getAgency();
  }

  @Override
  public Route route() {
    return trip.getRoute();
  }

  @Nullable
  @Override
  public Trip trip() {
    return trip;
  }

  @Override
  public TransitMode mode() {
    throw new NotImplementedException();
  }

  @Override
  public TransitLeg decorateWithAlerts(Set<TransitAlert> alerts) {
    throw new NotImplementedException();
  }

  @Override
  public TransitLeg decorateWithFareOffers(List<FareOffer> fares) {
    throw new NotImplementedException();
  }

  @Override
  public LegCallTime start() {
    return LegCallTime.ofStatic(startTime);
  }

  @Override
  public LegCallTime end() {
    return LegCallTime.ofStatic(endTime);
  }

  @Override
  public ZonedDateTime startTime() {
    return startTime;
  }

  @Override
  public ZonedDateTime endTime() {
    return endTime;
  }

  @Override
  public double distanceMeters() {
    return 0;
  }

  @Override
  public Place from() {
    return Place.forStop(from);
  }

  @Override
  public Place to() {
    return Place.forStop(to);
  }

  @Override
  public @Nullable LineString legGeometry() {
    throw new NotImplementedException();
  }

  @Override
  public Set<TransitAlert> listTransitAlerts() {
    throw new NotImplementedException();
  }

  @Override
  public @Nullable Emission emissionPerPerson() {
    throw new NotImplementedException();
  }

  @Override
  public @Nullable Leg withEmissionPerPerson(Emission emissionPerPerson) {
    throw new NotImplementedException();
  }

  @Override
  public int generalizedCost() {
    return 0;
  }

  @Override
  public LocalDate serviceDate() {
    return startTime.toLocalDate();
  }

  @Override
  public List<FareOffer> fareOffers() {
    return List.of();
  }

  public static TestTransitLegBuilder of() {
    return new TestTransitLegBuilder();
  }
}
