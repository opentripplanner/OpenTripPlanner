package org.opentripplanner.transit.repository;

import com.google.common.collect.ImmutableListMultimap;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.transit.model.calendar.TripCalendars;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

/**
 * The scheduled (non-realtime) routes, trips, trip patterns, flex trips and trip calendars of the
 * public transportation network, with the lookups (by id, by trip, by route, by stop, ...) over
 * them.
 * <p>
 * Immutable: created once when the server starts, from the scheduled data populated by the graph
 * build, and then owned by the timetable repository. It is shared,
 * unchanged, by every timetable snapshot. Real-time calendar changes are applied to the timetable
 * repository's own calendars, never to these.
 */
public final class ScheduledTimetableData {

  private final Map<FeedScopedId, TripPattern> tripPatternForId;
  private final Map<FeedScopedId, TripOnServiceDate> tripOnServiceDateById;
  private final ImmutableListMultimap<FeedScopedId, TripOnServiceDate> replacedByTripOnServiceDates;
  private final Map<FeedScopedId, FlexTrip<?, ?>> flexTripForId;
  private final TripCalendars tripCalendars;
  private final ScheduledTimetableIndex index;

  private ScheduledTimetableData(
    Map<FeedScopedId, TripPattern> tripPatternForId,
    Map<FeedScopedId, TripOnServiceDate> tripOnServiceDateById,
    Map<FeedScopedId, FlexTrip<?, ?>> flexTripForId,
    TripCalendars tripCalendars
  ) {
    this.tripPatternForId = copyOf(tripPatternForId);
    this.tripOnServiceDateById = copyOf(tripOnServiceDateById);
    this.replacedByTripOnServiceDates = replacedBy(this.tripOnServiceDateById.values());
    this.flexTripForId = copyOf(flexTripForId);
    this.tripCalendars = tripCalendars.initializeServiceCodesRunningForDate();
    this.index = new ScheduledTimetableIndex(this, this.tripCalendars);
  }

  /**
   * Create the immutable scheduled data. The given maps are copied, so later changes to them are
   * not reflected.
   *
   * @param tripPatternForId the scheduled trip patterns, by id
   * @param tripOnServiceDateById the scheduled trips on service date, by id
   * @param flexTripForId the flex trips, by id
   * @param tripCalendars the scheduled trip calendars; the service codes running on each date are
   *                      computed here
   */
  public static ScheduledTimetableData of(
    Map<FeedScopedId, TripPattern> tripPatternForId,
    Map<FeedScopedId, TripOnServiceDate> tripOnServiceDateById,
    Map<FeedScopedId, FlexTrip<?, ?>> flexTripForId,
    TripCalendars tripCalendars
  ) {
    return new ScheduledTimetableData(
      tripPatternForId,
      tripOnServiceDateById,
      flexTripForId,
      tripCalendars
    );
  }

  /** For each trip on service date id, the trips on service date replacing it. */
  private static ImmutableListMultimap<FeedScopedId, TripOnServiceDate> replacedBy(
    Collection<TripOnServiceDate> tripsOnServiceDate
  ) {
    var builder = ImmutableListMultimap.<FeedScopedId, TripOnServiceDate>builder();
    for (var tripOnServiceDate : tripsOnServiceDate) {
      for (var replacementFor : tripOnServiceDate.getReplacementFor()) {
        builder.put(replacementFor.getId(), tripOnServiceDate);
      }
    }
    return builder.build();
  }

  /** An unmodifiable copy that keeps the iteration order of {@code source}. */
  private static <K, V> Map<K, V> copyOf(Map<K, V> source) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(source));
  }

  /** Scheduled data without any trips or calendars. Only for tests. */
  public static ScheduledTimetableData empty() {
    return of(Map.of(), Map.of(), Map.of(), TripCalendars.empty());
  }

  @Nullable
  public TripPattern getTripPatternForId(FeedScopedId id) {
    return tripPatternForId.get(id);
  }

  public Collection<TripPattern> getAllTripPatterns() {
    return tripPatternForId.values();
  }

  @Nullable
  public TripOnServiceDate getTripOnServiceDateById(FeedScopedId id) {
    return tripOnServiceDateById.get(id);
  }

  public Collection<TripOnServiceDate> getAllTripsOnServiceDate() {
    return tripOnServiceDateById.values();
  }

  public List<TripOnServiceDate> getReplacedByTripOnServiceDate(FeedScopedId id) {
    return replacedByTripOnServiceDates.get(id);
  }

  public Collection<FlexTrip<?, ?>> getAllFlexTrips() {
    return flexTripForId.values();
  }

  /** The scheduled trip calendars, not including any real-time changes. */
  public TripCalendars getTripCalendars() {
    return tripCalendars;
  }

  @Nullable
  public Route getRouteForId(FeedScopedId id) {
    return index.getRouteForId(id);
  }

  public Collection<Route> getAllRoutes() {
    return index.getAllRoutes();
  }

  @Nullable
  public Trip getTripForId(FeedScopedId id) {
    return index.getTripForId(id);
  }

  public Collection<Trip> getAllTrips() {
    return index.getAllTrips();
  }

  public boolean containsTrip(FeedScopedId id) {
    return index.containsTrip(id);
  }

  @Nullable
  public TripPattern getPatternForTrip(Trip trip) {
    return index.getPatternForTrip(trip);
  }

  public Collection<TripPattern> getPatternsForRoute(Route route) {
    return index.getPatternsForRoute(route);
  }

  public Collection<TripPattern> getPatternsForStop(StopLocation stop) {
    return index.getPatternsForStop(stop);
  }

  /** The routes of the scheduled (non-flex) trip patterns visiting the given stop. */
  public Set<Route> getRoutesForStop(StopLocation stop) {
    return index.getRoutesForStop(stop);
  }

  @Nullable
  public TripOnServiceDate getTripOnServiceDateForTripAndDay(
    TripIdAndServiceDate tripIdAndServiceDate
  ) {
    return index.getTripOnServiceDateForTripAndDay(tripIdAndServiceDate);
  }

  public Collection<GroupOfRoutes> getAllGroupOfRoutes() {
    return index.getAllGroupOfRoutes();
  }

  public Collection<Route> getRoutesForGroupOfRoutes(GroupOfRoutes groupOfRoutes) {
    return index.getRoutesForGroupOfRoutes(groupOfRoutes);
  }

  @Nullable
  public GroupOfRoutes getGroupOfRoutesForId(FeedScopedId id) {
    return index.getGroupOfRoutesForId(id);
  }

  /**
   * Checks if the last scheduled service date for the stop is on or after the given date. This
   * does not include real-time updates.
   */
  public boolean hasScheduledServicesAfter(LocalDate date, StopLocation stop) {
    return index.hasScheduledServicesAfter(date, stop);
  }

  /** The flex index, or {@code null} if flex routing is disabled. */
  @Nullable
  public FlexIndex getFlexIndex() {
    return index.getFlexIndex();
  }
}
