package org.opentripplanner.ext.carpickupzone.model;

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
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.model.plan.leg.LegCallTime;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.street.model.elevation.ElevationProfile;
import org.opentripplanner.street.model.note.StreetNote;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

/**
 * A leg for a car pickup/drop-off decorated with a matched {@link CarPickupZone} provider (e.g. a
 * taxi). It is physically a street/driving leg, but is modeled as a {@link TransitLeg} since it
 * carries route, agency and booking information from the matched provider.
 * <p>
 * The underlying street route data (geometry, distance, elevation, etc.) is delegated to the
 * wrapped {@link StreetLeg}.
 */
public class CarPickupZoneLeg implements TransitLeg {

  private final StreetLeg streetLeg;
  private final CarPickupZone carPickupZone;
  private final Set<TransitAlert> transitAlerts;

  public CarPickupZoneLeg(StreetLeg streetLeg, CarPickupZone carPickupZone) {
    this(streetLeg, carPickupZone, Set.of());
  }

  private CarPickupZoneLeg(
    StreetLeg streetLeg,
    CarPickupZone carPickupZone,
    Set<TransitAlert> transitAlerts
  ) {
    this.streetLeg = streetLeg.copyOf().build();
    this.carPickupZone = carPickupZone;
    this.transitAlerts = Set.copyOf(transitAlerts);
  }

  public CarPickupZone carPickupZone() {
    return carPickupZone;
  }

  @Override
  public TransitMode mode() {
    return carPickupZone.trip().getRoute().getMode();
  }

  @Override
  public Agency agency() {
    return carPickupZone.trip().getRoute().getAgency();
  }

  @Override
  public Route route() {
    return carPickupZone.trip().getRoute();
  }

  @Override
  public Trip trip() {
    return carPickupZone.trip();
  }

  /**
   * There is no real GTFS calendar/service-date resolution available at itinerary-decoration
   * time (this is a post-hoc filter, not part of the routing search), so the leg's own start
   * date is used as a practical stand-in.
   */
  @Override
  public LocalDate serviceDate() {
    return startTime().toLocalDate();
  }

  /**
   * Car pickup zone trips always consist of exactly one pickup stop and one drop-off stop (see
   * {@link org.opentripplanner.ext.carpickupzone.graphbuilder.CarPickupZoneBuilder}), so these
   * positions are fixed.
   */
  @Override
  public Integer boardStopPosInPattern() {
    return 0;
  }

  /**
   * Car pickup zone trips always consist of exactly one pickup stop and one drop-off stop (see
   * {@link org.opentripplanner.ext.carpickupzone.graphbuilder.CarPickupZoneBuilder}), so these
   * positions are fixed.
   */
  @Override
  public Integer alightStopPosInPattern() {
    return 1;
  }

  @Override
  @Nullable
  public BookingInfo pickupBookingInfo() {
    return carPickupZone.pickupBookingInfo();
  }

  @Override
  @Nullable
  public BookingInfo dropOffBookingInfo() {
    return carPickupZone.dropOffBookingInfo();
  }

  @Override
  public Set<TransitAlert> listTransitAlerts() {
    return transitAlerts;
  }

  @Override
  public CarPickupZoneLeg decorateWithAlerts(Set<TransitAlert> alerts) {
    return new CarPickupZoneLeg(streetLeg, carPickupZone, alerts);
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
    return new CarPickupZoneLeg(
      (StreetLeg) streetLeg.withEmissionPerPerson(emissionPerPerson),
      carPickupZone,
      transitAlerts
    );
  }

  @Override
  public Leg withTimeShift(Duration duration) {
    return new CarPickupZoneLeg(
      (StreetLeg) streetLeg.withTimeShift(duration),
      carPickupZone,
      transitAlerts
    );
  }

  @Override
  public String toString() {
    return (
      "CarPickupZoneLeg{" + "streetLeg=" + streetLeg + ", carPickupZone=" + carPickupZone + '}'
    );
  }
}
