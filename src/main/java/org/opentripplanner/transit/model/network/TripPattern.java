package org.opentripplanner.transit.model.network;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;
import static org.opentripplanner.framework.lang.ObjectUtils.requireNotInitialized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.CompactLineStringUtils;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.LogInfo;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO OTP2 instances of this class are still mutable after construction with a builder, this will be refactored in a subsequent step
/**
 * Represents a group of trips on a route, with the same direction id that all call at the same
 * sequence of stops. For each stop, there is a list of departure times, running times, arrival
 * times, dwell times, and wheelchair accessibility information (one of each of these per trip per
 * stop). Trips are assumed to be non-overtaking, so that an earlier trip never arrives after a
 * later trip.
 * <p>
 * This is called a JOURNEY_PATTERN in the Transmodel vocabulary. However, GTFS calls a Transmodel
 * JOURNEY a "trip", thus TripPattern.
 * <p>
 * The {@code id} is a unique identifier for this trip pattern. For GTFS feeds this is generally
 * generated in the format FeedId:Agency:RouteId:DirectionId:PatternNumber. For NeTEx the
 * JourneyPattern id is used.
 */
public final class TripPattern
  extends AbstractTransitEntity<TripPattern, TripPatternBuilder>
  implements Cloneable, LogInfo {

  private static final Logger LOG = LoggerFactory.getLogger(TripPattern.class);

  private final Route route;
  /**
   * The stop-pattern help us reuse the same stops in several trip-patterns; Hence saving memory.
   * The field should not be accessible outside the class, and all access is done through method
   * delegation, like the {@link #numberOfStops()} and {@link #canBoard(int)} methods.
   */
  private final StopPattern stopPattern;
  private final Timetable scheduledTimetable;
  private final TransitMode mode;
  private final SubMode netexSubMode;
  private final boolean containsMultipleModes;
  private String name;
  /**
   * Geometries of each inter-stop segment of the tripPattern.
   */
  private final byte[][] hopGeometries;

  /**
   * The original TripPattern this replaces at least for one modified trip.
   */
  private final TripPattern originalTripPattern;

  /**
   * Has the TripPattern been created by a real-time update.
   */
  private final boolean createdByRealtimeUpdater;

  private final RoutingTripPattern routingTripPattern;

  public TripPattern(TripPatternBuilder builder) {
    super(builder.getId());
    this.name = builder.getName();
    this.route = builder.getRoute();
    this.stopPattern = requireNonNull(builder.getStopPattern());
    this.createdByRealtimeUpdater = builder.isCreatedByRealtimeUpdate();
    this.mode = requireNonNullElseGet(builder.getMode(), route::getMode);
    this.netexSubMode = requireNonNullElseGet(builder.getNetexSubmode(), route::getNetexSubmode);
    this.containsMultipleModes = builder.getContainsMultipleModes();

    this.scheduledTimetable =
      builder.getScheduledTimetable() != null
        ? builder.getScheduledTimetable()
        : new Timetable(this);

    this.originalTripPattern = builder.getOriginalTripPattern();

    this.hopGeometries = builder.hopGeometries();
    this.routingTripPattern = new RoutingTripPattern(this, builder);
  }

  public static TripPatternBuilder of(@Nonnull FeedScopedId id) {
    return new TripPatternBuilder(id);
  }

  /** The human-readable, unique name for this trip pattern. */
  public String getName() {
    return name;
  }

  public void initName(String name) {
    this.name = requireNotInitialized(this.name, name);
  }

  /**
   * The GTFS Route of all trips in this pattern.
   */
  public Route getRoute() {
    return route;
  }

  /**
   * Get the mode for all trips in this pattern.
   */
  public TransitMode getMode() {
    return mode;
  }

  public SubMode getNetexSubmode() {
    return netexSubMode;
  }

  public boolean getContainsMultipleModes() {
    return containsMultipleModes;
  }

  public LineString getHopGeometry(int stopPosInPattern) {
    if (hopGeometries != null) {
      return CompactLineStringUtils.uncompactLineString(hopGeometries[stopPosInPattern], false);
    } else {
      return GeometryUtils
        .getGeometryFactory()
        .createLineString(
          new Coordinate[] {
            coordinate(stopPattern.getStop(stopPosInPattern)),
            coordinate(stopPattern.getStop(stopPosInPattern + 1)),
          }
        );
    }
  }

  public StopPattern getStopPattern() {
    return stopPattern;
  }

  /**
   * Return the "original"/planned stop pattern as a builder. This is used when a realtime-update
   * contains a full set of stops/pickup/dropoff for a pattern. This will wipe out any changes
   * to the stop-pattern from previous updates.
   * <p>
   * Be aware, if the same update is applied twice, then the first instance will be reused to avoid
   * unnecessary objects creation and gc.
   */
  public StopPattern.StopPatternBuilder copyPlannedStopPattern() {
    return isModified()
      ? originalTripPattern.stopPattern.mutate(stopPattern)
      : stopPattern.mutate();
  }

  public LineString getGeometry() {
    if (hopGeometries == null || hopGeometries.length == 0) {
      return null;
    }

    List<LineString> lineStrings = new ArrayList<>();
    for (int i = 0; i < hopGeometries.length; i++) {
      lineStrings.add(getHopGeometry(i));
    }
    return GeometryUtils.concatenateLineStrings(lineStrings);
  }

  public int numberOfStops() {
    return stopPattern.getSize();
  }

  public StopLocation getStop(int stopPosInPattern) {
    return stopPattern.getStop(stopPosInPattern);
  }

  public StopLocation firstStop() {
    return getStop(0);
  }

  public StopLocation lastStop() {
    return getStop(stopPattern.getSize() - 1);
  }

  /** Read only list of stops */
  public List<StopLocation> getStops() {
    return stopPattern.getStops();
  }

  /**
   * Find the first stop position in pattern matching the given {@code stop}. The search start at
   * position {@code 0}. Return a negative number if not found. Use
   * {@link #findAlightStopPositionInPattern(StopLocation)} or
   * {@link #findBoardingStopPositionInPattern(StopLocation)} if possible.
   */
  public int findStopPosition(StopLocation stop) {
    return stopPattern.findStopPosition(stop);
  }

  /**
   * Find the first stop position in pattern matching the given {@code station} where it is allowed
   * to board. The search start at position {@code 0}. Return a negative number if not found.
   */
  public int findBoardingStopPositionInPattern(Station station) {
    return stopPattern.findBoardingPosition(station);
  }

  /**
   * Find the first stop position in pattern matching the given {@code station} where it is allowed
   * to alight. The search start at position {@code 1}. Return a negative number if not found.
   */
  public int findAlightStopPositionInPattern(Station station) {
    return stopPattern.findAlightPosition(station);
  }

  /**
   * Find the first stop position in pattern matching the given {@code stop} where it is allowed to
   * board. The search start at position {@code 0}. Return a negative number if not found.
   */
  public int findBoardingStopPositionInPattern(StopLocation stop) {
    return stopPattern.findBoardingPosition(stop);
  }

  /**
   * Find the first stop position in pattern matching the given {@code stop} where it is allowed to
   * alight. The search start at position {@code 1}. Return a negative number if not found.
   */
  public int findAlightStopPositionInPattern(StopLocation stop) {
    return stopPattern.findAlightPosition(stop);
  }

  /** Returns whether passengers can alight at a given stop */
  public boolean canAlight(int stopIndex) {
    return stopPattern.canAlight(stopIndex);
  }

  /** Returns whether passengers can board at a given stop */
  public boolean canBoard(int stopIndex) {
    return stopPattern.canBoard(stopIndex);
  }

  /**
   * Returns whether passengers can board at a given stop. This is an inefficient method iterating
   * over the stops, do not use it in routing.
   */
  public boolean canBoard(StopLocation stop) {
    return stopPattern.canBoard(stop);
  }

  /**
   * Returns whether passengers can alight at a given stop. This is an inefficient method iterating
   * over the stops, do not use it in routing.
   */
  public boolean canAlight(StopLocation stop) {
    return stopPattern.canAlight(stop);
  }

  /** Returns whether a given stop is wheelchair-accessible. */
  public boolean wheelchairAccessible(int stopIndex) {
    return (stopPattern.getStop(stopIndex).getWheelchairAccessibility() == Accessibility.POSSIBLE);
  }

  public PickDrop getAlightType(int stopIndex) {
    return stopPattern.getDropoff(stopIndex);
  }

  public PickDrop getBoardType(int stopIndex) {
    return stopPattern.getPickup(stopIndex);
  }

  public boolean isBoardAndAlightAt(int stopIndex, PickDrop value) {
    return getBoardType(stopIndex).is(value) && getAlightType(stopIndex).is(value);
  }

  /* METHODS THAT DELEGATE TO THE SCHEDULED TIMETABLE */

  // TODO: These should probably be deprecated. That would require grabbing the scheduled timetable,
  // and would avoid mistakes where real-time updates are accidentally not taken into account.

  public boolean stopPatternIsEqual(TripPattern other) {
    return stopPattern.equals(other.stopPattern);
  }

  public Trip getTrip(int tripIndex) {
    return scheduledTimetable.getTripTimes(tripIndex).getTrip();
  }

  // TODO OTP2 this method modifies the state, it will be refactored in a subsequent step
  /**
   * Add the given tripTimes to this pattern's scheduled timetable, recording the corresponding trip
   * as one of the scheduled trips on this pattern.
   */
  public void add(TripTimes tt) {
    // Only scheduled trips (added at graph build time, rather than directly to the timetable
    // via updates) are in this list.
    scheduledTimetable.addTripTimes(tt);

    // Check that all trips added to this pattern are on the initially declared route.
    // Identity equality is valid on GTFS entity objects.
    if (this.route != tt.getTrip().getRoute()) {
      LOG.warn(
        "The trip {} is on route {} but its stop pattern is on route {}.",
        tt.getTrip(),
        tt.getTrip().getRoute(),
        route
      );
    }
  }

  // TODO OTP2 this method modifies the state, it will be refactored in a subsequent step
  /**
   * Add the given FrequencyEntry to this pattern's scheduled timetable, recording the corresponding
   * trip as one of the scheduled trips on this pattern.
   * TODO possible improvements: combine freq entries and TripTimes. Do not keep trips list in TripPattern
   * since it is redundant.
   */
  public void add(FrequencyEntry freq) {
    scheduledTimetable.addFrequencyEntry(freq);
    if (this.getRoute() != freq.tripTimes.getTrip().getRoute()) {
      LOG.warn(
        "The trip {} is on a different route than its stop pattern, which is on {}.",
        freq.tripTimes.getTrip(),
        route
      );
    }
  }

  // TODO OTP2 this method modifies the state, it will be refactored in a subsequent step
  /**
   * Remove all trips matching the given predicate.
   *
   * @param removeTrip it the predicate returns true
   */
  public void removeTrips(Predicate<Trip> removeTrip) {
    scheduledTimetable.getTripTimes().removeIf(tt -> removeTrip.test(tt.getTrip()));
  }

  /**
   * Checks that this is TripPattern is based of the provided TripPattern and contains same stops
   * (but not necessarily with same pickup and dropoff values).
   */
  public boolean isModifiedFromTripPatternWithEqualStops(TripPattern other) {
    return (
      isModified() &&
      originalTripPattern.equals(other) &&
      getStopPattern().stopsEqual(other.getStopPattern())
    );
  }

  /**
   * The direction for all the trips in this pattern.
   */
  public Direction getDirection() {
    return scheduledTimetable.getDirection();
  }

  /**
   * This pattern may have multiple Timetable objects, but they should all contain TripTimes for the
   * same trips, in the same order (that of the scheduled Timetable). An exception to this rule may
   * arise if unscheduled trips are added to a Timetable. For that case we need to search for
   * trips/TripIds in the Timetable rather than the enclosing TripPattern.
   */
  public Stream<Trip> scheduledTripsAsStream() {
    var trips = scheduledTimetable.getTripTimes().stream().map(TripTimes::getTrip);
    var freqTrips = scheduledTimetable
      .getFrequencyEntries()
      .stream()
      .map(e -> e.tripTimes.getTrip());
    return Stream.concat(trips, freqTrips).distinct();
  }

  /**
   * This is the "original" timetable holding the scheduled stop times from GTFS, with no realtime
   * updates applied. If realtime stoptime updates are applied, next/previous departure searches
   * will be conducted using a different, updated timetable in a snapshot.
   */
  public Timetable getScheduledTimetable() {
    return scheduledTimetable;
  }

  /**
   * Has the TripPattern been created by a real-time update.
   */
  public boolean isCreatedByRealtimeUpdater() {
    return createdByRealtimeUpdater;
  }

  public TripPattern getOriginalTripPattern() {
    return originalTripPattern;
  }

  public boolean isModified() {
    return originalTripPattern != null;
  }

  /**
   * Returns trip headsign from the scheduled timetables or from the original pattern's scheduled
   * timetables if this pattern is added by realtime and the stop sequence has not changed apart
   * from pickup/dropoff values.
   *
   * @return trip headsign
   */
  public I18NString getTripHeadsign() {
    var tripTimes = scheduledTimetable.getRepresentativeTripTimes();
    return tripTimes == null
      ? getTripHeadsignFromOriginalPattern()
      : getTripHeadSignFromTripTimes(tripTimes);
  }

  public TripPattern clone() {
    try {
      return (TripPattern) super.clone();
    } catch (CloneNotSupportedException e) {
      /* cannot happen */
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the feed id this trip pattern belongs to.
   *
   * @return feed id for this trip pattern
   */
  public String getFeedId() {
    // The feed id is the same as the agency id on the route, this allows us to obtain it from there.
    return route.getId().getFeedId();
  }

  public RoutingTripPattern getRoutingTripPattern() {
    return routingTripPattern;
  }

  @Override
  public String logName() {
    return route.logName();
  }

  /**
   * Does the pattern contain any stops passed in as argument?
   * This method is not optimized for performance so don't use it where that is critical.
   */
  public boolean containsAnyStopId(Collection<FeedScopedId> ids) {
    return ids
      .stream()
      .anyMatch(id ->
        stopPattern
          .getStops()
          .stream()
          .map(StopLocation::getId)
          .collect(Collectors.toUnmodifiableSet())
          .contains(id)
      );
  }

  private static Coordinate coordinate(StopLocation s) {
    return s.getCoordinate().asJtsCoordinate();
  }

  @Override
  public boolean sameAs(@Nonnull TripPattern other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(this.route, other.route) &&
      Objects.equals(this.mode, other.mode) &&
      Objects.equals(this.netexSubMode, other.netexSubMode) &&
      Objects.equals(this.containsMultipleModes, other.containsMultipleModes) &&
      Objects.equals(this.name, other.name) &&
      Objects.equals(this.stopPattern, other.stopPattern) &&
      Objects.equals(this.scheduledTimetable, other.scheduledTimetable)
    );
  }

  @Override
  public TripPatternBuilder copy() {
    return new TripPatternBuilder(this);
  }

  /**
   * Checks if the stops in this trip pattern are the same as in the original pattern (if this trip
   * is added through a realtime update. The pickup and dropoff values don't have to be the same.
   */
  private boolean containsSameStopsAsOriginalPattern() {
    return isModified() && getStops().equals(originalTripPattern.getStops());
  }

  /**
   * Helper method for getting the trip headsign from the {@link TripTimes}.
   */
  private I18NString getTripHeadSignFromTripTimes(TripTimes tripTimes) {
    return tripTimes != null ? tripTimes.getTripHeadsign() : null;
  }

  /**
   * Returns trip headsign from the original pattern if one exists.
   */
  private I18NString getTripHeadsignFromOriginalPattern() {
    if (containsSameStopsAsOriginalPattern()) {
      var tripTimes = originalTripPattern.getScheduledTimetable().getRepresentativeTripTimes();
      return getTripHeadSignFromTripTimes(tripTimes);
    }
    return null;
  }
}
