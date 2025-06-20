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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
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
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.transit.RegularStopMatcherFactory;
import org.opentripplanner.transit.model.filter.transit.RouteMatcherFactory;
import org.opentripplanner.transit.model.filter.transit.StopLocationMatcherFactory;
import org.opentripplanner.transit.model.filter.transit.TripMatcherFactory;
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
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.updater.GraphUpdaterStatus;
import org.opentripplanner.utils.collection.CollectionsView;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * Default implementation of the Transit Service and Transit Editor Service.
 * A new instance of this class should be created for each request.
 * This ensures that the same TimetableSnapshot is used for the
 * duration of the request (which may involve several method calls).
 */
public class DefaultTransitService implements TransitEditorService {

  private final TimetableRepository timetableRepository;

  private final TimetableRepositoryIndex timetableRepositoryIndex;

  /**
   * A nullable timetable snapshot containing real-time updates. If {@code null} then this
   * instance does not contain any real-time information.
   */
  @Nullable
  private final TimetableSnapshot timetableSnapshot;

  /**
   * Helper for fetching stop times for APIs.
   */
  private final StopTimesHelper stopTimesHelper;

  /**
   * Create a service without a real-time snapshot (and therefore without any real-time data).
   */
  public DefaultTransitService(TimetableRepository timetableRepository) {
    this(timetableRepository, null);
  }

  @Inject
  public DefaultTransitService(
    TimetableRepository timetableRepository,
    @Nullable TimetableSnapshot timetableSnapshot
  ) {
    this.timetableRepository = timetableRepository;
    this.timetableRepositoryIndex = timetableRepository.getTimetableRepositoryIndex();
    this.timetableSnapshot = timetableSnapshot;
    this.stopTimesHelper = new StopTimesHelper(this);
  }

  @Override
  public Optional<List<TripTimeOnDate>> getScheduledTripTimes(Trip trip) {
    TripPattern tripPattern = findPattern(trip);
    return Optional.ofNullable(
      TripTimeOnDate.fromTripTimes(tripPattern.getScheduledTimetable(), trip)
    );
  }

  @Override
  public Optional<List<TripTimeOnDate>> getTripTimeOnDates(Trip trip, LocalDate serviceDate) {
    TripPattern pattern = findPattern(trip, serviceDate);

    Timetable timetable = findTimetable(pattern, serviceDate);

    // This check is made here to avoid changing TripTimeOnDate.fromTripTimes
    TripTimes times = timetable.getTripTimes(trip);
    if (
      times == null ||
      !this.getServiceCodesRunningForDate(serviceDate).contains(times.getServiceCode())
    ) {
      return Optional.empty();
    } else {
      Instant midnight = ServiceDateUtils.asStartOfService(
        serviceDate,
        this.getTimeZone()
      ).toInstant();
      return Optional.of(TripTimeOnDate.fromTripTimes(timetable, trip, serviceDate, midnight));
    }
  }

  @Override
  public Collection<String> listFeedIds() {
    return this.timetableRepository.getFeedIds();
  }

  @Override
  public Collection<Agency> listAgencies() {
    OTPRequestTimeoutException.checkForTimeout();
    return this.timetableRepository.getAgencies();
  }

  @Override
  public Optional<Agency> findAgency(FeedScopedId id) {
    return this.timetableRepository.findAgencyById(id);
  }

  @Override
  public FeedInfo getFeedInfo(String feedId) {
    return this.timetableRepository.getFeedInfo(feedId);
  }

  @Override
  public void addAgency(Agency agency) {
    this.timetableRepository.addAgency(agency);
  }

  @Override
  public void addFeedInfo(FeedInfo info) {
    this.timetableRepository.addFeedInfo(info);
  }

  @Override
  public Collection<Notice> findNotices(AbstractTransitEntity<?, ?> entity) {
    return this.timetableRepository.getNoticesByElement().get(entity);
  }

  @Override
  public TripPattern getTripPattern(FeedScopedId id) {
    return this.timetableRepository.getTripPatternForId(id);
  }

