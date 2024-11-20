package org.opentripplanner.transit.service;

import gnu.trove.set.TIntSet;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.transit.api.request.TripOnServiceDateRequest;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopLocationsGroup;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.updater.GraphUpdaterStatus;

/**
 * TransitService is a read-only interface for retrieving public transport data. It provides a
 * frozen view of all these elements at a point in time, which is not affected by incoming realtime
 * data, allowing results to remain stable over the course of a request. This can be used for
 * fetching tables of specific information like the routes passing through a particular stop, or for
 * gaining access to the entirety of the data to perform routing.
 * <p>
 * TODO RT_AB: this interface seems to provide direct access to TransitLayer but not TimetableRepository.
 *   Is this intentional, because TransitLayer is meant to be read-only and TimetableRepository is not?
 *   Should this be renamed TransitDataService since it seems to provide access to the data but
 *   not to transit routing functionality (which is provided by the RoutingService)?
 *   The DefaultTransitService implementation has a TimetableRepository instance and many of its methods
 *   read through to that TimetableRepository instance. But that field itself is not exposed, while the
 *   TransitLayer is here. It seems like exposing the raw TransitLayer is still a risk since it's
 *   copy-on-write and shares a lot of objects with any other TransitLayer instances.
 */
public interface TransitService {
  Collection<String> getFeedIds();

  Collection<Agency> getAgencies();
  Optional<Agency> findAgencyById(FeedScopedId id);

  FeedInfo getFeedInfo(String feedId);

  Collection<Notice> getNoticesByEntity(AbstractTransitEntity<?, ?> entity);

  /**
   * Return a trip pattern by id, not including patterns created by real-time updates.
   */
  TripPattern getTripPatternForId(FeedScopedId id);

  /**
   * Return all scheduled trip patterns, not including real-time created trip patterns.
   * TODO: verify this is the intended behavior and possibly change the method name to
   *       getAllScheduledTripPatterns
   */
  Collection<TripPattern> getAllTripPatterns();

  Collection<Notice> getNotices();

  Station getStationById(FeedScopedId id);

  MultiModalStation getMultiModalStation(FeedScopedId id);

  Collection<Station> getStations();

  Integer getServiceCodeForId(FeedScopedId id);

  TIntSet getServiceCodesRunningForDate(LocalDate date);

  Agency getAgencyForId(FeedScopedId id);

  /**
   * Return a route for a given id, including routes created by real-time updates.
   *
   */
  Route getRouteForId(FeedScopedId id);

  /**
   * Return the routes using the given stop, not including real-time updates.
   */
  Set<Route> getRoutesForStop(StopLocation stop);

  /**
   * Return all the scheduled trip patterns for a specific stop
   * (not taking into account real-time updates).
   */
  Collection<TripPattern> getPatternsForStop(StopLocation stop);

  /**
   * Returns all the patterns for a specific stop. If includeRealtimeUpdates is set, new patterns
   * added by realtime updates are added to the collection.
   */
  Collection<TripPattern> getPatternsForStop(StopLocation stop, boolean includeRealtimeUpdates);

  Collection<Trip> getTripsForStop(StopLocation stop);

  Collection<Operator> getAllOperators();

  Operator getOperatorForId(FeedScopedId id);

  RegularStop getRegularStop(FeedScopedId id);

  Collection<StopLocation> listStopLocations();

  Collection<RegularStop> listRegularStops();

  Collection<GroupStop> listGroupStops();

  StopLocation getStopLocation(FeedScopedId parseId);

  /**
   * Return all stops associated with the given id. If a Station, a MultiModalStation, or a
   * GroupOfStations matches the id, then all child stops are returned. If the id matches a regular
   * stop, area stop or stop group, then a list with one item is returned.
   * An empty list is if nothing is found.
   */
  Collection<StopLocation> getStopOrChildStops(FeedScopedId id);

  Collection<StopLocationsGroup> listStopLocationGroups();

  StopLocationsGroup getStopLocationsGroup(FeedScopedId id);

  AreaStop getAreaStop(FeedScopedId id);

  /**
   * Return the trip for the given id, including trips created in real time.
   */
  @Nullable
  Trip getTripForId(FeedScopedId id);

  /**
   * Return the trip for the given id, not including trips created in real time.
   */
  @Nullable
  Trip getScheduledTripForId(FeedScopedId id);

  /**
   * Return all trips, including those created by real-time updates.
   */
  Collection<Trip> getAllTrips();

  /**
   * Return all routes, including those created by real-time updates.
   */
  Collection<Route> getAllRoutes();

  /**
   * Return the scheduled trip pattern for a given trip.
   * If the trip is an added trip (extra journey), return the initial trip pattern for this trip.
   */
  TripPattern getPatternForTrip(Trip trip);

