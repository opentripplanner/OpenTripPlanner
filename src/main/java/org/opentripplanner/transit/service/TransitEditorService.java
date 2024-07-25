package org.opentripplanner.transit.service;

import java.time.LocalDate;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

/**
 * Entry point for requests (both read-only and read-write) towards the transit API.
 */
public interface TransitEditorService extends TransitService {
  void addAgency(Agency agency);

  void addFeedInfo(FeedInfo info);

  void addPatternForTrip(Trip trip, TripPattern pattern);

  void addPatternsForRoute(Route route, TripPattern pattern);

  void addRoutes(Route route);

  void addTransitMode(TransitMode mode);

  void addTripForId(FeedScopedId tripId, Trip trip);

  void addTripOnServiceDateById(FeedScopedId id, TripOnServiceDate tripOnServiceDate);

  void addTripOnServiceDateForTripAndDay(
    TripIdAndServiceDate tripIdAndServiceDate,
    TripOnServiceDate tripOnServiceDate
  );

  FeedScopedId getOrCreateServiceIdForDate(LocalDate serviceDate);

  void setTransitLayer(TransitLayer transitLayer);
}
