package org.opentripplanner.transit.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.transit.model.calendar.TripCalendars;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

/**
 * An immutable, read-only snapshot of the realtime-updated timetables and trip calendar. A new
 * snapshot is published each time a transaction that touched the {@link TimetableRepository}
 * commits. Request threads read a snapshot resolved at the start of the request, through the
 * request-scoped {@link org.opentripplanner.transit.service.TransitService}.
 * <p>
 * The trip calendar is included here (rather than being its own repository) because every
 * consumer that needs realtime timetable data also needs the calendar — new service ids created
 * for real-time-added trips must be visible in the same transaction as the trips that use them.
 */
public interface TimetableRepositorySnapshot {
  /**
   * Return the updated timetable for the specified pattern if one is available in this snapshot,
   * or the originally scheduled timetable if there are no updates in this snapshot.
   */
  Timetable resolve(TripPattern pattern, @Nullable LocalDate serviceDate);

  /**
   * Return the current trip pattern given a trip id and a service date, if it has been changed
   * from the scheduled pattern by an update with a different stop pattern.
   *
   * @return trip pattern created by the updater; null if the trip is on its original trip pattern
   */
  @Nullable
  TripPattern getNewTripPatternForModifiedTrip(FeedScopedId tripId, LocalDate serviceDate);

  /**
   * List trips which have been canceled by realtime updates.
   */
  List<TripOnServiceDate> listCanceledTrips();

  /**
   * Return true if any trip has been assigned to a new trip pattern by a realtime update.
   */
  boolean hasNewTripPatternsForModifiedTrips();

  /**
   * The scheduled (non-realtime) timetable data. Shared, unchanged, by every snapshot.
   */
  ScheduledTimetableData getScheduledTimetableData();

  /**
   * Return a route for a given id, including routes created by real-time updates, falling back to
   * the scheduled route if there is no real-time-added route with this id.
   */
  @Nullable
  Route getRoute(FeedScopedId id);

  /**
   * Return all routes, including those created by real-time updates.
   */
  Collection<Route> listRoutes();

  /**
   * Return the trip for the given id, including trips created by real-time updates, falling back
   * to the scheduled trip if there is no real-time-added trip with this id.
   */
  @Nullable
  Trip getTrip(FeedScopedId id);

  /**
   * Return the trip for the given id, not including trips created by real-time updates.
   */
  @Nullable
  Trip getScheduledTrip(FeedScopedId id);

  /**
   * Return all trips, including those created by real-time updates.
   */
  Collection<Trip> listTrips();

  /**
   * Return true if a trip with the given id exists, either in the scheduled data or among the
   * trips created by real-time updates.
   */
  boolean containsTrip(FeedScopedId id);

  /**
   * Return the scheduled trip pattern for a given trip, or, if the trip was added by a real-time
   * update (extra journey), the pattern it was created with.
   */
  TripPattern findPattern(Trip trip);

  /**
   * Return the trip pattern for a given trip on a service date. The real-time updated version is
   * returned if it exists, otherwise the scheduled (or real-time-added) trip pattern is returned.
   */
  TripPattern findPattern(Trip trip, @Nullable LocalDate serviceDate);

  /**
   * Return all the trip patterns used in the given route, including those added by real-time
   * updates.
   */
  Collection<TripPattern> findPatterns(Route route);

  /**
   * Return the trip-on-service-date for a given id, including those created by real-time updates,
   * falling back to the scheduled trip-on-service-date if there is none real-time-added with this
   * id.
   */
  @Nullable
  TripOnServiceDate getTripOnServiceDate(FeedScopedId id);

  /**
   * Return the trip-on-service-date for a given trip and service date, including those created by
   * real-time updates, falling back to the scheduled trip-on-service-date if there is none
   * real-time-added for this trip and date.
   */
  @Nullable
  TripOnServiceDate getTripOnServiceDate(TripIdAndServiceDate tripIdAndServiceDate);

  /**
   * Return all trips-on-service-date, including those created by real-time updates.
   */
  Collection<TripOnServiceDate> listTripsOnServiceDate();

  /**
   * Return the trips-on-service-date that replace the given trip-on-service-date according to
   * realtime updates.
   */
  Collection<TripOnServiceDate> getRealTimeReplacedByTripOnServiceDate(FeedScopedId id);

  /**
   * Return the patterns created by realtime updates which visit the given stop.
   */
  Collection<TripPattern> getPatternsForStop(StopLocation stop);

  /**
   * Return the raptor transit data that includes the realtime updates of this snapshot. This is
   * the transit data used for routing with this snapshot.
   */
  RaptorTransitData getRealtimeRaptorTransitData();

  /**
   * Does this snapshot contain any realtime data or is it completely empty?
   */
  boolean isEmpty();

  /**
   * Return the trip calendar for this snapshot, including any service ids created for
   * real-time-added trips.
   */
  TripCalendars getTripCalendars();
}