  @Override
  public Collection<TripPattern> listTripPatterns() {
    OTPRequestTimeoutException.checkForTimeout();
    return this.timetableRepository.getAllTripPatterns();
  }

  @Override
  public Station getStation(FeedScopedId id) {
    return this.timetableRepository.getSiteRepository().getStationById(id);
  }

  @Override
  public MultiModalStation getMultiModalStation(FeedScopedId id) {
    return this.timetableRepository.getSiteRepository().getMultiModalStation(id);
  }

  @Override
  public Collection<Station> listStations() {
    OTPRequestTimeoutException.checkForTimeout();
    return this.timetableRepository.getSiteRepository().listStations();
  }

  @Override
  public Integer getServiceCode(FeedScopedId id) {
    return this.timetableRepository.getServiceCodes().get(id);
  }

  @Override
  public TIntSet getServiceCodesRunningForDate(LocalDate serviceDate) {
    return timetableRepositoryIndex
      .getServiceCodesRunningForDate()
      .getOrDefault(serviceDate, new TIntHashSet());
  }

  @Override
  public Agency getAgency(FeedScopedId id) {
    return this.timetableRepositoryIndex.getAgencyForId(id);
  }

  @Override
  public RegularStop getRegularStop(FeedScopedId id) {
    return this.timetableRepository.getSiteRepository().getRegularStop(id);
  }

  @Override
  public Route getRoute(FeedScopedId id) {
    if (timetableSnapshot != null) {
      Route realtimeAddedRoute = timetableSnapshot.getRealtimeAddedRoute(id);
      if (realtimeAddedRoute != null) {
        return realtimeAddedRoute;
      }
    }
    return timetableRepositoryIndex.getRouteForId(id);
  }

  @Override
  public Collection<Route> getRoutes(Collection<FeedScopedId> ids) {
    return ids.stream().map(this::getRoute).filter(Objects::nonNull).toList();
  }

  @Override
  public Collection<Route> findRoutes(FindRoutesRequest request) {
    Matcher<Route> matcher = RouteMatcherFactory.of(request, this.getFlexIndex()::contains);
    return listRoutes().stream().filter(matcher::match).toList();
  }

  /**
   * Add a route to the transit model.
   * Used only in unit tests.
   */
  @Override
  public void addRoutes(Route route) {
    this.timetableRepositoryIndex.addRoutes(route);
  }

  @Override
  public Set<Route> findRoutes(StopLocation stop) {
    OTPRequestTimeoutException.checkForTimeout();
    return this.timetableRepositoryIndex.getRoutesForStop(stop);
  }

  @Override
  public Collection<TripPattern> findPatterns(StopLocation stop) {
    OTPRequestTimeoutException.checkForTimeout();
    return this.timetableRepositoryIndex.getPatternsForStop(stop);
  }

  @Override
  public Collection<Operator> listOperators() {
    OTPRequestTimeoutException.checkForTimeout();
    return this.timetableRepository.getOperators();
  }

  @Override
  public Operator getOperator(FeedScopedId id) {
    return this.timetableRepositoryIndex.getOperatorForId(id);
  }

  @Override
  public Collection<StopLocation> listStopLocations() {
    OTPRequestTimeoutException.checkForTimeout();
    return timetableRepository.getSiteRepository().listStopLocations();
  }

  @Override
  public Collection<GroupStop> listGroupStops() {
    OTPRequestTimeoutException.checkForTimeout();
    return timetableRepository.getSiteRepository().listGroupStops();
  }

  @Override
  public StopLocation getStopLocation(FeedScopedId id) {
    return timetableRepository.getSiteRepository().getStopLocation(id);
  }

  @Override
  public Collection<StopLocation> findStopLocations(FindStopLocationsRequest request) {
    Matcher<StopLocation> matcher = StopLocationMatcherFactory.of(request);
    return listStopLocations().stream().filter(matcher::match).toList();
  }

  @Override
  public Collection<StopLocation> findStopOrChildStops(FeedScopedId id) {
    return timetableRepository.getSiteRepository().findStopOrChildStops(id);
  }

