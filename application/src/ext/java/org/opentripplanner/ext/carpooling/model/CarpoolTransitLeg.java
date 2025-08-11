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
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.model.plan.leg.LegCallTime;
import org.opentripplanner.model.plan.leg.StopArrival;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle, which is running on flexible trip, i.e. not using fixed schedule and stops.
 */
public class CarpoolTransitLeg implements TransitLeg {

  private final ZonedDateTime startTime;

  private final ZonedDateTime endTime;

  private final Set<TransitAlert> transitAlerts;

  private final int generalizedCost;

  private final Emission emissionPerPerson;

  private final List<FareProductUse> fareProducts;

  private final Trip trip;

  CarpoolTransitLeg(CarpoolTransitLegBuilder builder) {
    this.startTime = Objects.requireNonNull(builder.startTime());
    this.endTime = Objects.requireNonNull(builder.endTime());
    this.generalizedCost = builder.generalizedCost();
    this.transitAlerts = Set.copyOf(builder.alerts());
    this.fareProducts = List.copyOf(builder.fareProducts());
    this.emissionPerPerson = builder.emissionPerPerson();
    this.trip = Objects.requireNonNull(builder.trip(), "Trip must not be null");
  }

  /**
   * Return an empty builder for {@link CarpoolTransitLeg}.
   */
  public static CarpoolTransitLegBuilder of() {
    return new CarpoolTransitLegBuilder();
  }

  public CarpoolTransitLegBuilder copyOf() {
    return new CarpoolTransitLegBuilder(this);
  }

  @Override
  public Agency agency() {
    return trip().getRoute().getAgency();
  }

  @Override
  @Nullable
  public Operator operator() {
    return trip().getOperator();
  }

  @Override
  public Route route() {
    return trip().getRoute();
  }

  @Override
  public Trip trip() {
    return trip;
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
  public TransitMode mode() {
    return trip().getMode();
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
    // TODO CARPOOLING
    return 999_999.0;
  }

  @Override
  public Integer routeType() {
    return trip().getRoute().getGtfsType();
  }

  @Override
  public I18NString headsign() {
    return trip().getHeadsign();
  }

  @Override
  public LocalDate serviceDate() {
    // TODO CARPOOLING
    return null;
  }

  @Override
  public Place from() {
    // TODO CARPOOLING
    return null;
  }

  @Override
  public Place to() {
    // TODO CARPOOLING
    return null;
  }

  @Override
  public List<StopArrival> listIntermediateStops() {
    return List.of();
  }

  @Override
  public LineString legGeometry() {
    // TODO CARPOOLING
    return null;
  }

  @Override
  public Set<TransitAlert> listTransitAlerts() {
    return transitAlerts;
  }

  @Override
  public TransitLeg decorateWithAlerts(Set<TransitAlert> alerts) {
    return copyOf().withAlerts(alerts).build();
  }

  @Override
  public TransitLeg decorateWithFareProducts(List<FareProductUse> fares) {
    return copyOf().withFareProducts(fares).build();
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

  @Override
  public List<FareProductUse> fareProducts() {
    return fareProducts;
  }

  /**
   * Should be used for debug logging only
   */
  @Override
  public String toString() {
    return ToStringBuilder.of(CarpoolTransitLeg.class)
      .addObj("from", from())
      .addObj("to", to())
      .addTime("startTime", startTime)
      .addTime("endTime", endTime)
      .addNum("distance", distanceMeters(), "m")
      .addNum("cost", generalizedCost)
      .addObjOp("agencyId", agency(), AbstractTransitEntity::getId)
      .addObjOp("routeId", route(), AbstractTransitEntity::getId)
      .addObjOp("tripId", trip(), AbstractTransitEntity::getId)
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
