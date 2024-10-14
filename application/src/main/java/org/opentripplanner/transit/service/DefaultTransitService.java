package org.opentripplanner.transit.service;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.framework.collection.CollectionsView;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.routing.stoptimes.StopTimesHelper;
import org.opentripplanner.transit.api.request.TripOnServiceDateRequest;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.transit.TripOnServiceDateMatcherFactory;
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
 * Default implementation of the Transit Service and Transit Editor Service.
 * A new instance of this class should be created for each request.
 * This ensures that the same TimetableSnapshot is used for the
 * duration of the request (which may involve several method calls).
 */
public class DefaultTransitService implements TransitEditorService {

  private final TransitModel transitModel;

  private final TransitModelIndex transitModelIndex;

  /**
   * This should only be accessed through the getTimetableSnapshot method.
   */
  private TimetableSnapshot timetableSnapshot;

  @Inject
  public DefaultTransitService(TransitModel transitModel) {
    this.transitModel = transitModel;
    this.transitModelIndex = transitModel.getTransitModelIndex();
  }

  public DefaultTransitService(
    TransitModel transitModel,
    TimetableSnapshot timetableSnapshotBuffer
  ) {
    this(transitModel);
    this.timetableSnapshot = timetableSnapshotBuffer;
  }

  @Override
  public Collection<String> getFeedIds() {
    return this.transitModel.getFeedIds();
  }

  @Override
  public Collection<Agency> getAgencies() {
    OTPRequestTimeoutException.checkForTimeout();
    return this.transitModel.getAgencies();
  }

  @Override
  public Optional<Agency> findAgencyById(FeedScopedId id) {
    return this.transitModel.findAgencyById(id);
  }

  @Override
  public FeedInfo getFeedInfo(String feedId) {
    return this.transitModel.getFeedInfo(feedId);
  }

  @Override
  public void addAgency(Agency agency) {
    this.transitModel.addAgency(agency);
  }

  @Override
  public void addFeedInfo(FeedInfo info) {
    this.transitModel.addFeedInfo(info);
  }

  @Override
  public Collection<Notice> getNoticesByEntity(AbstractTransitEntity<?, ?> entity) {
    return this.transitModel.getNoticesByElement().get(entity);
  }

  @Override
  public TripPattern getTripPatternForId(FeedScopedId id) {
    return this.transitModel.getTripPatternForId(id);
  }

  @Override
  public Collection<TripPattern> getAllTripPatterns() {
    OTPRequestTimeoutException.checkForTimeout();
    return this.transitModel.getAllTripPatterns();
  }

  @Override
  public Collection<Notice> getNotices() {
    OTPRequestTimeoutException.checkForTimeout();
    return this.transitModel.getNoticesByElement().values();
  }

  @Override
  public Station getStationById(FeedScopedId id) {
    return this.transitModel.getStopModel().getStationById(id);
  }

  @Override
  public MultiModalStation getMultiModalStation(FeedScopedId id) {
    return this.transitModel.getStopModel().getMultiModalStation(id);
  }

  @Override
  public Collection<Station> getStations() {
    OTPRequestTimeoutException.checkForTimeout();
    return this.transitModel.getStopModel().listStations();
  }

  @Override
  public Integer getServiceCodeForId(FeedScopedId id) {
    return this.transitModel.getServiceCodes().get(id);
  }

  @Override
  public TIntSet getServiceCodesRunningForDate(LocalDate serviceDate) {
    return transitModelIndex
      .getServiceCodesRunningForDate()
      .getOrDefault(serviceDate, new TIntHashSet());
  }

  @Override
  public AreaStop getAreaStop(FeedScopedId id) {
    return this.transitModel.getStopModel().getAreaStop(id);
  }

  @Override
  public Agency getAgencyForId(FeedScopedId id) {
    return this.transitModelIndex.getAgencyForId(id);
  }

  @Override
  public RegularStop getRegularStop(FeedScopedId id) {
    return this.transitModel.getStopModel().getRegularStop(id);
  }

  @Override
  public Route getRouteForId(FeedScopedId id) {
    TimetableSnapshot currentSnapshot = lazyGetTimeTableSnapShot();
    if (currentSnapshot != null) {
      Route realtimeAddedRoute = currentSnapshot.getRealtimeAddedRoute(id);
      if (realtimeAddedRoute != null) {
        return realtimeAddedRoute;
      }
    }
    return transitModelIndex.getRouteForId(id);
  }

