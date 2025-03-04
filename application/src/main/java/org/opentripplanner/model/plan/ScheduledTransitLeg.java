package org.opentripplanner.model.plan;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.fare.FareProductUse;
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
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.lang.Sandbox;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

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
  private final Set<TransitAlert> transitAlerts;
  private final ConstrainedTransfer transferFromPrevLeg;
  private final ConstrainedTransfer transferToNextLeg;
  protected final int boardStopPosInPattern;
  protected final int alightStopPosInPattern;
  private final int generalizedCost;
  protected final LocalDate serviceDate;
  protected final ZoneId zoneId;
  private final TripOnServiceDate tripOnServiceDate;
  private final double distanceMeters;
  private final double directDistanceMeters;
  private final Float accessibilityScore;
  private final List<FareProductUse> fareProducts;

  protected ScheduledTransitLeg(ScheduledTransitLegBuilder<?> builder) {
    this.tripTimes = builder.tripTimes();
    this.tripPattern = builder.tripPattern();

    this.boardStopPosInPattern = builder.boardStopIndexInPattern();
    this.alightStopPosInPattern = builder.alightStopIndexInPattern();

    this.startTime = builder.startTime();
    this.endTime = builder.endTime();

    this.serviceDate = builder.serviceDate();
    this.zoneId = Objects.requireNonNull(builder.zoneId(), "zoneId");

    this.tripOnServiceDate = builder.tripOnServiceDate();

    this.transferFromPrevLeg = builder.transferFromPreviousLeg();
    this.transferToNextLeg = builder.transferToNextLeg();

    this.generalizedCost = builder.generalizedCost();

    this.accessibilityScore = builder.accessibilityScore();
    List<Coordinate> transitLegCoordinates = LegConstructionSupport.extractTransitLegCoordinates(
      tripPattern,
      builder.boardStopIndexInPattern(),
      builder.alightStopIndexInPattern()
    );
    this.legGeometry = GeometryUtils.makeLineString(transitLegCoordinates);

    this.distanceMeters = DoubleUtils.roundTo2Decimals(
      Objects.requireNonNull(builder.distanceMeters(), "distanceMeters")
    );
    this.directDistanceMeters = GeometryUtils.sumDistances(
      List.of(transitLegCoordinates.getFirst(), transitLegCoordinates.getLast())
    );
    this.transitAlerts = Set.copyOf(builder.alerts());
    this.fareProducts = List.copyOf(builder.fareProducts());
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

  @Override
  public boolean isScheduledTransitLeg() {
    return true;
  }

  @Override
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
    return trip().getRoute().getAgency();
  }

  @Override
  @Nullable
  public Operator getOperator() {
    return trip().getOperator();
  }

  @Override
  public Route getRoute() {
    return trip().getRoute();
  }

  @Override
  public Trip getTrip() {
    return trip();
  }

  @Override
  public Accessibility getTripWheelchairAccessibility() {
    return tripTimes.getWheelchairAccessibility();
  }

  @Override
  public LegCallTime start() {
    if (isRealTimeUpdated()) {
      return LegCallTime.of(startTime, getDepartureDelay());
    } else {
      return LegCallTime.ofStatic(startTime);
    }
  }

  @Override
  public LegCallTime end() {
    if (isRealTimeUpdated()) {
      return LegCallTime.of(endTime, getArrivalDelay());
    } else {
      return LegCallTime.ofStatic(endTime);
    }
  }

  @Override
  public TransitMode getMode() {
    return trip().getMode();
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
  public boolean isRealTimeUpdated() {
    return (
      tripTimes.isRealTimeUpdated(boardStopPosInPattern) ||
      tripTimes.isRealTimeUpdated(alightStopPosInPattern)
    );
  }

  @Override
  public RealTimeState getRealTimeState() {
    return tripTimes.getRealTimeState();
  }

  @Override
  public double getDistanceMeters() {
    return distanceMeters;
  }

  public double getDirectDistanceMeters() {
    return directDistanceMeters;
  }

  @Override
  public Integer getRouteType() {
    return trip().getRoute().getGtfsType();
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
  @Nullable
  public TripOnServiceDate getTripOnServiceDate() {
    return tripOnServiceDate;
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
    var mapper = new StopArrivalMapper(zoneId, serviceDate, tripTimes);

    for (int i = boardStopPosInPattern + 1; i < alightStopPosInPattern; i++) {
      StopLocation stop = tripPattern.getStop(i);
      final StopArrival visit = mapper.map(i, stop, isRealTimeUpdated());
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
  public ScheduledTransitLeg decorateWithAlerts(Set<TransitAlert> alerts) {
    return copy().withAlerts(alerts).build();
  }

  @Override
  public TransitLeg decorateWithFareProducts(List<FareProductUse> fares) {
    return copy().withFareProducts(fares).build();
  }

  @Override
  @Nullable
  public PickDrop getBoardRule() {
    if (transferFromPrevLeg != null && transferFromPrevLeg.getTransferConstraint().isStaySeated()) {
      return null;
    }
    return tripPattern.getBoardType(boardStopPosInPattern);
  }

  @Override
  @Nullable
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
    return tripTimes.gtfsSequenceOfStopIndex(boardStopPosInPattern);
  }

  @Override
  public Integer getAlightGtfsStopSequence() {
    return tripTimes.gtfsSequenceOfStopIndex(alightStopPosInPattern);
  }

  @Override
  public int getGeneralizedCost() {
    return generalizedCost;
  }

  /**
   * Construct a leg reference from this leg.
   * If the trip is based on a TripOnServiceDate, the leg reference will contain the
   * TripOnServiceDate id instead of the Trip id.
   */
  @Override
  public LegReference getLegReference() {
    return new ScheduledTransitLegReference(
      tripOnServiceDate == null ? tripTimes.getTrip().getId() : null,
      serviceDate,
      boardStopPosInPattern,
      alightStopPosInPattern,
      tripPattern.getStops().get(boardStopPosInPattern).getId(),
      tripPattern.getStops().get(alightStopPosInPattern).getId(),
      tripOnServiceDate == null ? null : tripOnServiceDate.getId()
    );
  }

  @Override
  public List<FareProductUse> fareProducts() {
    return fareProducts;
  }

  @Override
  @Nullable
  @Sandbox
  public Float accessibilityScore() {
    return accessibilityScore;
  }

  public ScheduledTransitLegBuilder copy() {
    return new ScheduledTransitLegBuilder<>(this);
  }

  /**
   * Should be used for debug logging only
   * <p>
   * The {@code legGeometry} and {@code transitAlerts} are skipped to avoid
   * spamming logs. Explicit access should be used if needed.
   */
  @Override
  public String toString() {
    return ToStringBuilder.of(ScheduledTransitLeg.class)
      .addObj("from", getFrom())
      .addObj("to", getTo())
      .addTime("startTime", startTime)
      .addTime("endTime", endTime)
      .addBool("realTime", isRealTimeUpdated())
      .addNum("distance", distanceMeters, "m")
      .addNum("cost", generalizedCost)
      .addNum("routeType", getRouteType())
      .addObjOp("agencyId", getAgency(), AbstractTransitEntity::getId)
      .addObjOp("routeId", getRoute(), AbstractTransitEntity::getId)
      .addObjOp("tripId", getTrip(), AbstractTransitEntity::getId)
      .addObj("headsign", getHeadsign())
      .addObj("serviceDate", serviceDate)
      .addColSize("transitAlerts", transitAlerts)
      .addEnum("boardRule", getBoardRule())
      .addEnum("alightRule", getAlightRule())
      .addObj("transferFromPrevLeg", transferFromPrevLeg)
      .addObj("transferToNextLeg", transferToNextLeg)
      .toString();
  }

  /**
   * Non-null getter for trip
   */
  private Trip trip() {
    return tripTimes.getTrip();
  }
}
