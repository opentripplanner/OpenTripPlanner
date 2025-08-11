package org.opentripplanner.ext.carpooling.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.ElevationProfile;
import org.opentripplanner.model.plan.leg.LegCallTime;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.StopArrival;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.street.model.note.StreetNote;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.FareZone;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle, which is running on flexible trip, i.e. not using fixed schedule and stops.
 */
public class CarpoolLeg implements Leg {

  private final ZonedDateTime startTime;

  private final ZonedDateTime endTime;

  private final Set<TransitAlert> transitAlerts;

  private final int generalizedCost;

  private final Emission emissionPerPerson;

  private final List<FareProductUse> fareProducts;

  private final Place from;

  private final Place to;

  private final LineString geometry;

  private final double distanceMeters;

  CarpoolLeg(CarpoolLegBuilder builder) {
    this.startTime = Objects.requireNonNull(builder.startTime());
    this.endTime = Objects.requireNonNull(builder.endTime());
    this.generalizedCost = builder.generalizedCost();
    this.transitAlerts = Set.copyOf(builder.alerts());
    this.fareProducts = List.copyOf(builder.fareProducts());
    this.emissionPerPerson = builder.emissionPerPerson();
    this.from = builder.from();
    this.to = builder.to();
    this.geometry = builder.geometry();
    this.distanceMeters = builder.distanceMeters();
  }

  /**
   * Return an empty builder for {@link CarpoolLeg}.
   */
  public static CarpoolLegBuilder of() {
    return new CarpoolLegBuilder();
  }

  public CarpoolLegBuilder copyOf() {
    return new CarpoolLegBuilder(this);
  }

  @Override
  public boolean isTransitLeg() {
    return false;
  }

  @Override
  public boolean isScheduledTransitLeg() {
    return Leg.super.isScheduledTransitLeg();
  }

  @Override
  public ScheduledTransitLeg asScheduledTransitLeg() {
    return Leg.super.asScheduledTransitLeg();
  }

  @Override
  public Boolean isInterlinedWithPreviousLeg() {
    return Leg.super.isInterlinedWithPreviousLeg();
  }

  @Override
  public boolean isWalkingLeg() {
    return Leg.super.isWalkingLeg();
  }

  @Override
  public boolean isStreetLeg() {
    return Leg.super.isStreetLeg();
  }

  @Override
  public Duration duration() {
    return Leg.super.duration();
  }

  @Override
  public boolean isPartiallySameTransitLeg(Leg other) {
    return Leg.super.isPartiallySameTransitLeg(other);
  }

  @Override
  public boolean hasSameMode(Leg other) {
    return false;
  }

  @Override
  public boolean isPartiallySameLeg(Leg other) {
    return Leg.super.isPartiallySameLeg(other);
  }

  @Override
  public boolean overlapInTime(Leg other) {
    return Leg.super.overlapInTime(other);
  }

  @Override
  public Agency agency() {
    return null;
  }

  @Override
  @Nullable
  public Operator operator() {
    return null;
  }

  @Override
  public Route route() {
    return null;
  }

  @Nullable
  @Override
  public TripOnServiceDate tripOnServiceDate() {
    return Leg.super.tripOnServiceDate();
  }