  /**
   * Add a route to the transit model.
   * Used only in unit tests.
   */
  @Override
  public void addRoutes(Route route) {
    this.transitModelIndex.addRoutes(route);
  }

  @Override
  public Set<Route> getRoutesForStop(StopLocation stop) {
    OTPRequestTimeoutException.checkForTimeout();
    return this.transitModelIndex.getRoutesForStop(stop);
  }

  @Override
  public Collection<TripPattern> getPatternsForStop(StopLocation stop) {
    OTPRequestTimeoutException.checkForTimeout();
    return this.transitModelIndex.getPatternsForStop(stop);
  }

  @Override
  public Collection<Trip> getTripsForStop(StopLocation stop) {
    OTPRequestTimeoutException.checkForTimeout();
    return this.transitModelIndex.getTripsForStop(stop);
  }

  @Override
  public Collection<Operator> getAllOperators() {
    OTPRequestTimeoutException.checkForTimeout();
    return this.transitModel.getOperators();
  }

  @Override
  public Operator getOperatorForId(FeedScopedId id) {
    return this.transitModelIndex.getOperatorForId(id);
  }

  @Override
  public Collection<StopLocation> listStopLocations() {
    OTPRequestTimeoutException.checkForTimeout();
    return transitModel.getStopModel().listStopLocations();
  }

  @Override
  public Collection<RegularStop> listRegularStops() {
    OTPRequestTimeoutException.checkForTimeout();
    return transitModel.getStopModel().listRegularStops();
  }

  @Override
  public Collection<GroupStop> listGroupStops() {
    OTPRequestTimeoutException.checkForTimeout();
    return transitModel.getStopModel().listGroupStops();
  }

  @Override
  public StopLocation getStopLocation(FeedScopedId id) {
    return transitModel.getStopModel().getStopLocation(id);
  }

  @Override
  public Collection<StopLocation> getStopOrChildStops(FeedScopedId id) {
    return transitModel.getStopModel().findStopOrChildStops(id);
  }

  @Override
  public Collection<StopLocationsGroup> listStopLocationGroups() {
    OTPRequestTimeoutException.checkForTimeout();
    return transitModel.getStopModel().listStopLocationGroups();
  }

  @Override
  public StopLocationsGroup getStopLocationsGroup(FeedScopedId id) {
    return transitModel.getStopModel().getStopLocationsGroup(id);
  }

  @Override
  public Trip getTripForId(FeedScopedId id) {
    TimetableSnapshot currentSnapshot = lazyGetTimeTableSnapShot();
    if (currentSnapshot != null) {
      Trip trip = currentSnapshot.getRealTimeAddedTrip(id);
      if (trip != null) {
        return trip;
      }
    }
    return getScheduledTripForId(id);
  }

  @Nullable
  @Override
  public Trip getScheduledTripForId(FeedScopedId id) {
    return this.transitModelIndex.getTripForId(id);
  }

  @Override
  public Collection<Trip> getAllTrips() {
    OTPRequestTimeoutException.checkForTimeout();
    TimetableSnapshot currentSnapshot = lazyGetTimeTableSnapShot();
    if (currentSnapshot != null) {
      return new CollectionsView<>(
        transitModelIndex.getAllTrips(),
        currentSnapshot.listRealTimeAddedTrips()
      );
    }
    return Collections.unmodifiableCollection(transitModelIndex.getAllTrips());
  }

  @Override
  public Collection<Route> getAllRoutes() {
    OTPRequestTimeoutException.checkForTimeout();
    TimetableSnapshot currentSnapshot = lazyGetTimeTableSnapShot();
    if (currentSnapshot != null) {
      return new CollectionsView<>(
        transitModelIndex.getAllRoutes(),
        currentSnapshot.listRealTimeAddedRoutes()
      );
    }
    return transitModelIndex.getAllRoutes();
  }

  @Override
  public TripPattern getPatternForTrip(Trip trip) {
    TimetableSnapshot currentSnapshot = lazyGetTimeTableSnapShot();
    if (currentSnapshot != null) {
      TripPattern realtimeAddedTripPattern = currentSnapshot.getRealTimeAddedPatternForTrip(trip);
      if (realtimeAddedTripPattern != null) {
        return realtimeAddedTripPattern;
      }
    }
    return this.transitModelIndex.getPatternForTrip(trip);
  }

