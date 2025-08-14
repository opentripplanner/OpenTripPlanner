package org.opentripplanner.model.plan.leg;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.plan.Leg;
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
import org.opentripplanner.utils.lang.IntUtils;
import org.opentripplanner.utils.lang.Sandbox;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle.
 */
public class ScheduledTransitLeg implements TransitLeg {

  private static final int ZERO = 0;
  /**
   * A leg's fare offers don't really have any particular order, but it's nice if the order remains
   * the same for two identical collections.
   */
  private static final Comparator<FareOffer> FARE_OFFER_COMPARATOR = Comparator.comparing(
    (FareOffer o) -> o.fareProduct().id().getId()
  ).thenComparing(FareOffer::uniqueId);
  private final TripTimes tripTimes;
  private final TripPattern tripPattern;

  private final ZonedDateTime startTime;
  private final ZonedDateTime endTime;
  private final LineString legGeometry;
  private final Set<TransitAlert> transitAlerts;
  private final ConstrainedTransfer transferFromPrevLeg;
  private final ConstrainedTransfer transferToNextLeg;
  // the positions are protected to avoid boxing/unboxing when used in subclasses
  protected final int boardStopPosInPattern;
  protected final int alightStopPosInPattern;
  private final int generalizedCost;
  private final LocalDate serviceDate;
  private final ZoneId zoneId;
  private final TripOnServiceDate tripOnServiceDate;
  private final double distanceMeters;
  private final double directDistanceMeters;

  // Sandbox fields
  private final Float accessibilityScore;
  private final Emission emissionPerPerson;
  private final List<FareOffer> fareOffers;

  protected ScheduledTransitLeg(ScheduledTransitLegBuilder<?> builder) {
    // TODO - Add requireNonNull for trip-times. Some tests fails when this is done, these tests
    //        should be fixed.
    this.tripTimes = builder.tripTimes();
    this.tripPattern = Objects.requireNonNull(builder.tripPattern());

    int maxStopPosInPatternLimit = tripPattern.numberOfStops() - 1;
    this.boardStopPosInPattern = IntUtils.requireInRange(
      builder.boardStopIndexInPattern(),
      ZERO,
      maxStopPosInPatternLimit,
      "boardStopPosInPattern"
    );
    this.alightStopPosInPattern = IntUtils.requireInRange(
      builder.alightStopIndexInPattern(),
      boardStopPosInPattern + 1,
      maxStopPosInPatternLimit,
      "alightStopPosInPattern"
    );

    this.startTime = Objects.requireNonNull(builder.startTime());
    this.endTime = Objects.requireNonNull(builder.endTime());
    this.serviceDate = Objects.requireNonNull(builder.serviceDate());
    this.zoneId = Objects.requireNonNull(builder.zoneId());

    this.tripOnServiceDate = builder.tripOnServiceDate();

    this.transferFromPrevLeg = builder.transferFromPreviousLeg();
    this.transferToNextLeg = builder.transferToNextLeg();

    this.generalizedCost = builder.generalizedCost();

    List<Coordinate> transitLegCoordinates = LegConstructionSupport.extractTransitLegCoordinates(
      tripPattern,
      boardStopPosInPattern,
      alightStopPosInPattern
    );
    this.legGeometry = GeometryUtils.makeLineString(transitLegCoordinates);

    this.distanceMeters = DoubleUtils.roundTo2Decimals(
      Objects.requireNonNull(builder.distanceMeters(), "distanceMeters")
    );
    this.directDistanceMeters = GeometryUtils.sumDistances(
      List.of(transitLegCoordinates.getFirst(), transitLegCoordinates.getLast())
    );
    this.transitAlerts = Set.copyOf(builder.alerts());

    // Sandbox
    this.accessibilityScore = builder.accessibilityScore();
    this.emissionPerPerson = builder.emissionPerPerson();
    this.fareOffers = builder.fareOffers().stream().sorted(FARE_OFFER_COMPARATOR).toList();
  }

  public ScheduledTransitLegBuilder copyOf() {
    return new ScheduledTransitLegBuilder<>(this);
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
    return copyOf().withAlerts(alerts).build();
  }

  @Override
  public TransitLeg decorateWithFareOffers(List<FareOffer> fares) {
    return copyOf().withFareProducts(fares).build();
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
  @Nullable
  @Sandbox
  public Float accessibilityScore() {
    return accessibilityScore;
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
  public List<FareOffer> fareOffers() {
    return fareOffers;
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
      .addCost("generalizedCost", generalizedCost, Cost.ZERO.toSeconds())
      .addObjOp("agencyId", agency(), AbstractTransitEntity::getId)
      .addNum("routeType", routeType())
      .addObjOp("routeId", route(), AbstractTransitEntity::getId)
      .addObjOp("tripId", this.trip(), AbstractTransitEntity::getId)
      .addObj("headsign", headsign())
      .addObj("serviceDate", serviceDate)
      .addEnum("boardRule", boardRule())
      .addEnum("alightRule", alightRule())
      .addObj("transferFromPrevLeg", transferFromPrevLeg)
      .addObj("transferToNextLeg", transferToNextLeg)
      .addColSize("transitAlerts", transitAlerts)
      .addObj("emissionPerPerson", emissionPerPerson)
      .addColSize("fareProducts", fareOffers)
      .toString();
  }
}
