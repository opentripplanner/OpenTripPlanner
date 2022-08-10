package org.opentripplanner.transit.service;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TimetableSnapshotProvider;
import org.opentripplanner.model.TripIdAndServiceDate;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.routing.stoptimes.StopTimesHelper;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.TransitEntity;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.FlexStopLocation;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.model.site.StopCollection;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
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

  public DefaultTransitService(TransitModel transitModel) {
    this.transitModel = transitModel;
    this.transitModelIndex = transitModel.getTransitModelIndex();
  }

  /** {@link TransitModel#getFeedIds()} */
  @Override
  public Collection<String> getFeedIds() {
    return this.transitModel.getFeedIds();
  }

  /** {@link TransitModel#getAgencies()} */
  @Override
  public Collection<Agency> getAgencies() {
    return this.transitModel.getAgencies();
  }

  /** {@link TransitModel#getFeedInfo(String)} ()} */
  @Override
  public FeedInfo getFeedInfo(String feedId) {
    return this.transitModel.getFeedInfo(feedId);
  }

  /** {@link TransitModel#addAgency(String, Agency)} */
  @Override
  public void addAgency(String feedId, Agency agency) {
    this.transitModel.addAgency(feedId, agency);
  }

  /** {@link TransitModel#addFeedInfo(FeedInfo)} */
  @Override
  public void addFeedInfo(FeedInfo info) {
    this.transitModel.addFeedInfo(info);
  }

  /** {@link TransitModel#getOperators()} */
  @Override
  public Collection<Operator> getOperators() {
    return this.transitModel.getOperators();
  }

  /** {@link TransitModel#getNoticesByElement()} */
  @Override
  public Collection<Notice> getNoticesByEntity(TransitEntity entity) {
    return this.transitModel.getNoticesByElement().get(entity);
  }

  /** {@link TransitModel#getTripPatternForId(FeedScopedId)} */
  @Override
  public TripPattern getTripPatternForId(FeedScopedId id) {
    return this.transitModel.getTripPatternForId(id);
  }

  /** {@link TransitModel#getAllTripPatterns()} ()} */
  @Override
  public Collection<TripPattern> getAllTripPatterns() {
    return this.transitModel.getAllTripPatterns();
  }

  /** {@link TransitModel#getNoticesByElement()} */
  @Override
  public Collection<Notice> getNotices() {
    return this.transitModel.getNoticesByElement().values();
  }

  /** {@link StopModel#getStationById(FeedScopedId)} */
  @Override
  public Station getStationById(FeedScopedId id) {
    return this.transitModel.getStopModel().getStationById(id);
  }

  /** {@link StopModel#getMultiModalStation(FeedScopedId)} */
  @Override
  public MultiModalStation getMultiModalStation(FeedScopedId id) {
    return this.transitModel.getStopModel().getMultiModalStation(id);
  }

  /** {@link StopModel#getStations()} */
  @Override
  public Collection<Station> getStations() {
    return this.transitModel.getStopModel().getStations();
  }

  /** {@link TransitModel#getServiceCodes()} */
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

  /** {@link StopModel#getLocationById(FeedScopedId)} */
  @Override
  public FlexStopLocation getLocationById(FeedScopedId id) {
    return this.transitModel.getStopModel().getLocationById(id);
  }

  /** {@link TransitModelIndex#getAgencyForId(FeedScopedId)} */
  @Override
  public Agency getAgencyForId(FeedScopedId id) {
    return this.transitModelIndex.getAgencyForId(id);
  }

  /** {@link StopModelIndex#getStopForId(FeedScopedId)} */
  @Override
  public StopLocation getStopForId(FeedScopedId id) {
    return this.transitModel.getStopModel().getStopModelIndex().getStopForId(id);
  }

  /** {@link TransitModelIndex#getRouteForId(FeedScopedId)} */
  @Override
  public Route getRouteForId(FeedScopedId id) {
    return this.transitModelIndex.getRouteForId(id);
  }

  /** {@link TransitModelIndex#addRoutes(Route)} */
  @Override
  public void addRoutes(Route route) {
    this.transitModelIndex.addRoutes(route);
  }

  /** {@link TransitModelIndex#getRoutesForStop(StopLocation)} */
  @Override
  public Set<Route> getRoutesForStop(StopLocation stop) {
    return this.transitModelIndex.getRoutesForStop(stop);
  }

  /** {@link TransitModelIndex#getPatternsForStop(StopLocation)} */
  @Override
  public Collection<TripPattern> getPatternsForStop(StopLocation stop) {
    return this.transitModelIndex.getPatternsForStop(stop);
  }

  @Override
  public Collection<Trip> getTripsForStop(StopLocation stop) {
    return this.transitModelIndex.getTripsForStop(stop);
  }

  /** {@link TransitModelIndex#getAllOperators()} */
  @Override
  public Collection<Operator> getAllOperators() {
    return this.transitModelIndex.getAllOperators();
  }

  /** {@link TransitModelIndex#getOperatorForId()} */
  @Override
  public Operator getOperatorForId(FeedScopedId id) {
    return this.transitModelIndex.getOperatorForId().get(id);
  }

  /** {@link StopModelIndex#getAllStops()} */
  @Override
  public Collection<StopLocation> getAllStops() {
    return transitModel.getStopModel().getStopModelIndex().getAllStops();
  }

  @Override
  public StopLocation getStopLocationById(FeedScopedId id) {
    return transitModel.getStopModel().getStopModelIndex().getStopForId(id);
  }

  @Override
  public Collection<StopCollection> getAllStopCollections() {
    return transitModel.getStopModel().getAllStopCollections().toList();
  }

  @Override
  public StopCollection getStopCollectionById(FeedScopedId id) {
    return transitModel.getStopModel().getStopCollectionById(id);
  }

  /** {@link TransitModelIndex#getTripForId()} */
  @Override
  public Trip getTripForId(FeedScopedId id) {
    return this.transitModelIndex.getTripForId().get(id);
  }

  @Override
  public Collection<Trip> getAllTrips() {
    return transitModelIndex.getTripForId().values();
  }

  /** {@link TransitModelIndex#getAllRoutes()} */
  @Override
  public Collection<Route> getAllRoutes() {
    return this.transitModelIndex.getAllRoutes();
  }

  /** {@link TransitModelIndex#getPatternForTrip()} */
  @Override
  public TripPattern getPatternForTrip(Trip trip) {
    return this.transitModelIndex.getPatternForTrip().get(trip);
  }

  /** {@link TransitModelIndex#getPatternsForRoute()} */
  @Override
  public Collection<TripPattern> getPatternsForRoute(Route route) {
    return this.transitModelIndex.getPatternsForRoute().get(route);
  }

  /** {@link StopModelIndex#getMultiModalStationForStation(Station)} */
  @Override
  public MultiModalStation getMultiModalStationForStation(Station station) {
    return this.transitModel.getStopModel()
      .getStopModelIndex()
      .getMultiModalStationForStation(station);
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
    ArrivalDeparture arrivalDeparture
  ) {
    return StopTimesHelper.stopTimesForStop(this, stop, serviceDate, arrivalDeparture);
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
    ArrivalDeparture arrivalDeparture
  ) {
    return StopTimesHelper.stopTimesForPatternAtStop(
      this,
      stop,
      pattern,
      startTime,
      timeRange,
      numberOfDepartures,
      arrivalDeparture
    );
  }

  /**
   * Returns all the patterns for a specific stop. If includeRealtimeUpdates is set, new patterns
   * added by realtime updates are added to the collection.
   */
  @Override
  public Collection<TripPattern> getPatternsForStop(
    StopLocation stop,
    boolean includeRealtimeUpdates
  ) {
    return transitModel
      .getTransitModelIndex()
      .getPatternsForStop(stop, includeRealtimeUpdates ? lazyGetTimeTableSnapShot() : null);
  }

  @Override
  public Collection<GroupOfRoutes> getGroupsOfRoutes() {
    return transitModelIndex.getRoutesForGroupOfRoutes().keySet();
  }

  @Override
  public Collection<Route> getRoutesForGroupOfRoutes(GroupOfRoutes groupOfRoutes) {
    return transitModelIndex.getRoutesForGroupOfRoutes().get(groupOfRoutes);
  }

  @Override
  public GroupOfRoutes getGroupOfRoutesForId(FeedScopedId id) {
    return transitModelIndex.getGroupOfRoutesForId().get(id);
  }

  /**
   * Get the most up-to-date timetable for the given TripPattern, as of right now. There should
   * probably be a less awkward way to do this that just gets the latest entry from the resolver
   * without making a fake routing request.
   */
  @Override
  public Timetable getTimetableForTripPattern(TripPattern tripPattern, LocalDate serviceDate) {
    TimetableSnapshot timetableSnapshot = lazyGetTimeTableSnapShot();
    return timetableSnapshot != null
      ? timetableSnapshot.resolve(tripPattern, serviceDate)
      : tripPattern.getScheduledTimetable();
  }

  @Override
  public TripPattern getRealtimeAddedTripPattern(FeedScopedId tripId, LocalDate serviceDate) {
    TimetableSnapshot timetableSnapshot = lazyGetTimeTableSnapShot();
    if (timetableSnapshot == null) {
      return null;
    }
    return timetableSnapshot.getRealtimeAddedTripPattern(tripId, serviceDate);
  }

  @Override
  public boolean hasRealtimeAddedTripPatterns() {
    TimetableSnapshot timetableSnapshot = lazyGetTimeTableSnapShot();
    if (timetableSnapshot == null) {
      return false;
    }
    return timetableSnapshot.hasRealtimeAddedTripPatterns();
  }

  /**
   * Lazy-initialization of TimetableSnapshot
   *
   * @return The same TimetableSnapshot is returned throughout the lifecycle of this object.
   */
  private TimetableSnapshot lazyGetTimeTableSnapShot() {
    if (this.timetableSnapshot == null) {
      timetableSnapshot = transitModel.getTimetableSnapshot();
    }
    return this.timetableSnapshot;
  }

  @Override
  public TripOnServiceDate getTripOnServiceDateById(FeedScopedId datedServiceJourneyId) {
    TimetableSnapshot timetableSnapshot = lazyGetTimeTableSnapShot();
    if (timetableSnapshot != null) {
      TripOnServiceDate tripOnServiceDate = timetableSnapshot
        .getRealtimeAddedTripOnServiceDate()
        .get(datedServiceJourneyId);
      if (tripOnServiceDate != null) {
        return tripOnServiceDate;
      }
    }
    return transitModelIndex.getTripOnServiceDateById().get(datedServiceJourneyId);
  }

  @Override
  public Collection<TripOnServiceDate> getAllTripOnServiceDates() {
    return transitModelIndex.getTripOnServiceDateForTripAndDay().values();
  }

  @Override
  public TripOnServiceDate getTripOnServiceDateForTripAndDay(
    TripIdAndServiceDate tripIdAndServiceDate
  ) {
    TimetableSnapshot timetableSnapshot = lazyGetTimeTableSnapShot();
    if (timetableSnapshot != null) {
      TripOnServiceDate tripOnServiceDate = timetableSnapshot
        .getRealtimeAddedTripOnServiceDateByTripIdAndServiceDate()
        .get(tripIdAndServiceDate);
      if (tripOnServiceDate != null) {
        return tripOnServiceDate;
      }
    }
    return transitModelIndex.getTripOnServiceDateForTripAndDay().get(tripIdAndServiceDate);
  }

  /** {@link TransitModel#addTransitMode(TransitMode)} */
  @Override
  public void addTransitMode(TransitMode mode) {
    this.transitModel.addTransitMode(mode);
  }

  /** {@link TransitModel#getTransitModes()} */
  @Override
  public Set<TransitMode> getTransitModes() {
    return this.transitModel.getTransitModes();
  }

  /** {@link TransitModel#getTransfersByStop(StopLocation)} */
  @Override
  public Collection<PathTransfer> getTransfersByStop(StopLocation stop) {
    return this.transitModel.getTransfersByStop(stop);
  }

  /** {@link TransitModel#getOrSetupTimetableSnapshotProvider(Function)} */
  @Override
  public <T extends TimetableSnapshotProvider> T getOrSetupTimetableSnapshotProvider(
    Function<TransitModel, T> creator
  ) {
    return this.transitModel.getOrSetupTimetableSnapshotProvider(creator);
  }

  /** {@link TransitModel#getTransitLayer()} */
  @Override
  public TransitLayer getTransitLayer() {
    return this.transitModel.getTransitLayer();
  }

  @Override
  public TransitLayer getRealtimeTransitLayer() {
    return this.transitModel.getRealtimeTransitLayer();
  }

  /** {@link TransitModel#setTransitLayer(TransitLayer)} */
  @Override
  public void setTransitLayer(TransitLayer transitLayer) {
    this.transitModel.setTransitLayer(transitLayer);
  }

  /** {@link TransitModel#getCalendarService()} */
  @Override
  public CalendarService getCalendarService() {
    return this.transitModel.getCalendarService();
  }

  /** {@link TransitModel#getTimeZone()} */
  @Override
  public ZoneId getTimeZone() {
    return this.transitModel.getTimeZone();
  }

  /** {@link TransitModel#getTransitAlertService()} */
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

  /** {@link StopModelIndex#getStopVertexForStop(Stop)} */
  @Override
  public TransitStopVertex getStopVertexForStop(Stop stop) {
    return transitModel.getStopModel().getStopVertexForStop(stop);
  }

  @Override
  public Collection<Stop> queryStopSpatialIndex(Envelope envelope) {
    return transitModel.getStopModel().getStopModelIndex().queryStopSpatialIndex(envelope);
  }

  @Override
  public GraphUpdaterStatus getUpdaterStatus() {
    return transitModel.getUpdaterManager();
  }

  @Override
  public Optional<Coordinate> getCenter() {
    return transitModel.getStopModel().getCenter();
  }

  @Override
  public TransferService getTransferService() {
    return transitModel.getTransferService();
  }

  @Override
  public boolean transitFeedCovers(Instant dateTime) {
    return transitModel.transitFeedCovers(dateTime);
  }
}