  @Override
  public TripPattern getPatternForTrip(Trip trip, LocalDate serviceDate) {
    TripPattern realtimePattern = getNewTripPatternForModifiedTrip(trip.getId(), serviceDate);
    if (realtimePattern != null) {
      return realtimePattern;
    }
    return getPatternForTrip(trip);
  }

  @Override
  public Collection<TripPattern> getPatternsForRoute(Route route) {
    OTPRequestTimeoutException.checkForTimeout();
    Collection<TripPattern> tripPatterns = new HashSet<>(
      transitModelIndex.getPatternsForRoute(route)
    );
    TimetableSnapshot currentSnapshot = lazyGetTimeTableSnapShot();
    if (currentSnapshot != null) {
      Collection<TripPattern> realTimeAddedPatternForRoute = currentSnapshot.getRealTimeAddedPatternForRoute(
        route
      );
      tripPatterns.addAll(realTimeAddedPatternForRoute);
    }
    return tripPatterns;
  }

  @Override
  public MultiModalStation getMultiModalStationForStation(Station station) {
    return this.transitModel.getStopModel().getMultiModalStationForStation(station);
  }

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
  @Override
  public List<StopTimesInPattern> stopTimesForStop(
    StopLocation stop,
    Instant startTime,
    Duration timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancelledTrips
  ) {
    OTPRequestTimeoutException.checkForTimeout();
    return StopTimesHelper.stopTimesForStop(
      this,
      stop,
      startTime,
      timeRange,
      numberOfDepartures,
      arrivalDeparture,
      includeCancelledTrips
    );
  }

  /**
   * Get a list of all trips that pass through a stop during a single ServiceDate. Useful when
   * creating complete stop timetables for a single day.
   *
   * @param stop        Stop object to perform the search for
   * @param serviceDate Return all departures for the specified date
   */
  @Override
  public List<StopTimesInPattern> getStopTimesForStop(
    StopLocation stop,
    LocalDate serviceDate,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancellations
  ) {
    OTPRequestTimeoutException.checkForTimeout();
    return StopTimesHelper.stopTimesForStop(
      this,
      stop,
      serviceDate,
      arrivalDeparture,
      includeCancellations
    );
  }

  /**
   * Fetch upcoming vehicle departures from a stop for a specific pattern, passing the stop for the
   * previous, current and next service date. It uses a priority queue to keep track of the next
   * departures. The queue is shared between all dates, as services from the previous service date
   * can visit the stop later than the current service date's services.
   * <p>
   * TODO: Add frequency based trips
   *
   * @param stop               Stop object to perform the search for
   * @param pattern            Pattern object to perform the search for
   * @param startTime          Start time for the search.
   * @param timeRange          Searches forward for timeRange from startTime
   * @param numberOfDepartures Number of departures to fetch per pattern
   * @param arrivalDeparture   Filter by arrivals, departures, or both
   */
  @Override
  public List<TripTimeOnDate> stopTimesForPatternAtStop(
    StopLocation stop,
    TripPattern pattern,
    Instant startTime,
    Duration timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancellations
  ) {
    OTPRequestTimeoutException.checkForTimeout();
    return StopTimesHelper.stopTimesForPatternAtStop(
      this,
      stop,
      pattern,
      startTime,
      timeRange,
      numberOfDepartures,
      arrivalDeparture,
      includeCancellations
    );
  }

  /**
   * Returns all the patterns for a specific stop. If includeRealtimeUpdates is set, new patterns
   * added by realtime updates are added to the collection.
   * A set is used here because trip patterns
   * that were updated by realtime data is both part of the TransitModelIndex and the TimetableSnapshot
   */
  @Override
  public Collection<TripPattern> getPatternsForStop(
    StopLocation stop,
    boolean includeRealtimeUpdates
  ) {
    Set<TripPattern> tripPatterns = new HashSet<>(getPatternsForStop(stop));

    if (includeRealtimeUpdates) {
      TimetableSnapshot currentSnapshot = lazyGetTimeTableSnapShot();
      if (currentSnapshot != null) {
        tripPatterns.addAll(currentSnapshot.getPatternsForStop(stop));
      }
    }
    return tripPatterns;
  }

  @Override
  public Collection<GroupOfRoutes> getGroupsOfRoutes() {
    OTPRequestTimeoutException.checkForTimeout();
    return transitModelIndex.getAllGroupOfRoutes();
  }

  @Override
  public Collection<Route> getRoutesForGroupOfRoutes(GroupOfRoutes groupOfRoutes) {
    OTPRequestTimeoutException.checkForTimeout();
    return transitModelIndex.getRoutesForGroupOfRoutes(groupOfRoutes);
  }

