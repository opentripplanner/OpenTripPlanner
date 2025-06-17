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
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.api.request.FindRegularStopsByBoundingBoxRequest;
import org.opentripplanner.transit.api.request.FindRoutesRequest;
import org.opentripplanner.transit.api.request.FindStopLocationsRequest;
import org.opentripplanner.transit.api.request.TripOnServiceDateRequest;
import org.opentripplanner.transit.api.request.TripRequest;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequest;
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
 * TODO RT_AB: this interface seems to provide direct access to RaptorTransitData but not TimetableRepository.
 *   Is this intentional, because RaptorTransitData is meant to be read-only and TimetableRepository is not?
 *   Should this be renamed TransitDataService since it seems to provide access to the data but
 *   not to transit routing functionality (which is provided by the RoutingService)?
 *   The DefaultTransitService implementation has a TimetableRepository instance and many of its methods
 *   read through to that TimetableRepository instance. But that field itself is not exposed, while the
 *   RaptorTransitData is here. It seems like exposing the raw RaptorTransitData is still a risk since it's
 *   copy-on-write and shares a lot of objects with any other RaptorTransitData instances.
 */
public interface TransitService {
  /**
   * @return empty if the trip doesn't exist in the timetable (e.g. real-time added)
   */
  Optional<List<TripTimeOnDate>> getScheduledTripTimes(Trip trip);

  /**
   * @return empty if the trip doesn't run on the date specified
   */
  Optional<List<TripTimeOnDate>> getTripTimeOnDates(Trip trip, LocalDate serviceDate);

  Collection<String> listFeedIds();

  Collection<Agency> listAgencies();
  Optional<Agency> findAgency(FeedScopedId id);

  FeedInfo getFeedInfo(String feedId);

  Collection<Notice> findNotices(AbstractTransitEntity<?, ?> entity);

  /**
   * Return a trip pattern by id, not including patterns created by real-time updates.
   */
  TripPattern getTripPattern(FeedScopedId id);

  /**
   * Return all scheduled trip patterns, not including real-time created trip patterns.
   * TODO: verify this is the intended behavior and possibly change the method name to
   *       getAllScheduledTripPatterns
   */
  Collection<TripPattern> listTripPatterns();

  Station getStation(FeedScopedId id);

  MultiModalStation getMultiModalStation(FeedScopedId id);

  Collection<Station> listStations();

  Integer getServiceCode(FeedScopedId id);

  TIntSet getServiceCodesRunningForDate(LocalDate date);

  Agency getAgency(FeedScopedId id);

  /**
   * Return a route for a given id, including routes created by real-time updates.
   *
   */
  Route getRoute(FeedScopedId id);

  /**
   * Return all routes for a given set of ids, including routes created by real-time updates.
   */
  Collection<Route> getRoutes(Collection<FeedScopedId> ids);

  /**
   * Return the routes using the given stop, not including real-time updates.
   */
  Set<Route> findRoutes(StopLocation stop);

  /**
   * Return all the scheduled trip patterns for a specific stop
   * (not taking into account real-time updates).
   */
  Collection<TripPattern> findPatterns(StopLocation stop);

  /**
   * Returns all the patterns for a specific stop. If includeRealtimeUpdates is set, new patterns
   * added by realtime updates are added to the collection.
   */
  Collection<TripPattern> findPatterns(StopLocation stop, boolean includeRealtimeUpdates);

  Collection<Operator> listOperators();

  Operator getOperator(FeedScopedId id);

  @Nullable
  RegularStop getRegularStop(FeedScopedId id);

  Collection<StopLocation> listStopLocations();

  Collection<GroupStop> listGroupStops();

  StopLocation getStopLocation(FeedScopedId id);

  /**
   * Return all stops associated with the given id. If a Station, a MultiModalStation, or a
   * GroupOfStations matches the id, then all child stops are returned. If the id matches a regular
   * stop, area stop or stop group, then a list with one item is returned.
   * An empty list is if nothing is found.
   */
  Collection<StopLocation> findStopOrChildStops(FeedScopedId id);

