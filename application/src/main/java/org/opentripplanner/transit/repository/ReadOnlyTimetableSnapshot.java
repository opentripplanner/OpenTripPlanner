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

public interface ReadOnlyTimetableSnapshot {
  Timetable resolve(TripPattern pattern, @Nullable LocalDate serviceDate);

  @Nullable
  TripPattern getNewTripPatternForModifiedTrip(FeedScopedId tripId, LocalDate serviceDate);

  List<TripOnServiceDate> listCanceledTrips();

  boolean hasNewTripPatternsForModifiedTrips();

  @Nullable
  Route getRealtimeAddedRoute(FeedScopedId id);

  Collection<Route> listRealTimeAddedRoutes();

  @Nullable
  Trip getRealTimeAddedTrip(FeedScopedId id);

  Collection<Trip> listRealTimeAddedTrips();

  @Nullable
  TripPattern getRealTimeAddedPatternForTrip(Trip trip);

  Collection<TripPattern> getRealTimeAddedPatternForRoute(Route route);

  @Nullable
  TripOnServiceDate getRealTimeAddedTripOnServiceDateById(FeedScopedId id);

  @Nullable
  TripOnServiceDate getRealTimeAddedTripOnServiceDateForTripAndDay(
    TripIdAndServiceDate tripIdAndServiceDate
  );

  Collection<? extends TripOnServiceDate> listRealTimeAddedTripOnServiceDate();

  Collection<TripOnServiceDate> getRealTimeReplacedByTripOnServiceDate(FeedScopedId id);

  Collection<TripPattern> getPatternsForStop(StopLocation stop);

  RaptorTransitData getRealtimeRaptorTransitData();

  /**
   * Does this snapshot contain any realtime data or is it completely empty?
   */
  boolean isEmpty();
}
