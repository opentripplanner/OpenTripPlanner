package org.opentripplanner.ext.taxizone.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.LegCallTime;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.street.model.elevation.ElevationProfile;
import org.opentripplanner.street.model.note.StreetNote;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

/**
 * A leg for a car pickup/drop-off decorated with a matched {@link TaxiZone} provider (e.g. a
 * taxi). It is physically a street/driving leg, and is modeled as a plain {@link Leg} (not a
 * {@link org.opentripplanner.model.plan.TransitLeg}) even though it carries route, agency and
 * booking information from the matched provider.
 * <p>
 * The underlying street route data (geometry, distance, elevation, etc.) is delegated to the
 * wrapped {@link StreetLeg}.
 */
public class TaxiZoneLeg implements Leg {

  private final StreetLeg streetLeg;
  private final TaxiZone taxiZone;

  public TaxiZoneLeg(StreetLeg streetLeg, TaxiZone taxiZone) {
    this.streetLeg = streetLeg.copyOf().build();
    this.taxiZone = taxiZone;
  }

  public TaxiZone taxiZone() {
    return taxiZone;
  }

  @Override
  public boolean isTransitLeg() {
    return false;
  }

  @Override
  public boolean isStreetLeg() {
    return true;
  }

  @Override
  public boolean hasSameMode(Leg other) {
    return other instanceof TaxiZoneLeg tzl && mode().equals(tzl.mode());
  }

  public TransitMode mode() {
    return taxiZone.route().getMode();
  }

  @Override
  public Agency agency() {
    return taxiZone.route().getAgency();
  }

  @Override
  public Route route() {
    return taxiZone.route();
  }

  /**
   * The leg only exists because {@link org.opentripplanner.ext.taxizone.TaxiZoneIndex} already
   * matched the {@link TaxiZone} against this exact date (see
   * {@link TaxiZone#serviceDateRange()}).
   */
  @Override
  public LocalDate serviceDate() {
    return startTime().toLocalDate();
  }

  /**
   * Taxi zone trips always consist of exactly one pickup stop and one drop-off stop (see
   * {@link org.opentripplanner.ext.taxizone.graphbuilder.TaxiZoneBuilder}), so these
   * positions are fixed.
   */
  @Override
  public Integer boardStopPosInPattern() {
    return 0;
  }

  /**
   * Taxi zone trips always consist of exactly one pickup stop and one drop-off stop (see
   * {@link org.opentripplanner.ext.taxizone.graphbuilder.TaxiZoneBuilder}), so these
   * positions are fixed.
   */
  @Override
  public Integer alightStopPosInPattern() {
    return 1;
  }

  @Override
  @Nullable
  public BookingInfo pickupBookingInfo() {
    return taxiZone.pickupBookingInfo();
  }

  @Override
  @Nullable
  public BookingInfo dropOffBookingInfo() {
    return taxiZone.dropOffBookingInfo();
  }

  @Override
  public Set<TransitAlert> listTransitAlerts() {
    return Set.of();
  }

  @Override
  public ZonedDateTime startTime() {
    return streetLeg.startTime();
  }

  @Override
  public ZonedDateTime endTime() {
    return streetLeg.endTime();
  }

  @Override
  public double distanceMeters() {
    return streetLeg.distanceMeters();
  }

  @Override
  public Place from() {
    return streetLeg.from();
  }

  @Override
  public Place to() {
    return streetLeg.to();
  }

  @Override
  public LineString legGeometry() {
    return streetLeg.legGeometry();
  }

  @Override
  public ElevationProfile elevationProfile() {
    return streetLeg.elevationProfile();
  }

  @Override
  public List<WalkStep> listWalkSteps() {
    return streetLeg.listWalkSteps();
  }

  @Override
  public Set<StreetNote> listStreetNotes() {
    return streetLeg.listStreetNotes();
  }

  @Override
  public Boolean walkingBike() {
    return streetLeg.walkingBike();
  }

  @Override
  public Boolean rentedVehicle() {
    return streetLeg.rentedVehicle();
  }

  @Override
  public String vehicleRentalNetwork() {
    return streetLeg.vehicleRentalNetwork();
  }

  @Override
  public int generalizedCost() {
    return streetLeg.generalizedCost();
  }

  @Override
  public LegCallTime start() {
    return streetLeg.start();
  }

  @Override
  public LegCallTime end() {
    return streetLeg.end();
  }

  @Override
  @Nullable
  public Float accessibilityScore() {
    return streetLeg.accessibilityScore();
  }

  @Nullable
  @Override
  public Emission emissionPerPerson() {
    return streetLeg.emissionPerPerson();
  }

  @Override
  public List<FareOffer> fareOffers() {
    return streetLeg.fareOffers();
  }

  @Nullable
  @Override
  public Leg withEmissionPerPerson(Emission emissionPerPerson) {
    return new TaxiZoneLeg(
      (StreetLeg) streetLeg.withEmissionPerPerson(emissionPerPerson),
      taxiZone
    );
  }

  @Override
  public Leg withTimeShift(Duration duration) {
    return new TaxiZoneLeg((StreetLeg) streetLeg.withTimeShift(duration), taxiZone);
  }

  @Override
  public String toString() {
    return "TaxiZoneLeg{" + "streetLeg=" + streetLeg + ", taxiZone=" + taxiZone + '}';
  }
}