  Collection<StopLocationsGroup> listStopLocationGroups();

  StopLocationsGroup getStopLocationsGroup(FeedScopedId id);

  /**
   * Return the trip for the given id, including trips created in real time.
   */
  @Nullable
  Trip getTrip(FeedScopedId id);

  /**
   * Return all trips, including those created by real-time updates.
   */
  Collection<Trip> listTrips();

  /**
   * List all canceled trips.
   */
  List<TripOnServiceDate> listCanceledTrips();

  /**
   * Return all routes, including those created by real-time updates.
   */
  Collection<Route> listRoutes();

  /**
   * Return the scheduled trip pattern for a given trip.
   * If the trip is an added trip (extra journey), return the initial trip pattern for this trip.
   */
  TripPattern findPattern(Trip trip);

  /**
   * Return the trip pattern for a given trip on a service date. The real-time updated version
   * is returned if it exists, otherwise the scheduled trip pattern is returned.
   */
  TripPattern findPattern(Trip trip, LocalDate serviceDate);

  /**
   * Return all the trip patterns used in the given route, including those added by real-time updates
   */
  Collection<TripPattern> findPatterns(Route route);

  MultiModalStation findMultiModalStation(Station station);

  /**
   * Fetch upcoming vehicle departures from a stop. It goes though all patterns passing the stop for
   * the previous, current and next service date. It uses a priority queue to keep track of the next
   * departures. The queue is shared between all dates, as services from the previous service date
   * can visit the stop later than the current service date's services. This happens eg. with
   * sleeper trains.
   * <p>
   * TODO: Add frequency based trips
   *
   * @param stop                  Stop object to perform the search for
   * @param startTime             Start time for the search.
   * @param timeRange             Searches forward for timeRange from startTime
   * @param numberOfDepartures    Number of departures to fetch per pattern
   * @param arrivalDeparture      Filter by arrivals, departures, or both
   * @param includeCancelledTrips If true, cancelled trips will also be included in result.
   */
  List<StopTimesInPattern> findStopTimesInPattern(
    StopLocation stop,
    Instant startTime,
    Duration timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancelledTrips
  );

  /**
   * Get a list of all trips that pass through a stop during a single ServiceDate. Useful when
   * creating complete stop timetables for a single day.
   *
   * @param stop        Stop object to perform the search for
   * @param serviceDate Return all departures for the specified date
   */
  List<StopTimesInPattern> findStopTimesInPattern(
    StopLocation stop,
    LocalDate serviceDate,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancellations
  );

  /**
   * Fetch upcoming vehicle departures from a stop for a specific pattern, passing the stop for the
   * previous, current and next service date. It uses a priority queue to keep track of the next
   * departures. The queue is shared between all dates, as services from the previous service date
   * can visit the stop later than the current service date's services.
   * <p>
   * TODO: Add frequency based trips
   *
   * @param stop                 Stop object to perform the search for
   * @param pattern              Pattern object to perform the search for
   * @param startTime            Start time for the search.
   * @param timeRange            Searches forward for timeRange from startTime
   * @param numberOfDepartures   Number of departures to fetch per pattern
   * @param arrivalDeparture     Filter by arrivals, departures, or both
   * @param includeCancellations If the result should include those trip times where either the entire
   *                             trip or the stop at the given stop location has been cancelled.
   *                             Deleted trips are never returned no matter the value of this parameter.
   */
  List<TripTimeOnDate> findTripTimesOnDate(
    StopLocation stop,
    TripPattern pattern,
    Instant startTime,
    Duration timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancellations
  );

  /**
   * Fetch upcoming vehicle departures from a stop for a specific pattern, passing the stop for the
   * previous, current and next service date. It uses a priority queue to keep track of the next
   * departures. The queue is shared between all dates, as services from the previous service date
   * can visit the stop later than the current service date's services.
   * <p>
   * This method is similar to {@link TransitService#findTripTimesOnDate(StopLocation, TripPattern, Instant, Duration, int, ArrivalDeparture, boolean)}
   * in that it uses a filter request which allows you to include and exclude routes, agencies and modes.
   */
  List<TripTimeOnDate> findTripTimesOnDate(TripTimeOnDateRequest request);

