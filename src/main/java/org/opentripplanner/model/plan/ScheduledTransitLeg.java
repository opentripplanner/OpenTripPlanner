package org.opentripplanner.model.plan;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.model.plan.legreference.ScheduledTransitLegReference;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle.
 */
public class ScheduledTransitLeg implements TransitLeg {

  protected final TripTimes tripTimes;
  protected final TripPattern tripPattern;

  private final ZonedDateTime startTime;
  private final ZonedDateTime endTime;
  private final LineString legGeometry;
  private final Set<TransitAlert> transitAlerts = new HashSet<>();
  private final ConstrainedTransfer transferFromPrevLeg;
  private final ConstrainedTransfer transferToNextLeg;
  protected final Integer boardStopPosInPattern;
  protected final Integer alightStopPosInPattern;
  private final int generalizedCost;
  protected final LocalDate serviceDate;
  protected final ZoneId zoneId;
  private double distanceMeters;
  private double directDistanceMeters;
  private final Float accessibilityScore;

  public ScheduledTransitLeg(
    TripTimes tripTimes,
    TripPattern tripPattern,
    int boardStopIndexInPattern,
    int alightStopIndexInPattern,
    ZonedDateTime startTime,
    ZonedDateTime endTime,
    LocalDate serviceDate,
    ZoneId zoneId,
    ConstrainedTransfer transferFromPreviousLeg,
    ConstrainedTransfer transferToNextLeg,
    int generalizedCost,
    @Nullable Float accessibilityScore
  ) {
    this.tripTimes = tripTimes;
    this.tripPattern = tripPattern;

    this.boardStopPosInPattern = boardStopIndexInPattern;
    this.alightStopPosInPattern = alightStopIndexInPattern;

    this.startTime = startTime;
    this.endTime = endTime;

    this.serviceDate = serviceDate;
    this.zoneId = zoneId;

    this.transferFromPrevLeg = transferFromPreviousLeg;
    this.transferToNextLeg = transferToNextLeg;

    this.generalizedCost = generalizedCost;

    this.accessibilityScore = accessibilityScore;
    List<Coordinate> transitLegCoordinates = extractTransitLegCoordinates(
      tripPattern,
      boardStopIndexInPattern,
      alightStopIndexInPattern
    );
    this.legGeometry = GeometryUtils.makeLineString(transitLegCoordinates);

    setDistanceMeters(getDistanceFromCoordinates(transitLegCoordinates));
    this.directDistanceMeters =
      getDistanceFromCoordinates(
        List.of(
          transitLegCoordinates.get(0),
          transitLegCoordinates.get(transitLegCoordinates.size() - 1)
        )
      );
  }

  public ZoneId getZoneId() {
    return zoneId;
  }

  public TripTimes getTripTimes() {
    return tripTimes;
  }

  public TripPattern getTripPattern() {
    return tripPattern;
  }

  public Instant getServiceDateMidnight() {
    return ServiceDateUtils.asStartOfService(serviceDate, zoneId).toInstant();
  }

  public boolean isScheduledTransitLeg() {
    return true;
  }

  public ScheduledTransitLeg asScheduledTransitLeg() {
    return this;
  }

  @Override
  public Boolean isInterlinedWithPreviousLeg() {
    if (transferFromPrevLeg == null) {
      return false;
    }
    return transferFromPrevLeg.getTransferConstraint().isStaySeated();
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
    return tripTimes.getTrip();
  }

