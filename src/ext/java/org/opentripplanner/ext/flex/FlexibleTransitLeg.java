package org.opentripplanner.ext.flex;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.transit.model.basic.TransitEntity;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.util.lang.ToStringBuilder;

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle, which is running on flexible trip, i.e. not using fixed schedule and stops.
 */
public class FlexibleTransitLeg implements Leg {

  private final FlexTripEdge edge;

  private final ZonedDateTime startTime;

  private final ZonedDateTime endTime;

  private final Set<TransitAlert> transitAlerts = new HashSet<>();

  private final int generalizedCost;

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
  public boolean isTransitLeg() {
    return true;
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
  public TraverseMode getMode() {
    return TraverseMode.fromTransitMode(getTrip().getMode());
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
    return edge.getDistanceMeters();
  }

  @Override
  public Integer getRouteType() {
    return getTrip().getRoute().getGtfsType();
  }

  @Override
  public String getHeadsign() {
    return getTrip().getTripHeadsign();
  }

  @Override
  public ServiceDate getServiceDate() {
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

  /**
   * Should be used for debug logging only
   */
  @Override
  public String toString() {
    return ToStringBuilder
      .of(FlexibleTransitLeg.class)
      .addObj("from", getFrom())
      .addObj("to", getTo())
      .addTimeCal("startTime", startTime)
      .addTimeCal("endTime", endTime)
      .addNum("distance", getDistanceMeters(), "m")
      .addNum("cost", generalizedCost)
      .addObjOp("agencyId", getAgency(), TransitEntity::getId)
      .addObjOp("routeId", getRoute(), TransitEntity::getId)
      .addObjOp("tripId", getTrip(), TransitEntity::getId)
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