  /**
   * Return the trip pattern for a given trip on a service date. The real-time updated version
   * is returned if it exists, otherwise the scheduled trip pattern is returned.
   */
  TripPattern getPatternForTrip(Trip trip, LocalDate serviceDate);

  /**
   * Return all the trip patterns used in the given route, including those added by real-time updates
   */
  Collection<TripPattern> getPatternsForRoute(Route route);

  MultiModalStation getMultiModalStationForStation(Station station);

  List<StopTimesInPattern> stopTimesForStop(
    StopLocation stop,
    Instant startTime,
    Duration timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancelledTrips
  );

  List<StopTimesInPattern> getStopTimesForStop(
    StopLocation stop,
    LocalDate serviceDate,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancellations
  );

  List<TripTimeOnDate> stopTimesForPatternAtStop(
    StopLocation stop,
    TripPattern pattern,
    Instant startTime,
    Duration timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancellations
  );

  Collection<GroupOfRoutes> getGroupsOfRoutes();

  Collection<Route> getRoutesForGroupOfRoutes(GroupOfRoutes groupOfRoutes);

  GroupOfRoutes getGroupOfRoutesForId(FeedScopedId id);

  /**
   * Return the timetable for a given trip pattern and date, taking into account real-time updates.
   * If no real-times update are applied, fall back to scheduled data.
   */
  @Nullable
  Timetable getTimetableForTripPattern(TripPattern tripPattern, LocalDate serviceDate);

  /**
   * Return the real-time added pattern for a given tripId and a given service date.
   * Return null if the trip does not exist or if the trip has no real-time added pattern for
   * this date (that is: it is still using its scheduled trip pattern for this date).
   */
  @Nullable
  TripPattern getNewTripPatternForModifiedTrip(FeedScopedId tripId, LocalDate serviceDate);

  /**
   * Return true if at least one trip pattern has been modified by a real-time update.
   */
  boolean hasNewTripPatternsForModifiedTrips();

  TripOnServiceDate getTripOnServiceDateForTripAndDay(TripIdAndServiceDate tripIdAndServiceDate);

  /**
   * Return the TripOnServiceDate for a given id, including real-time updates.
   */
  TripOnServiceDate getTripOnServiceDateById(FeedScopedId datedServiceJourneyId);

  Collection<TripOnServiceDate> getAllTripOnServiceDates();

  Set<TransitMode> getTransitModes();

  Collection<PathTransfer> getTransfersByStop(StopLocation stop);

  TransitLayer getTransitLayer();

  TransitLayer getRealtimeTransitLayer();

  CalendarService getCalendarService();

  ZoneId getTimeZone();

  TransitAlertService getTransitAlertService();

  FlexIndex getFlexIndex();

  ZonedDateTime getTransitServiceEnds();

  ZonedDateTime getTransitServiceStarts();

  TransferService getTransferService();

  boolean transitFeedCovers(Instant dateTime);

  Collection<RegularStop> findRegularStops(Envelope envelope);

  Collection<AreaStop> findAreaStops(Envelope envelope);

  GraphUpdaterStatus getUpdaterStatus();

  /**
   * For a {@link StopLocationsGroup} get all child stops and get their modes.
   * <p>
   * The mode is either taken from {@link StopLocation#getVehicleType()} (if non-null)
   * or from the list of patterns that use the stop location.
   * <p>
   * The returning stream is ordered by the number of occurrences of the mode in the child stops.
   * So, if more patterns of mode BUS than RAIL visit the group, the result will be [BUS,RAIL].
   */
  List<TransitMode> getModesOfStopLocationsGroup(StopLocationsGroup station);
  /**
   * For a {@link StopLocation} return its modes.
   * <p>
   * The mode is either taken from {@link StopLocation#getVehicleType()} (if non-null)
   * or from the list of patterns that use the stop location.
   * <p>
   * If {@link StopLocation#getVehicleType()} is null the returning stream is ordered by the number
   * of occurrences of the mode in the stop.
   * <p>
   * So, if more patterns of mode BUS than RAIL visit the stop, the result will be [BUS,RAIL].
   */
  List<TransitMode> getModesOfStopLocation(StopLocation stop);

  Deduplicator getDeduplicator();

  Set<LocalDate> getAllServiceCodes();

  Map<LocalDate, TIntSet> getServiceCodesRunningForDate();

  /**
   * Returns a list of TripOnServiceDates that match the filtering defined in the request.
   *
   * @param request - A TripOnServiceDateRequest object with filtering defined.
   * @return - A list of TripOnServiceDates
   */
  List<TripOnServiceDate> getTripOnServiceDates(TripOnServiceDateRequest request);
}
