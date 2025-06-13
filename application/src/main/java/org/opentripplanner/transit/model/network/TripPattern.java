package org.opentripplanner.transit.model.network;

import static java.util.Objects.requireNonNull;
import static org.opentripplanner.utils.lang.ObjectUtils.requireNotInitialized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;

// TODO OTP2 instances of this class are still mutable after construction with a builder, this will be refactored in a subsequent step
/**
 * Represents a group of trips on a route, with the same direction id that all call at the same
 * sequence of stops. For each stop, there is a list of departure times, running times, arrival
 * times, dwell times, and wheelchair accessibility information (one of each of these per trip per
 * stop). Trips are assumed to be non-overtaking, so that an earlier trip never arrives after a
 * later trip.
 * <p>
 * The key of the TripPattern includes the Route, StopPattern, TransitMode, and SubMode. All trips
 * grouped under a TripPattern should have the same values for these characteristics (with possible
 * exceptions for TransitMode and SubMode).
 * TODO RT_AB: We need to clarify exactly which characteristics are identical across the trips.
 *   Grouping into patterns serves more than one purpose: it conserves memory by not replicating
 *   details shared across all trips in the TripPattern; it reflects business practices outside
 *   routing; it is essential to optimizations in routing algorithms like Raptor. We may be
 *   conflating a domain model grouping with an internal routing grouping.
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

  private final Route route;

  /**
   * This field should not be accessed outside this class. All access to the StopPattern is
   * performed through method  delegation, like the {@link #numberOfStops()} and
   * {@link #canBoard(int)} methods.
   */
  private final StopPattern stopPattern;

  /**
   * TripPatterns hold a reference to a Timetable (i.e. TripTimes for all Trips in the pattern) for
   * only scheduled trips from the GTFS or NeTEx data. If any trips were later updated in real time,
   * there will be another Timetable holding those updates and reading through to the scheduled one.
   * That other realtime Timetable is retrieved from a TimetableSnapshot (see end of Javadoc on
   * TimetableSnapshot for more details).
   * TODO RT_AB: The above system should be changed to integrate realtime and scheduled data more
   *   closely. The Timetable may become obsolete or change significantly when they are integrated.
   */
  private final Timetable scheduledTimetable;

  // This TransitMode is arguably a redundant replication/memoization of information on the Route.
  // It appears that in the TripPatternBuilder it is only ever set from a Trip which is itself set
  // from a Route. This does not just read through to Route because in Netex trips may override
  // the mode of their route. But we need to establish with more clarity whether our internal model
  // TripPatterns allow trips of mixed modes, or rather if a single mode is part of their unique key.
  private final TransitMode mode;

  private final SubMode netexSubMode;
  private final boolean containsMultipleModes;
  private String name;

  /**
   * Geometries of each inter-stop segment of the tripPattern.
   * Not used in routing, only for API listing.
   * TODO: Encapsulate the byte arrays in a class.
   */
  private final byte[][] hopGeometries;

  /**
   * The original TripPattern this replaces at least for one modified trip.
   *
   * Currently this seems to only be set (via TripPatternBuilder) from TripPatternCache and
   * SiriTripPatternCache.
   *
   * FIXME RT_AB: Revise comments to make it clear how this is used (it is only used rarely).
   */
  private final TripPattern originalTripPattern;

  /**
   * When a trip is added or rerouted by a realtime update, this may give rise to a new TripPattern
   * that did not exist in the scheduled data. For such TripPatterns this field will be true. If on
   * the other hand this TripPattern instance was created from the schedule data, this field will be
   * false.
   */
  private final boolean createdByRealtimeUpdater;

  private final RoutingTripPattern routingTripPattern;

  TripPattern(TripPatternBuilder builder) {
    super(builder.getId());
    this.name = builder.getName();
    this.route = builder.getRoute();
    this.stopPattern = requireNonNull(builder.getStopPattern());
    this.createdByRealtimeUpdater = builder.isCreatedByRealtimeUpdate();
    this.mode = requireNonNull(builder.getMode());
    this.netexSubMode = requireNonNull(builder.getNetexSubmode());
    this.containsMultipleModes = builder.getContainsMultipleModes();

    if (builder.getScheduledTimetable() != null) {
      if (builder.getScheduledTimetableBuilder() != null) {
        throw new IllegalArgumentException(
          "Cannot provide both scheduled timetable and scheduled timetable builder"
        );
      }
      this.scheduledTimetable = builder.getScheduledTimetable();
    } else {
      this.scheduledTimetable = builder
        .getScheduledTimetableBuilder()
        .withTripPattern(this)
        .build();
    }

    this.originalTripPattern = builder.getOriginalTripPattern();

    this.hopGeometries = builder.hopGeometries();
    this.routingTripPattern = new RoutingTripPattern(this, builder);
  }

  public static TripPatternBuilder of(FeedScopedId id) {
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
      return GeometryUtils.getGeometryFactory()
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
      ? originalTripPattern.stopPattern.copyOf(stopPattern)
      : stopPattern.copyOf();
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
   * Return the direction for all the trips in this pattern.
   * By construction, all trips in a pattern have the same direction:
   * - trips derived from NeTEx data belong to a ServiceJourney that belongs to a JourneyPattern
   * that belongs to a NeTEx Route that specifies a single direction.
   * - trips derived from GTFS data are grouped by direction in a trip pattern, during graph build.
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
  public boolean sameAs(TripPattern other) {
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
