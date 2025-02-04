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
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.LegCallTime;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.plan.TransitLeg;
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
  private final List<FareProductUse> fareProducts;

  FlexibleTransitLeg(FlexibleTransitLegBuilder builder) {
    this.edge = Objects.requireNonNull(builder.flexTripEdge());
    this.startTime = Objects.requireNonNull(builder.startTime());
    this.endTime = Objects.requireNonNull(builder.endTime());
    this.generalizedCost = builder.generalizedCost();
    this.transitAlerts = Set.copyOf(builder.alerts());
    this.fareProducts = List.copyOf(builder.fareProducts());
  }

  /**
   * Return an empty builder for {@link FlexibleTransitLeg}.
   */
  public static FlexibleTransitLegBuilder of() {
    return new FlexibleTransitLegBuilder();
  }

  @Override
  public Agency getAgency() {
    return getTrip().getRoute().getAgency();
  }

  @Override
  @Nullable
  public Operator getOperator() {
    return getTrip().getOperator();
  }

  @Override
  public Route getRoute() {
    return getTrip().getRoute();
  }

  @Override
  public Trip getTrip() {
    return edge.getFlexTrip().getTrip();
  }

  @Override
  public Accessibility getTripWheelchairAccessibility() {
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
  public TransitMode getMode() {
    return getTrip().getMode();
  }

  @Override
  public ZonedDateTime getStartTime() {
    return startTime;
  }

  @Override
  public ZonedDateTime getEndTime() {
    return endTime;
  }

  @Override
  public boolean isFlexibleTrip() {
    return true;
  }

  @Override
  public double getDistanceMeters() {
    return DoubleUtils.roundTo2Decimals(edge.getDistanceMeters());
  }

  @Override
  public Integer getRouteType() {
    return getTrip().getRoute().getGtfsType();
  }

  @Override
  public I18NString getHeadsign() {
    return getTrip().getHeadsign();
  }

  @Override
  public LocalDate getServiceDate() {
    return edge.serviceDate();
  }

  @Override
  public Place getFrom() {
    return Place.forFlexStop(edge.s1(), edge.getFromVertex());
  }

  @Override
  public Place getTo() {
    return Place.forFlexStop(edge.s2(), edge.getToVertex());
  }

  @Override
  public List<StopArrival> getIntermediateStops() {
    return List.of();
  }

  @Override
  public LineString getLegGeometry() {
    return edge.getGeometry();
  }

  @Override
  public Set<TransitAlert> getTransitAlerts() {
    return transitAlerts;
  }

  @Override
  public TransitLeg decorateWithAlerts(Set<TransitAlert> alerts) {
    return copy().withAlerts(alerts).build();
  }

  @Override
  public TransitLeg decorateWithFareProducts(List<FareProductUse> fares) {
    return copy().withFareProducts(fares).build();
  }

  @Override
  public PickDrop getBoardRule() {
    return edge.getFlexTrip().getBoardRule(getBoardStopPosInPattern());
  }

  @Override
  public PickDrop getAlightRule() {
    return edge.getFlexTrip().getAlightRule(getAlightStopPosInPattern());
  }

  @Override
  public BookingInfo getDropOffBookingInfo() {
    return edge.getFlexTrip().getDropOffBookingInfo(getAlightStopPosInPattern());
  }

  @Override
  public BookingInfo getPickupBookingInfo() {
    return edge.getFlexTrip().getPickupBookingInfo(getBoardStopPosInPattern());
  }

  @Override
  public Integer getBoardStopPosInPattern() {
    return edge.boardStopPosInPattern();
  }

  @Override
  public Integer getAlightStopPosInPattern() {
    return edge.alightStopPosInPattern();
  }

  @Override
  public int getGeneralizedCost() {
    return generalizedCost;
  }

  @Override
  public Leg withTimeShift(Duration duration) {
    return copy()
      .withStartTime(startTime.plus(duration))
      .withEndTime(endTime.plus(duration))
      .build();
  }

  @Override
  public List<FareProductUse> fareProducts() {
    return fareProducts;
  }

  public FlexibleTransitLegBuilder copy() {
    return new FlexibleTransitLegBuilder(this);
  }

  /**
   * Should be used for debug logging only
   */
  @Override
  public String toString() {
    return ToStringBuilder
      .of(FlexibleTransitLeg.class)
      .addObj("from", getFrom())
      .addObj("to", getTo())
      .addTime("startTime", startTime)
      .addTime("endTime", endTime)
      .addNum("distance", getDistanceMeters(), "m")
      .addNum("cost", generalizedCost)
      .addObjOp("agencyId", getAgency(), AbstractTransitEntity::getId)
      .addObjOp("routeId", getRoute(), AbstractTransitEntity::getId)
      .addObjOp("tripId", getTrip(), AbstractTransitEntity::getId)
      .addObj("serviceDate", getServiceDate())
      .addObj("legGeometry", getLegGeometry())
      .addCol("transitAlerts", transitAlerts)
      .addNum("boardingStopIndex", getBoardStopPosInPattern())
      .addNum("alightStopIndex", getAlightStopPosInPattern())
      .addEnum("boardRule", getBoardRule())
      .addEnum("alightRule", getAlightRule())
      .addObj("pickupBookingInfo", getPickupBookingInfo())
      .addObj("dropOffBookingInfo", getDropOffBookingInfo())
      .toString();
  }

  FlexTripEdge flexTripEdge() {
    return edge;
  }
}
