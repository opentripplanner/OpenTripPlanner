package org.opentripplanner.model.plan;

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

/**
 * Many methods in this class throw {@link NotImplementedException}. Please implement them when
 * you need them.
 */
public class TestTransitLeg implements TransitLeg {

  private final ZonedDateTime startTime;
  private final ZonedDateTime endTime;

  public TestTransitLeg(TestTransitLegBuilder builder) {
    this.startTime = builder.startTime;
    this.endTime = builder.endTime;
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
    throw new NotImplementedException();
  }

  @Override
  public LegCallTime end() {
    throw new NotImplementedException();
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
    throw new NotImplementedException();
  }

  @Override
  public Place to() {
    throw new NotImplementedException();
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
  public List<FareOffer> fareOffers() {
    return List.of();
  }

  public static TestTransitLegBuilder of() {
    return new TestTransitLegBuilder();
  }
}
