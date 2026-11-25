package org.opentripplanner.ext.carpooling.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.LegCallTime;
import org.opentripplanner.model.plan.leg.StopArrival;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * One leg of a carpooling trip -- that is, a temporally continuous piece of the journey that takes
 * place on a particular vehicle.
 */
public class CarpoolLeg implements Leg {

  private final ZonedDateTime startTime;

  private final ZonedDateTime endTime;

  private final Set<TransitAlert> transitAlerts;

  private final int generalizedCost;

  private final Emission emissionPerPerson;

  private final Place from;

  private final Place to;

  private final LineString geometry;

  private final double distanceMeters;

  @Nullable
  private final BookingInfo pickupBookingInfo;

  CarpoolLeg(CarpoolLegBuilder builder) {
    this.startTime = Objects.requireNonNull(builder.startTime());
    this.endTime = Objects.requireNonNull(builder.endTime());
    this.generalizedCost = builder.generalizedCost();
    this.transitAlerts = Set.copyOf(builder.alerts());
    this.emissionPerPerson = builder.emissionPerPerson();
    this.from = builder.from();
    this.to = builder.to();
    this.geometry = builder.geometry();
    this.distanceMeters = builder.distanceMeters();
    this.pickupBookingInfo = builder.pickupBookingInfo();
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
  public boolean hasSameMode(Leg other) {
    return false;
  }

  @Nullable
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
  public boolean isFlexibleTrip() {
    return true;
  }

  @Override
  public double distanceMeters() {
    return distanceMeters;
  }

  @Nullable
  @Override
  public LocalDate serviceDate() {
    // TODO CARPOOLING
    return null;
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

  @Override
  public Set<TransitAlert> listTransitAlerts() {
    return transitAlerts;
  }

  @Nullable
  @Override
  public PickDrop boardRule() {
    // TODO CARPOOLING
    return null;
  }

  @Nullable
  @Override
  public PickDrop alightRule() {
    // TODO CARPOOLING
    return null;
  }

  @Nullable
  @Override
  public BookingInfo dropOffBookingInfo() {
    // TODO CARPOOLING
    return null;
  }

  @Nullable
  @Override
  public BookingInfo pickupBookingInfo() {
    return pickupBookingInfo;
  }

  @Nullable
  @Override
  public Integer boardStopPosInPattern() {
    // TODO CARPOOLING
    return null;
  }

  @Nullable
  @Override
  public Integer alightStopPosInPattern() {
    // TODO CARPOOLING
    return null;
  }

  @Override
  public int generalizedCost() {
    return generalizedCost;
  }

  @Override
  public Leg withTimeShift(Duration duration) {
    return copyOf()
      .withStartTime(startTime.plus(duration))
      .withEndTime(endTime.plus(duration))
      .build();
  }

  @Override
  public List<FareOffer> fareOffers() {
    return List.of();
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