  @Override
  public Collection<StopLocationsGroup> listStopLocationGroups() {
    OTPRequestTimeoutException.checkForTimeout();
    return timetableRepository.getSiteRepository().listStopLocationGroups();
  }

  @Override
  public StopLocationsGroup getStopLocationsGroup(FeedScopedId id) {
    return timetableRepository.getSiteRepository().getStopLocationsGroup(id);
  }

  @Override
  public Trip getTrip(FeedScopedId id) {
    if (timetableSnapshot != null) {
      Trip trip = timetableSnapshot.getRealTimeAddedTrip(id);
      if (trip != null) {
        return trip;
      }
    }
    return getScheduledTrip(id);
  }

  @Nullable
  @Override
  public Trip getScheduledTrip(FeedScopedId id) {
    return this.timetableRepositoryIndex.getTripForId(id);
  }

  /**
   * TODO This only supports realtime cancelled trips for now.
   */
  @Override
  public List<TripOnServiceDate> listCanceledTrips() {
    OTPRequestTimeoutException.checkForTimeout();
    if (timetableSnapshot == null) {
      return List.of();
    }
    List<TripOnServiceDate> canceledTrips = timetableSnapshot.listCanceledTrips();
    canceledTrips.sort(new TripOnServiceDateComparator());
    return canceledTrips;
  }

  @Override
  public Collection<Trip> listTrips() {
    OTPRequestTimeoutException.checkForTimeout();
    if (timetableSnapshot != null) {
      return new CollectionsView<>(
        timetableRepositoryIndex.getAllTrips(),
        timetableSnapshot.listRealTimeAddedTrips()
      );
    }
    return Collections.unmodifiableCollection(timetableRepositoryIndex.getAllTrips());
  }

  @Override
  public Collection<Route> listRoutes() {
    OTPRequestTimeoutException.checkForTimeout();
    if (timetableSnapshot != null) {
      return new CollectionsView<>(
        timetableRepositoryIndex.getAllRoutes(),
        timetableSnapshot.listRealTimeAddedRoutes()
      );
    }
    return timetableRepositoryIndex.getAllRoutes();
  }

  @Override
  public TripPattern findPattern(Trip trip) {
    if (timetableSnapshot != null) {
      TripPattern realtimeAddedTripPattern = timetableSnapshot.getRealTimeAddedPatternForTrip(trip);
      if (realtimeAddedTripPattern != null) {
        return realtimeAddedTripPattern;
      }
    }
    return this.timetableRepositoryIndex.getPatternForTrip(trip);
  }

  @Override
  public TripPattern findPattern(Trip trip, LocalDate serviceDate) {
    TripPattern realtimePattern = findNewTripPatternForModifiedTrip(trip.getId(), serviceDate);
    if (realtimePattern != null) {
      return realtimePattern;
    }
    return findPattern(trip);
  }

  @Override
  public Collection<TripPattern> findPatterns(Route route) {
    OTPRequestTimeoutException.checkForTimeout();
    Collection<TripPattern> tripPatterns = new HashSet<>(
      timetableRepositoryIndex.getPatternsForRoute(route)
    );
    if (timetableSnapshot != null) {
      Collection<TripPattern> realTimeAddedPatternForRoute =
        timetableSnapshot.getRealTimeAddedPatternForRoute(route);
      tripPatterns.addAll(realTimeAddedPatternForRoute);
    }
    return tripPatterns;
  }

  @Override
  public MultiModalStation findMultiModalStation(Station station) {
    return this.timetableRepository.getSiteRepository().getMultiModalStationForStation(station);
  }

  @Override
  public List<StopTimesInPattern> findStopTimesInPattern(
    StopLocation stop,
    Instant startTime,
    Duration timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancelledTrips
  ) {
    OTPRequestTimeoutException.checkForTimeout();
    return stopTimesHelper.stopTimesForStop(
      stop,
      startTime,
      timeRange,
      numberOfDepartures,
      arrivalDeparture,
      includeCancelledTrips,
      TripTimeOnDate.compareByDeparture()
    );
  }

