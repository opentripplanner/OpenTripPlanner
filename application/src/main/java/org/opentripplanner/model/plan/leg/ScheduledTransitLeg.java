package org.opentripplanner.model.plan.leg;

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
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.TransitLeg;
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

  public ZoneId zoneId() {
    return zoneId;
  }

  public TripTimes tripTimes() {
    return tripTimes;
  }

  public TripPattern tripPattern() {
    return tripPattern;
  }

  public Instant serviceDateMidnight() {
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
    return tripTimes.getTrip();
  }

  @Override
  public Accessibility tripWheelchairAccessibility() {
    return tripTimes.getWheelchairAccessibility();
  }

  @Override
  public LegCallTime start() {
    if (isRealTimeUpdated()) {
      return LegCallTime.of(startTime, departureDelay());
    } else {
      return LegCallTime.ofStatic(startTime);
    }
  }

  @Override
  public LegCallTime end() {
    if (isRealTimeUpdated()) {
      return LegCallTime.of(endTime, arrivalDelay());
    } else {
      return LegCallTime.ofStatic(endTime);
    }
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
  public int departureDelay() {
    return (
        tripTimes.isCancelledStop(boardStopPosInPattern) ||
        tripTimes.isNoDataStop(boardStopPosInPattern)
      )
      ? 0
      : tripTimes.getDepartureDelay(boardStopPosInPattern);
  }

  @Override
  public int arrivalDelay() {
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
  public RealTimeState realTimeState() {
    return tripTimes.getRealTimeState();
  }

  @Override
  public double distanceMeters() {
    return distanceMeters;
  }

  public double directDistanceMeters() {
    return directDistanceMeters;
  }

  @Override
  public Integer routeType() {
    return trip().getRoute().getGtfsType();
  }

  @Override
  public I18NString headsign() {
    return tripTimes.getHeadsign(boardStopPosInPattern);
  }

  @Override
  public LocalDate serviceDate() {
    return serviceDate;
  }

  @Override
  @Nullable
  public TripOnServiceDate tripOnServiceDate() {
    return tripOnServiceDate;
  }

  @Override
  public Place from() {
    return Place.forStop(tripPattern.getStop(boardStopPosInPattern));
  }

  @Override
  public Place to() {
    return Place.forStop(tripPattern.getStop(alightStopPosInPattern));
  }

  @Override
  public List<StopArrival> listIntermediateStops() {
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
  public LineString legGeometry() {
    return legGeometry;
  }

  @Override
  public Set<TransitAlert> listTransitAlerts() {
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
  public PickDrop boardRule() {
    if (transferFromPrevLeg != null && transferFromPrevLeg.getTransferConstraint().isStaySeated()) {
      return null;
    }
    return tripPattern.getBoardType(boardStopPosInPattern);
  }

  @Override
  @Nullable
  public PickDrop alightRule() {
    if (transferToNextLeg != null && transferToNextLeg.getTransferConstraint().isStaySeated()) {
      return null;
    }
    return tripPattern.getAlightType(alightStopPosInPattern);
  }

  @Override
  public BookingInfo dropOffBookingInfo() {
    return tripTimes.getDropOffBookingInfo(alightStopPosInPattern);
  }

  @Override
  public BookingInfo pickupBookingInfo() {
    return tripTimes.getPickupBookingInfo(boardStopPosInPattern);
  }

  @Override
  public ConstrainedTransfer transferFromPrevLeg() {
    return transferFromPrevLeg;
  }

  @Override
  public ConstrainedTransfer transferToNextLeg() {
    return transferToNextLeg;
  }

  @Override
  public Integer boardStopPosInPattern() {
    return boardStopPosInPattern;
  }

  @Override
  public Integer alightStopPosInPattern() {
    return alightStopPosInPattern;
  }

  @Override
  public Integer boardingGtfsStopSequence() {
    return tripTimes.gtfsSequenceOfStopIndex(boardStopPosInPattern);
  }

  @Override
  public Integer alightGtfsStopSequence() {
    return tripTimes.gtfsSequenceOfStopIndex(alightStopPosInPattern);
  }

  @Override
  public int generalizedCost() {
    return generalizedCost;
  }

  /**
   * Construct a leg reference from this leg.
   * If the trip is based on a TripOnServiceDate, the leg reference will contain the
   * TripOnServiceDate id instead of the Trip id.
   */
  @Override
  public LegReference legReference() {
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
      .addObj("from", from())
      .addObj("to", to())
      .addTime("startTime", startTime)
      .addTime("endTime", endTime)
      .addBool("realTime", isRealTimeUpdated())
      .addNum("distance", distanceMeters, "m")
      .addNum("cost", generalizedCost)
      .addNum("routeType", routeType())
      .addObjOp("agencyId", agency(), AbstractTransitEntity::getId)
      .addObjOp("routeId", route(), AbstractTransitEntity::getId)
      .addObjOp("tripId", this.trip(), AbstractTransitEntity::getId)
      .addObj("headsign", headsign())
      .addObj("serviceDate", serviceDate)
      .addColSize("transitAlerts", transitAlerts)
      .addEnum("boardRule", boardRule())
      .addEnum("alightRule", alightRule())
      .addObj("transferFromPrevLeg", transferFromPrevLeg)
      .addObj("transferToNextLeg", transferToNextLeg)
      .toString();
  }
}
