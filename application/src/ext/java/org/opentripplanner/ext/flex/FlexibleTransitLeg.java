package org.opentripplanner.ext.flex;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
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
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle, which is running on flexible trip, i.e. not using fixed schedule and stops.
 */
public class FlexibleTransitLeg implements TransitLeg {

  private final FlexTripEdge edge;

  private final ZonedDateTime startTime;

  private final ZonedDateTime endTime;

  private final Set<TransitAlert> transitAlerts;

  private final int generalizedCost;

  private final Emission emissionPerPerson;

  private final List<FareProductUse> fareProducts;

  FlexibleTransitLeg(FlexibleTransitLegBuilder builder) {
    this.edge = Objects.requireNonNull(builder.flexTripEdge());
    this.startTime = Objects.requireNonNull(builder.startTime());
    this.endTime = Objects.requireNonNull(builder.endTime());
    this.generalizedCost = builder.generalizedCost();
    this.transitAlerts = Set.copyOf(builder.alerts());
    this.fareProducts = List.copyOf(builder.fareProducts());
    this.emissionPerPerson = builder.emissionPerPerson();
  }

  /**
   * Return an empty builder for {@link FlexibleTransitLeg}.
   */
  public static FlexibleTransitLegBuilder of() {
    return new FlexibleTransitLegBuilder();
  }

  public FlexibleTransitLegBuilder copyOf() {
    return new FlexibleTransitLegBuilder(this);
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
    return edge.getFlexTrip().getTrip();
  }

  @Override
  public Accessibility tripWheelchairAccessibility() {
    return edge.getFlexTrip().getTrip().getWheelchairBoarding();
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
    return DoubleUtils.roundTo2Decimals(edge.getDistanceMeters());
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
    return edge.serviceDate();
  }

  @Override
  public Place from() {
    return Place.forFlexStop(edge.s1(), edge.getFromVertex());
  }

  @Override
  public Place to() {
    return Place.forFlexStop(edge.s2(), edge.getToVertex());
  }

  @Override
  public List<StopArrival> listIntermediateStops() {
    return List.of();
  }

  @Override
  public LineString legGeometry() {
    return edge.getGeometry();
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
    return edge.getFlexTrip().getBoardRule(boardStopPosInPattern());
  }

  @Override
  public PickDrop alightRule() {
    return edge.getFlexTrip().getAlightRule(alightStopPosInPattern());
  }

  @Override
  public BookingInfo dropOffBookingInfo() {
    return edge.getFlexTrip().getDropOffBookingInfo(alightStopPosInPattern());
  }

  @Override
  public BookingInfo pickupBookingInfo() {
    return edge.getFlexTrip().getPickupBookingInfo(boardStopPosInPattern());
  }

  @Override
  public Integer boardStopPosInPattern() {
    return edge.boardStopPosInPattern();
  }

  @Override
  public Integer alightStopPosInPattern() {
    return edge.alightStopPosInPattern();
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
    return ToStringBuilder.of(FlexibleTransitLeg.class)
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

  FlexTripEdge flexTripEdge() {
    return edge;
  }
}