  @Override
  public Accessibility getTripWheelchairAccessibility() {
    return tripTimes.getWheelchairAccessibility();
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
  public int getDepartureDelay() {
    return (
        tripTimes.isCancelledStop(boardStopPosInPattern) ||
        tripTimes.isNoDataStop(boardStopPosInPattern)
      )
      ? 0
      : tripTimes.getDepartureDelay(boardStopPosInPattern);
  }

  @Override
  public int getArrivalDelay() {
    return (
        tripTimes.isCancelledStop(alightStopPosInPattern) ||
        tripTimes.isNoDataStop(alightStopPosInPattern)
      )
      ? 0
      : tripTimes.getArrivalDelay(alightStopPosInPattern);
  }

  @Override
  public boolean getRealTime() {
    return (
      !tripTimes.isScheduled() &&
      (
        !tripTimes.isNoDataStop(boardStopPosInPattern) ||
        !tripTimes.isNoDataStop(alightStopPosInPattern)
      )
    );
  }

  @Override
  public double getDistanceMeters() {
    return distanceMeters;
  }

  /** Only for testing purposes */
  protected void setDistanceMeters(double distanceMeters) {
    this.distanceMeters = DoubleUtils.roundTo2Decimals(distanceMeters);
  }

  public double getDirectDistanceMeters() {
    return directDistanceMeters;
  }

  @Override
  public Integer getRouteType() {
    return getTrip().getRoute().getGtfsType();
  }

  @Override
  public I18NString getHeadsign() {
    return tripTimes.getHeadsign(boardStopPosInPattern);
  }

  @Override
  public LocalDate getServiceDate() {
    return serviceDate;
  }

  @Override
  public Place getFrom() {
    return Place.forStop(tripPattern.getStop(boardStopPosInPattern));
  }

  @Override
  public Place getTo() {
    return Place.forStop(tripPattern.getStop(alightStopPosInPattern));
  }

  @Override
  public List<StopArrival> getIntermediateStops() {
    List<StopArrival> visits = new ArrayList<>();

    for (int i = boardStopPosInPattern + 1; i < alightStopPosInPattern; i++) {
      StopLocation stop = tripPattern.getStop(i);

      StopArrival visit = new StopArrival(
        Place.forStop(stop),
        ServiceDateUtils.toZonedDateTime(serviceDate, zoneId, tripTimes.getArrivalTime(i)),
        ServiceDateUtils.toZonedDateTime(serviceDate, zoneId, tripTimes.getDepartureTime(i)),
        i,
        tripTimes.getOriginalGtfsStopSequence(i)
      );
      visits.add(visit);
    }
    return visits;
  }

  @Override
  public LineString getLegGeometry() {
    return legGeometry;
  }

  @Override
  public Set<TransitAlert> getTransitAlerts() {
    return transitAlerts;
  }

  @Override
  public PickDrop getBoardRule() {
    if (transferFromPrevLeg != null && transferFromPrevLeg.getTransferConstraint().isStaySeated()) {
      return null;
    }
    return tripPattern.getBoardType(boardStopPosInPattern);
  }

  @Override
  public PickDrop getAlightRule() {
    if (transferToNextLeg != null && transferToNextLeg.getTransferConstraint().isStaySeated()) {
      return null;
    }
    return tripPattern.getAlightType(alightStopPosInPattern);
  }

  @Override
  public BookingInfo getDropOffBookingInfo() {
    return tripTimes.getDropOffBookingInfo(alightStopPosInPattern);
  }

  @Override
  public BookingInfo getPickupBookingInfo() {
    return tripTimes.getPickupBookingInfo(boardStopPosInPattern);
  }

  @Override
  public ConstrainedTransfer getTransferFromPrevLeg() {
    return transferFromPrevLeg;
  }

  @Override
  public ConstrainedTransfer getTransferToNextLeg() {
    return transferToNextLeg;
  }

  @Override
  public Integer getBoardStopPosInPattern() {
    return boardStopPosInPattern;
  }

  @Override
  public Integer getAlightStopPosInPattern() {
    return alightStopPosInPattern;
  }

  @Override
  public Integer getBoardingGtfsStopSequence() {
    return tripTimes.getOriginalGtfsStopSequence(boardStopPosInPattern);
  }

  @Override
  public Integer getAlightGtfsStopSequence() {
    return tripTimes.getOriginalGtfsStopSequence(alightStopPosInPattern);
  }

  @Override
  public int getGeneralizedCost() {
    return generalizedCost;
  }

  @Override
  public LegReference getLegReference() {
    return new ScheduledTransitLegReference(
      tripTimes.getTrip().getId(),
      serviceDate,
      boardStopPosInPattern,
      alightStopPosInPattern
    );
  }

  public void addAlert(TransitAlert alert) {
    transitAlerts.add(alert);
  }

  @Override
  @Nullable
  public Float accessibilityScore() {
    return accessibilityScore;
  }

  public ScheduledTransitLeg withAccessibilityScore(Float score) {
    return new ScheduledTransitLeg(
      tripTimes,
      tripPattern,
      boardStopPosInPattern,
      alightStopPosInPattern,
      startTime,
      endTime,
      serviceDate,
      zoneId,
      transferFromPrevLeg,
      transferToNextLeg,
      generalizedCost,
      score
    );
  }

  /**
   * Should be used for debug logging only
   */
  @Override
  public String toString() {
    return ToStringBuilder
      .of(ScheduledTransitLeg.class)
      .addObj("from", getFrom())
      .addObj("to", getTo())
      .addTime("startTime", startTime)
      .addTime("endTime", endTime)
      .addBool("realTime", getRealTime())
      .addNum("distance", distanceMeters, "m")
      .addNum("cost", generalizedCost)
      .addNum("routeType", getRouteType())
      .addObjOp("agencyId", getAgency(), AbstractTransitEntity::getId)
      .addObjOp("routeId", getRoute(), AbstractTransitEntity::getId)
      .addObjOp("tripId", getTrip(), AbstractTransitEntity::getId)
      .addObj("headsign", getHeadsign())
      .addObj("serviceDate", serviceDate)
      .addObj("legGeometry", legGeometry)
      .addCol("transitAlerts", transitAlerts)
      .addEnum("boardRule", getBoardRule())
      .addEnum("alightRule", getAlightRule())
      .addObj("transferFromPrevLeg", transferFromPrevLeg)
      .addObj("transferToNextLeg", transferToNextLeg)
      .toString();
  }

  private List<Coordinate> extractTransitLegCoordinates(
    TripPattern tripPattern,
    int boardStopIndexInPattern,
    int alightStopIndexInPattern
  ) {
    List<Coordinate> transitLegCoordinates = new ArrayList<>();

    for (int i = boardStopIndexInPattern + 1; i <= alightStopIndexInPattern; i++) {
      transitLegCoordinates.addAll(
        Arrays.asList(tripPattern.getHopGeometry(i - 1).getCoordinates())
      );
    }

    return transitLegCoordinates;
  }

  private double getDistanceFromCoordinates(List<Coordinate> coordinates) {
    double distance = 0;
    for (int i = 1; i < coordinates.size(); i++) {
      distance += SphericalDistanceLibrary.distance(coordinates.get(i), coordinates.get(i - 1));
    }
    return distance;
  }
}
