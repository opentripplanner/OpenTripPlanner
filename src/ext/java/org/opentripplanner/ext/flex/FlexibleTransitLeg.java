package org.opentripplanner.ext.flex;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.plan.Leg;
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

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle, which is running on flexible trip, i.e. not using fixed schedule and stops.
 */
public class FlexibleTransitLeg implements TransitLeg {

  private final FlexTripEdge edge;

  private final ZonedDateTime startTime;

  private final ZonedDateTime endTime;

  private final Set<TransitAlert> transitAlerts = new HashSet<>();

  private final int generalizedCost;
  private List<FareProductUse> fareProducts;

  public FlexibleTransitLeg(
    FlexTripEdge flexTripEdge,
    ZonedDateTime startTime,
    ZonedDateTime endTime,
    int generalizedCost
  ) {
    this.edge = flexTripEdge;

    this.startTime = startTime;
    this.endTime = endTime;

    this.generalizedCost = generalizedCost;
  }

  @Override
  public Agency getAgency() {
    return getTrip().getRoute().getAgency();
  }

  @Override
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
  @Nonnull
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
    return edge.flexTemplate.serviceDate;
  }

  @Override
  public Place getFrom() {
    return Place.forFlexStop(edge.s1, edge.getFromVertex());
  }

  @Override
  public Place getTo() {
    return Place.forFlexStop(edge.s2, edge.getToVertex());
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
  public PickDrop getBoardRule() {
    return edge.getFlexTrip().getBoardRule(getBoardStopPosInPattern());
  }

  @Override
  public PickDrop getAlightRule() {
    return edge.getFlexTrip().getAlightRule(getAlightStopPosInPattern());
  }

  @Override
  public BookingInfo getDropOffBookingInfo() {
    return edge.getFlexTrip().getDropOffBookingInfo(getBoardStopPosInPattern());
  }

  @Override
  public BookingInfo getPickupBookingInfo() {
    return edge.getFlexTrip().getPickupBookingInfo(getAlightStopPosInPattern());
  }

  @Override
  public Integer getBoardStopPosInPattern() {
    return edge.flexTemplate.fromStopIndex;
  }

  @Override
  public Integer getAlightStopPosInPattern() {
    return edge.flexTemplate.toStopIndex;
  }

  @Override
  public int getGeneralizedCost() {
    return generalizedCost;
  }

  public void addAlert(TransitAlert alert) {
    transitAlerts.add(alert);
  }

  @Override
  public Leg withTimeShift(Duration duration) {
    FlexibleTransitLeg copy = new FlexibleTransitLeg(
      edge,
      startTime.plus(duration),
      endTime.plus(duration),
      generalizedCost
    );

    for (TransitAlert alert : transitAlerts) {
      copy.addAlert(alert);
    }

    return copy;
  }

  @Override
  public void setFareProducts(List<FareProductUse> products) {
    this.fareProducts = List.copyOf(products);
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
}