  Collection<GroupOfRoutes> listGroupsOfRoutes();

  Collection<Route> findRoutes(GroupOfRoutes groupOfRoutes);

  @Nullable
  GroupOfRoutes getGroupOfRoutes(FeedScopedId id);

  /**
   * Return the timetable for a given trip pattern and date, taking into account real-time updates.
   * If no real-times update are applied, fall back to scheduled data.
   */
  Timetable findTimetable(TripPattern tripPattern, LocalDate serviceDate);

  /**
   * Return the real-time added pattern for a given tripId and a given service date.
   * Return null if the trip does not exist or if the trip has no real-time added pattern for
   * this date (that is: it is still using its scheduled trip pattern for this date).
   */
  @Nullable
  TripPattern findNewTripPatternForModifiedTrip(FeedScopedId tripId, LocalDate serviceDate);

  /**
   * Return true if at least one trip pattern has been modified by a real-time update.
   */
  boolean hasNewTripPatternsForModifiedTrips();

  TripOnServiceDate getTripOnServiceDate(TripIdAndServiceDate tripIdAndServiceDate);

  /**
   * Return the TripOnServiceDate for a given id, including real-time updates.
   */
  TripOnServiceDate getTripOnServiceDate(FeedScopedId id);

  Collection<TripOnServiceDate> listTripsOnServiceDate();

  Set<TransitMode> listTransitModes();

  Collection<PathTransfer> findPathTransfers(StopLocation stop);

  RaptorTransitData getRaptorTransitData();

  RaptorTransitData getRealtimeRaptorTransitData();

  CalendarService getCalendarService();

  ZoneId getTimeZone();

  TransitAlertService getTransitAlertService();

  FlexIndex getFlexIndex();

  ZonedDateTime getTransitServiceEnds();

  ZonedDateTime getTransitServiceStarts();

  TransferService getTransferService();

  boolean transitFeedCovers(Instant dateTime);

  Collection<RegularStop> findRegularStopsByBoundingBox(Envelope envelope);

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
  List<TransitMode> findTransitModes(StopLocationsGroup station);

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
  List<TransitMode> findTransitModes(StopLocation stop);

  Deduplicator getDeduplicator();

  Set<LocalDate> listServiceDates();

  Map<LocalDate, TIntSet> getServiceCodesRunningForDate();

  /**
   * Returns a list of {@link TripOnServiceDate}s that match the filtering defined in the request.
   */
  List<TripOnServiceDate> findTripsOnServiceDate(TripOnServiceDateRequest request);

  /**
   * Returns a list of {@link Trip}s that match the filtering defined in the request.
   *
   */
  List<Trip> getTrips(TripRequest request);

  /**
   * Checks if a trip with the given ID exists in the model.
   *
   * @param id the {@link FeedScopedId} of the trip to check
   * @return true if the trip exists, false otherwise
   */
  boolean containsTrip(FeedScopedId id);

  /**
   * @see TimetableRepository#findStopByScheduledStopPoint(FeedScopedId)
   */
  Optional<RegularStop> findStopByScheduledStopPoint(FeedScopedId scheduledStopPoint);

  /**
   * Returns a list of {@link RegularStop}s that lay within a bounding box and match the other criteria
   * in the request object.
   */
  Collection<RegularStop> findRegularStopsByBoundingBox(
    FindRegularStopsByBoundingBoxRequest request
  );

  /**
   * Returns a list of {@link Route}s that match the filtering defined in the request.
   */
  Collection<Route> findRoutes(FindRoutesRequest request);

  /**
   * Returns a list of {@link StopLocation}s that match the filtering defined in the request.
   */
  Collection<StopLocation> findStopLocations(FindStopLocationsRequest request);
}