  @Override
  public List<StopTimesInPattern> findStopTimesInPattern(
    StopLocation stop,
    LocalDate serviceDate,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancellations
  ) {
    OTPRequestTimeoutException.checkForTimeout();
    return stopTimesHelper.stopTimesForStop(
      stop,
      serviceDate,
      arrivalDeparture,
      includeCancellations
    );
  }

  @Override
  public List<TripTimeOnDate> findTripTimesOnDate(
    StopLocation stop,
    TripPattern pattern,
    Instant startTime,
    Duration timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancellations
  ) {
    OTPRequestTimeoutException.checkForTimeout();
    return stopTimesHelper.stopTimesForPatternAtStop(
      stop,
      pattern,
      startTime,
      timeRange,
      numberOfDepartures,
      arrivalDeparture,
      includeCancellations
    );
  }

  @Override
  public List<TripTimeOnDate> findTripTimesOnDate(TripTimeOnDateRequest request) {
    OTPRequestTimeoutException.checkForTimeout();
    return stopTimesHelper.findTripTimesOnDate(request);
  }

  /**
   * Returns all the patterns for a specific stop. If includeRealtimeUpdates is set, new patterns
   * added by realtime updates are added to the collection.
   * A set is used here because trip patterns
   * that were updated by realtime data is both part of the TimetableRepositoryIndex and the TimetableSnapshot
   */
  @Override
  public Collection<TripPattern> findPatterns(StopLocation stop, boolean includeRealtimeUpdates) {
    Set<TripPattern> tripPatterns = new HashSet<>(findPatterns(stop));

    if (includeRealtimeUpdates) {
      if (timetableSnapshot != null) {
        tripPatterns.addAll(timetableSnapshot.getPatternsForStop(stop));
      }
    }
    return tripPatterns;
  }

  @Override
  public Collection<GroupOfRoutes> listGroupsOfRoutes() {
    OTPRequestTimeoutException.checkForTimeout();
    return timetableRepositoryIndex.getAllGroupOfRoutes();
  }

  @Override
  public Collection<Route> findRoutes(GroupOfRoutes groupOfRoutes) {
    OTPRequestTimeoutException.checkForTimeout();
    return timetableRepositoryIndex.getRoutesForGroupOfRoutes(groupOfRoutes);
  }

  @Override
  public GroupOfRoutes getGroupOfRoutes(FeedScopedId id) {
    return timetableRepositoryIndex.getGroupOfRoutesForId(id);
  }

  /**
   * Get the most up-to-date timetable for the given TripPattern, as of right now. There should
   * probably be a less awkward way to do this that just gets the latest entry from the resolver
   * without making a fake routing request.
   */
  @Override
  public Timetable findTimetable(TripPattern tripPattern, LocalDate serviceDate) {
    OTPRequestTimeoutException.checkForTimeout();
    return timetableSnapshot != null
      ? timetableSnapshot.resolve(tripPattern, serviceDate)
      : tripPattern.getScheduledTimetable();
  }

  @Override
  public TripPattern findNewTripPatternForModifiedTrip(FeedScopedId tripId, LocalDate serviceDate) {
    if (timetableSnapshot == null) {
      return null;
    }
    return timetableSnapshot.getNewTripPatternForModifiedTrip(tripId, serviceDate);
  }

  @Override
  public boolean hasNewTripPatternsForModifiedTrips() {
    if (timetableSnapshot == null) {
      return false;
    }
    return timetableSnapshot.hasNewTripPatternsForModifiedTrips();
  }

  @Override
  public TripOnServiceDate getTripOnServiceDate(FeedScopedId id) {
    if (timetableSnapshot != null) {
      TripOnServiceDate tripOnServiceDate = timetableSnapshot.getRealTimeAddedTripOnServiceDateById(
        id
      );
      if (tripOnServiceDate != null) {
        return tripOnServiceDate;
      }
    }
    return timetableRepository.getTripOnServiceDateById(id);
  }

