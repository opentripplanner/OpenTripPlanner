package org.opentripplanner.transit.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

/**
 * An immutable, read-only snapshot of the realtime-updated timetables. A new snapshot is published
 * each time a transaction that touched the {@link TimetableRepository} commits. Request
 * threads read a snapshot resolved at the start of the request, through the request-scoped
 * {@link org.opentripplanner.transit.service.TransitService}.
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
   * Return the route created by a realtime update for the given id, or null if no route was added
   * with this id.
   */
  @Nullable
  Route getRealtimeAddedRoute(FeedScopedId id);

  /**
   * List all routes created by realtime updates, that is routes which are not present in the
   * scheduled data.
   */
  Collection<Route> listRealTimeAddedRoutes();

  /**
   * Return the trip created by a realtime update for the given id, or null if no trip was added
   * with this id.
   */
  @Nullable
  Trip getRealTimeAddedTrip(FeedScopedId id);

  /**
   * List all trips created by realtime updates, that is trips which are not present in the
   * scheduled data.
   */
  Collection<Trip> listRealTimeAddedTrips();

  /**
   * Return the pattern created by a realtime update for the given realtime-added trip, or null if
   * the trip is not realtime-added.
   */
  @Nullable
  TripPattern getRealTimeAddedPatternForTrip(Trip trip);

  /**
   * Return the patterns created by realtime updates for the given route.
   */
  Collection<TripPattern> getRealTimeAddedPatternForRoute(Route route);

  /**
   * Return the trip-on-service-date created by a realtime update for the given id, or null if no
   * trip-on-service-date was added with this id.
   */
  @Nullable
  TripOnServiceDate getRealTimeAddedTripOnServiceDateById(FeedScopedId id);

  /**
   * Return the trip-on-service-date created by a realtime update for the given trip and service
   * date, or null if the trip is not realtime-added on that date.
   */
  @Nullable
  TripOnServiceDate getRealTimeAddedTripOnServiceDateForTripAndDay(
    TripIdAndServiceDate tripIdAndServiceDate
  );

  /**
   * List all trips-on-service-date created by realtime updates.
   */
  Collection<? extends TripOnServiceDate> listRealTimeAddedTripOnServiceDate();

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
}