  @Override
  public GroupOfRoutes getGroupOfRoutesForId(FeedScopedId id) {
    return transitModelIndex.getGroupOfRoutesForId(id);
  }

  /**
   * Get the most up-to-date timetable for the given TripPattern, as of right now. There should
   * probably be a less awkward way to do this that just gets the latest entry from the resolver
   * without making a fake routing request.
   */
  @Override
  public Timetable getTimetableForTripPattern(TripPattern tripPattern, LocalDate serviceDate) {
    OTPRequestTimeoutException.checkForTimeout();
    TimetableSnapshot currentSnapshot = lazyGetTimeTableSnapShot();
    return currentSnapshot != null
      ? currentSnapshot.resolve(tripPattern, serviceDate)
      : tripPattern.getScheduledTimetable();
  }

  @Override
  public TripPattern getNewTripPatternForModifiedTrip(FeedScopedId tripId, LocalDate serviceDate) {
    TimetableSnapshot currentSnapshot = lazyGetTimeTableSnapShot();
    if (currentSnapshot == null) {
      return null;
    }
    return currentSnapshot.getNewTripPatternForModifiedTrip(tripId, serviceDate);
  }

  @Override
  public boolean hasNewTripPatternsForModifiedTrips() {
    TimetableSnapshot currentSnapshot = lazyGetTimeTableSnapShot();
    if (currentSnapshot == null) {
      return false;
    }
    return currentSnapshot.hasNewTripPatternsForModifiedTrips();
  }

  /**
   * Lazy-initialization of TimetableSnapshot
   *
   * @return The same TimetableSnapshot is returned throughout the lifecycle of this object.
   */
  @Nullable
  private TimetableSnapshot lazyGetTimeTableSnapShot() {
    if (this.timetableSnapshot == null) {
      timetableSnapshot = transitModel.getTimetableSnapshot();
    }
    return this.timetableSnapshot;
  }

  @Override
  public TripOnServiceDate getTripOnServiceDateById(FeedScopedId tripOnServiceDateId) {
    TimetableSnapshot currentSnapshot = lazyGetTimeTableSnapShot();
    if (currentSnapshot != null) {
      TripOnServiceDate tripOnServiceDate = currentSnapshot.getRealTimeAddedTripOnServiceDateById(
        tripOnServiceDateId
      );
      if (tripOnServiceDate != null) {
        return tripOnServiceDate;
      }
    }
    return transitModel.getTripOnServiceDateById(tripOnServiceDateId);
  }

  @Override
  public Collection<TripOnServiceDate> getAllTripOnServiceDates() {
    TimetableSnapshot currentSnapshot = lazyGetTimeTableSnapShot();
    if (currentSnapshot != null) {
      return new CollectionsView<>(
        transitModel.getAllTripsOnServiceDates(),
        currentSnapshot.listRealTimeAddedTripOnServiceDate()
      );
    }
    return transitModel.getAllTripsOnServiceDates();
  }

  @Override
  public TripOnServiceDate getTripOnServiceDateForTripAndDay(
    TripIdAndServiceDate tripIdAndServiceDate
  ) {
    TimetableSnapshot currentSnapshot = lazyGetTimeTableSnapShot();
    if (currentSnapshot != null) {
      TripOnServiceDate tripOnServiceDate = currentSnapshot.getRealTimeAddedTripOnServiceDateForTripAndDay(
        tripIdAndServiceDate
      );
      if (tripOnServiceDate != null) {
        return tripOnServiceDate;
      }
    }
    return transitModelIndex.getTripOnServiceDateForTripAndDay(tripIdAndServiceDate);
  }