  @Override
  public Collection<TripOnServiceDate> listTripsOnServiceDate() {
    if (timetableSnapshot != null) {
      return new CollectionsView<>(
        timetableRepository.getAllTripsOnServiceDates(),
        timetableSnapshot.listRealTimeAddedTripOnServiceDate()
      );
    }
    return timetableRepository.getAllTripsOnServiceDates();
  }

  @Override
  public TripOnServiceDate getTripOnServiceDate(TripIdAndServiceDate tripIdAndServiceDate) {
    if (timetableSnapshot != null) {
      TripOnServiceDate tripOnServiceDate =
        timetableSnapshot.getRealTimeAddedTripOnServiceDateForTripAndDay(tripIdAndServiceDate);
      if (tripOnServiceDate != null) {
        return tripOnServiceDate;
      }
    }
    return timetableRepositoryIndex.getTripOnServiceDateForTripAndDay(tripIdAndServiceDate);
  }

  /**
   * Returns a list of TripOnServiceDates that match the filtering defined in the request.
   *
   * @param request - A TripOnServiceDateRequest object with filtering defined.
   * @return - A list of TripOnServiceDates
   */
  @Override
  public List<TripOnServiceDate> findTripsOnServiceDate(TripOnServiceDateRequest request) {
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);
    return listTripsOnServiceDate().stream().filter(matcher::match).toList();
  }

  @Override
  public boolean containsTrip(FeedScopedId id) {
    if (timetableSnapshot != null) {
      Trip trip = timetableSnapshot.getRealTimeAddedTrip(id);
      if (trip != null) {
        return true;
      }
    }
    return this.timetableRepositoryIndex.containsTrip(id);
  }

  @Override
  public Optional<RegularStop> findStopByScheduledStopPoint(FeedScopedId scheduledStopPoint) {
    return timetableRepository.findStopByScheduledStopPoint(scheduledStopPoint);
  }

  /**
   * Returns a list of Trips that match the filtering defined in the request.
   *
   * @param request - A TripRequest object with filtering defined.
   * @return - A list Trips
   */
  @Override
  public List<Trip> getTrips(TripRequest request) {
    Matcher<Trip> matcher = TripMatcherFactory.of(
      request,
      this.getCalendarService()::getServiceDatesForServiceId
    );
    return listTrips().stream().filter(matcher::match).toList();
  }

  /**
   * TODO OTP2 - This is NOT THREAD-SAFE and is used in the real-time updaters, we need to fix
   * this when doing the issue #3030.
   */
  @Override
  public FeedScopedId getOrCreateServiceIdForDate(LocalDate serviceDate) {
    return timetableRepository.getOrCreateServiceIdForDate(serviceDate);
  }

  @Override
  public void addTransitMode(TransitMode mode) {
    this.timetableRepository.addTransitMode(mode);
  }

  @Override
  public Set<TransitMode> listTransitModes() {
    return this.timetableRepository.getTransitModes();
  }

  @Override
  public Collection<PathTransfer> findPathTransfers(StopLocation stop) {
    return this.timetableRepository.getTransfersByStop(stop);
  }

  @Override
  public RaptorTransitData getRaptorTransitData() {
    OTPRequestTimeoutException.checkForTimeout();
    return this.timetableRepository.getRaptorTransitData();
  }

  @Override
  public RaptorTransitData getRealtimeRaptorTransitData() {
    OTPRequestTimeoutException.checkForTimeout();
    return this.timetableRepository.getRealtimeRaptorTransitData();
  }

  @Override
  public CalendarService getCalendarService() {
    return this.timetableRepository.getCalendarService();
  }

  @Override
  public ZoneId getTimeZone() {
    return this.timetableRepository.getTimeZone();
  }

  @Override
  public TransitAlertService getTransitAlertService() {
    return this.timetableRepository.getTransitAlertService();
  }

  @Override
  public FlexIndex getFlexIndex() {
    return this.timetableRepositoryIndex.getFlexIndex();
  }

  @Override
  public ZonedDateTime getTransitServiceEnds() {
    return timetableRepository.getTransitServiceEnds();
  }

  @Override
  public ZonedDateTime getTransitServiceStarts() {
    return timetableRepository.getTransitServiceStarts();
  }

  @Override
  public Collection<RegularStop> findRegularStopsByBoundingBox(Envelope envelope) {
    OTPRequestTimeoutException.checkForTimeout();
    return timetableRepository.getSiteRepository().findRegularStops(envelope);
  }

  @Override
  public Collection<RegularStop> findRegularStopsByBoundingBox(
    FindRegularStopsByBoundingBoxRequest request
  ) {
    OTPRequestTimeoutException.checkForTimeout();
    Collection<RegularStop> stops = timetableRepository
      .getSiteRepository()
      .findRegularStops(request.envelope());

    Matcher<RegularStop> matcher = RegularStopMatcherFactory.of(request, stop ->
      !findPatterns(stop, true).isEmpty()
    );
    return stops.stream().filter(matcher::match).toList();
  }

  @Override
  public Collection<AreaStop> findAreaStops(Envelope envelope) {
    OTPRequestTimeoutException.checkForTimeout();
    return timetableRepository.getSiteRepository().findAreaStops(envelope);
  }

  @Override
  public GraphUpdaterStatus getUpdaterStatus() {
    return timetableRepository.getUpdaterManager();
  }

  @Override
  public List<TransitMode> findTransitModes(StopLocationsGroup station) {
    return sortByOccurrenceAndReduce(
      station.getChildStops().stream().flatMap(this::getPatternModesOfStop)
    ).toList();
  }

  @Override
  public List<TransitMode> findTransitModes(StopLocation stop) {
    return sortByOccurrenceAndReduce(getPatternModesOfStop(stop)).toList();
  }

  @Override
  public Deduplicator getDeduplicator() {
    return timetableRepository.getDeduplicator();
  }

  @Override
  public Set<LocalDate> listServiceDates() {
    return Collections.unmodifiableSet(
      timetableRepositoryIndex.getServiceCodesRunningForDate().keySet()
    );
  }

  @Override
  public Map<LocalDate, TIntSet> getServiceCodesRunningForDate() {
    return Collections.unmodifiableMap(timetableRepositoryIndex.getServiceCodesRunningForDate());
  }

  /**
   * For each pattern visiting this {@link StopLocation} return its {@link TransitMode}
   */
  private Stream<TransitMode> getPatternModesOfStop(StopLocation stop) {
    if (stop.getVehicleType() != null) {
      return Stream.of(stop.getVehicleType());
    } else {
      return findPatterns(stop).stream().map(TripPattern::getMode);
    }
  }

  @Override
  public TransferService getTransferService() {
    return timetableRepository.getTransferService();
  }

  @Override
  public boolean transitFeedCovers(Instant dateTime) {
    return timetableRepository.transitFeedCovers(dateTime);
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

  private int getDepartureTime(TripOnServiceDate trip) {
    var pattern = findPattern(trip.getTrip());
    var timetable = timetableSnapshot.resolve(pattern, trip.getServiceDate());
    return timetable.getTripTimes(trip.getTrip()).getDepartureTime(0);
  }

  private class TripOnServiceDateComparator implements Comparator<TripOnServiceDate> {

    @Override
    public int compare(TripOnServiceDate t1, TripOnServiceDate t2) {
      if (t1.getServiceDate().isBefore(t2.getServiceDate())) {
        return -1;
      } else if (t2.getServiceDate().isBefore(t1.getServiceDate())) {
        return 1;
      }
      var departure1 = getDepartureTime(t1);
      var departure2 = getDepartureTime(t2);
      if (departure1 < departure2) {
        return -1;
      } else if (departure1 > departure2) {
        return 1;
      } else {
        // identical departure day and time, so sort by unique feedscope id
        return t1.getTrip().getId().compareTo(t2.getTrip().getId());
      }
    }
  }
}