  @Override
  public Accessibility tripWheelchairAccessibility() {
    // TODO CARPOOLING
    return null;
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
  public int departureDelay() {
    return Leg.super.departureDelay();
  }

  @Override
  public int arrivalDelay() {
    return Leg.super.arrivalDelay();
  }

  @Override
  public boolean isRealTimeUpdated() {
    return Leg.super.isRealTimeUpdated();
  }

  @Nullable
  @Override
  public RealTimeState realTimeState() {
    return Leg.super.realTimeState();
  }

  @Override
  public boolean isFlexibleTrip() {
    return true;
  }

  @Nullable
  @Override
  public Boolean isNonExactFrequency() {
    return Leg.super.isNonExactFrequency();
  }

  @Nullable
  @Override
  public Integer headway() {
    return Leg.super.headway();
  }

  @Override
  public double distanceMeters() {
    return distanceMeters;
  }

  @Override
  public int agencyTimeZoneOffset() {
    return Leg.super.agencyTimeZoneOffset();
  }

  @Override
  public Integer routeType() {
    return null;
  }

  @Override
  public I18NString headsign() {
    return null;
  }

  @Override
  public LocalDate serviceDate() {
    // TODO CARPOOLING
    return null;
  }

  @Nullable
  @Override
  public String routeBrandingUrl() {
    return Leg.super.routeBrandingUrl();
  }

  @Override
  public Place from() {
    return from;
  }

  @Override
  public Place to() {
    return to;
  }

  @Override
  public List<StopArrival> listIntermediateStops() {
    return List.of();
  }

  @Override
  public LineString legGeometry() {
    return geometry;
  }

  @Nullable
  @Override
  public ElevationProfile elevationProfile() {
    return Leg.super.elevationProfile();
  }

  @Override
  public List<WalkStep> listWalkSteps() {
    return Leg.super.listWalkSteps();
  }

  @Override
  public Set<StreetNote> listStreetNotes() {
    return Leg.super.listStreetNotes();
  }

  @Override
  public Set<TransitAlert> listTransitAlerts() {
    return transitAlerts;
  }

  @Override
  public PickDrop boardRule() {
    // TODO CARPOOLING
    return null;
  }

  @Override
  public PickDrop alightRule() {
    // TODO CARPOOLING
    return null;
  }

  @Override
  public BookingInfo dropOffBookingInfo() {
    // TODO CARPOOLING
    return null;
  }

  @Override
  public BookingInfo pickupBookingInfo() {
    // TODO CARPOOLING
    return null;
  }

  @Nullable
  @Override
  public ConstrainedTransfer transferFromPrevLeg() {
    return Leg.super.transferFromPrevLeg();
  }

  @Nullable
  @Override
  public ConstrainedTransfer transferToNextLeg() {
    return Leg.super.transferToNextLeg();
  }

  @Override
  public Integer boardStopPosInPattern() {
    // TODO CARPOOLING
    return 0;
  }

  @Override
  public Integer alightStopPosInPattern() {
    // TODO CARPOOLING
    return 1;
  }

  @Nullable
  @Override
  public Integer boardingGtfsStopSequence() {
    return Leg.super.boardingGtfsStopSequence();
  }

  @Nullable
  @Override
  public Integer alightGtfsStopSequence() {
    return Leg.super.alightGtfsStopSequence();
  }

  @Nullable
  @Override
  public Boolean walkingBike() {
    return Leg.super.walkingBike();
  }

  @Nullable
  @Override
  public Float accessibilityScore() {
    return Leg.super.accessibilityScore();
  }

  @Override
  public int generalizedCost() {
    return generalizedCost;
  }

  @Nullable
  @Override
  public LegReference legReference() {
    return Leg.super.legReference();
  }

  @Override
  public Leg withTimeShift(Duration duration) {
    return copyOf()
      .withStartTime(startTime.plus(duration))
      .withEndTime(endTime.plus(duration))
      .build();
  }

  @Override
  public Set<FareZone> fareZones() {
    return Leg.super.fareZones();
  }

  @Nullable
  @Override
  public Emission emissionPerPerson() {
    return emissionPerPerson;
  }

  @Nullable
  @Override
  public Leg withEmissionPerPerson(Emission emissionPerPerson) {
    return copyOf().withEmissionPerPerson(emissionPerPerson).build();
  }

  @Nullable
  @Override
  public Boolean rentedVehicle() {
    return Leg.super.rentedVehicle();
  }

  @Nullable
  @Override
  public String vehicleRentalNetwork() {
    return Leg.super.vehicleRentalNetwork();
  }

  @Override
  public List<FareProductUse> fareProducts() {
    return fareProducts;
  }

  public TransitMode mode() {
    return TransitMode.CARPOOL;
  }

  /**
   * Should be used for debug logging only
   */
  @Override
  public String toString() {
    return ToStringBuilder.of(CarpoolLeg.class)
      .addObj("from", from())
      .addObj("to", to())
      .addTime("startTime", startTime)
      .addTime("endTime", endTime)
      .addNum("distance", distanceMeters(), "m")
      .addNum("cost", generalizedCost)
      .addObj("serviceDate", serviceDate())
      .addObj("legGeometry", legGeometry())
      .addCol("transitAlerts", transitAlerts)
      .addNum("boardingStopIndex", boardStopPosInPattern())
      .addNum("alightStopIndex", alightStopPosInPattern())
      .addEnum("boardRule", boardRule())
      .addEnum("alightRule", alightRule())
      .addObj("pickupBookingInfo", pickupBookingInfo())
      .addObj("dropOffBookingInfo", dropOffBookingInfo())
      .toString();
  }
}