  /**
   * Returns a list of TripOnServiceDates that match the filtering defined in the request.
   *
   * @param request - A TripOnServiceDateRequest object with filtering defined.
   * @return - A list of TripOnServiceDates
   */
  @Override
  public List<TripOnServiceDate> getTripOnServiceDates(TripOnServiceDateRequest request) {
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);
    return getAllTripOnServiceDates().stream().filter(matcher::match).toList();
  }

  /**
   * TODO OTP2 - This is NOT THREAD-SAFE and is used in the real-time updaters, we need to fix
   * this when doing the issue #3030.
   */
  @Override
  public FeedScopedId getOrCreateServiceIdForDate(LocalDate serviceDate) {
    return transitModel.getOrCreateServiceIdForDate(serviceDate);
  }

  @Override
  public void addTransitMode(TransitMode mode) {
    this.transitModel.addTransitMode(mode);
  }

  @Override
  public Set<TransitMode> getTransitModes() {
    return this.transitModel.getTransitModes();
  }

  @Override
  public Collection<PathTransfer> getTransfersByStop(StopLocation stop) {
    return this.transitModel.getTransfersByStop(stop);
  }

  @Override
  public TransitLayer getTransitLayer() {
    OTPRequestTimeoutException.checkForTimeout();
    return this.transitModel.getTransitLayer();
  }

  @Override
  public TransitLayer getRealtimeTransitLayer() {
    OTPRequestTimeoutException.checkForTimeout();
    return this.transitModel.getRealtimeTransitLayer();
  }

  @Override
  public void setTransitLayer(TransitLayer transitLayer) {
    this.transitModel.setTransitLayer(transitLayer);
  }

  @Override
  public void setRealtimeTransitLayer(TransitLayer realtimeTransitLayer) {
    transitModel.setRealtimeTransitLayer(realtimeTransitLayer);
  }

  @Override
  public boolean hasRealtimeTransitLayer() {
    return transitModel.hasRealtimeTransitLayer();
  }

  @Override
  public CalendarService getCalendarService() {
    return this.transitModel.getCalendarService();
  }

  @Override
  public ZoneId getTimeZone() {
    return this.transitModel.getTimeZone();
  }

  @Override
  public TransitAlertService getTransitAlertService() {
    return this.transitModel.getTransitAlertService();
  }

  @Override
  public FlexIndex getFlexIndex() {
    return this.transitModelIndex.getFlexIndex();
  }

  @Override
  public ZonedDateTime getTransitServiceEnds() {
    return transitModel.getTransitServiceEnds();
  }

  @Override
  public ZonedDateTime getTransitServiceStarts() {
    return transitModel.getTransitServiceStarts();
  }

  @Override
  public Collection<RegularStop> findRegularStops(Envelope envelope) {
    OTPRequestTimeoutException.checkForTimeout();
    return transitModel.getStopModel().findRegularStops(envelope);
  }

  @Override
  public Collection<AreaStop> findAreaStops(Envelope envelope) {
    OTPRequestTimeoutException.checkForTimeout();
    return transitModel.getStopModel().findAreaStops(envelope);
  }

  @Override
  public GraphUpdaterStatus getUpdaterStatus() {
    return transitModel.getUpdaterManager();
  }

  @Override
  public List<TransitMode> getModesOfStopLocationsGroup(StopLocationsGroup station) {
    return sortByOccurrenceAndReduce(
      station.getChildStops().stream().flatMap(this::getPatternModesOfStop)
    )
      .toList();
  }

  @Override
  public List<TransitMode> getModesOfStopLocation(StopLocation stop) {
    return sortByOccurrenceAndReduce(getPatternModesOfStop(stop)).toList();
  }

  @Override
  public Deduplicator getDeduplicator() {
    return transitModel.getDeduplicator();
  }

  @Override
  public Set<LocalDate> getAllServiceCodes() {
    return Collections.unmodifiableSet(transitModelIndex.getServiceCodesRunningForDate().keySet());
  }

  @Override
  public Map<LocalDate, TIntSet> getServiceCodesRunningForDate() {
    return Collections.unmodifiableMap(transitModelIndex.getServiceCodesRunningForDate());
  }

  /**
   * For each pattern visiting this {@link StopLocation} return its {@link TransitMode}
   */
  private Stream<TransitMode> getPatternModesOfStop(StopLocation stop) {
    if (stop.getGtfsVehicleType() != null) {
      return Stream.of(stop.getGtfsVehicleType());
    } else {
      return getPatternsForStop(stop).stream().map(TripPattern::getMode);
    }
  }

  @Override
  public TransferService getTransferService() {
    return transitModel.getTransferService();
  }

  @Override
  public boolean transitFeedCovers(Instant dateTime) {
    return transitModel.transitFeedCovers(dateTime);
  }

  /**
   * Take a stream of T, count the occurrences of each value and return it in order of frequency
   * from high to low.
   * <p>
   * Example: [a,b,b,c,c,c] will return [c,b,a]
   */
  private static <T> Stream<T> sortByOccurrenceAndReduce(Stream<T> input) {
    return input
      .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
      .entrySet()
      .stream()
      .sorted(Map.Entry.<T, Long>comparingByValue().reversed())
      .map(Map.Entry::getKey);
  }
}
