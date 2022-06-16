package org.opentripplanner.transit.service;

import com.google.common.collect.Multimap;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TimetableSnapshotProvider;
import org.opentripplanner.model.TripIdAndServiceDate;
import org.opentripplanner.model.TripOnServiceDate;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.DatedServiceJourneyHelper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.routing.stoptimes.StopTimesHelper;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.TransitEntity;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Default implementation of the Transit Service and Transit Editor Service.
 * A new instance of this class should be created for each request.
 * This ensures that the same TimetableSnapshot is used for the
 * duration of the request (which may involve several method calls).
 */
public class DefaultTransitService implements TransitEditorService {

  private final Graph graph;

  private final GraphIndex graphIndex;

  /**
   * This should only be accessed through the getTimetableSnapshot method.
   */
  private TimetableSnapshot timetableSnapshot;

  public DefaultTransitService(Graph graph) {
    this.graph = graph;
    this.graphIndex = graph.index;
  }

  /** {@link Graph#getFeedIds()} */
  @Override
  public Collection<String> getFeedIds() {
    return this.graph.getFeedIds();
  }

  /** {@link Graph#getAgencies()} */
  @Override
  public Collection<Agency> getAgencies() {
    return this.graph.getAgencies();
  }

  /** {@link Graph#getFeedInfo(String)} ()} */
  @Override
  public FeedInfo getFeedInfo(String feedId) {
    return this.graph.getFeedInfo(feedId);
  }

  /** {@link Graph#addAgency(String, Agency)} */
  @Override
  public void addAgency(String feedId, Agency agency) {
    this.graph.addAgency(feedId, agency);
  }

  /** {@link Graph#addFeedInfo(FeedInfo)} */
  @Override
  public void addFeedInfo(FeedInfo info) {
    this.graph.addFeedInfo(info);
  }

  /** {@link Graph#getOperators()} */
  @Override
  public Collection<Operator> getOperators() {
    return this.graph.getOperators();
  }

  /** {@link Graph#getNoticesByElement()} */
  @Override
  public Multimap<TransitEntity, Notice> getNoticesByElement() {
    return this.graph.getNoticesByElement();
  }

  /** {@link Graph#addNoticeAssignments(Multimap)} */
  @Override
  public void addNoticeAssignments(Multimap<TransitEntity, Notice> noticesByElement) {
    this.graph.addNoticeAssignments(noticesByElement);
  }

  /** {@link Graph#getNoticesByEntity(TransitEntity)} */
  @Override
  public Collection<Notice> getNoticesByEntity(TransitEntity entity) {
    return this.graph.getNoticesByEntity(entity);
  }

  /** {@link Graph#getTripPatternForId(FeedScopedId)} */
  @Override
  public TripPattern getTripPatternForId(FeedScopedId id) {
    return this.graph.getTripPatternForId(id);
  }

  /** {@link Graph#getTripPatterns()} */
  @Override
  public Collection<TripPattern> getTripPatterns() {
    return this.graph.getTripPatterns();
  }

  /** {@link Graph#getNotices()} */
  @Override
  public Collection<Notice> getNotices() {
    return this.graph.getNotices();
  }

  /** {@link Graph#getStopsByBoundingBox(double, double, double, double)} */
  @Override
  public Collection<StopLocation> getStopsByBoundingBox(
    double minLat,
    double minLon,
    double maxLat,
    double maxLon
  ) {
    return this.graph.getStopsByBoundingBox(minLat, minLon, maxLat, maxLon);
  }

  /** {@link Graph#getStopsInRadius(WgsCoordinate, double)} */
  @Override
  public List<T2<Stop, Double>> getStopsInRadius(WgsCoordinate center, double radius) {
    return this.graph.getStopsInRadius(center, radius);
  }

  /** {@link Graph#getStationById(FeedScopedId)} */
  @Override
  public Station getStationById(FeedScopedId id) {
    return this.graph.getStationById(id);
  }

  /** {@link Graph#getMultiModalStation(FeedScopedId)} */
  @Override
  public MultiModalStation getMultiModalStation(FeedScopedId id) {
    return this.graph.getMultiModalStation(id);
  }

  /** {@link Graph#getStations()} */
  @Override
  public Collection<Station> getStations() {
    return this.graph.getStations();
  }

  /** {@link Graph#getServiceCodes()} */
  @Override
  public Map<FeedScopedId, Integer> getServiceCodes() {
    return this.graph.getServiceCodes();
  }

  /** {@link Graph#getLocationById(FeedScopedId)} */
  @Override
  public FlexStopLocation getLocationById(FeedScopedId id) {
    return this.graph.getLocationById(id);
  }

  /** {@link Graph#getAllFlexStopsFlat()} */
  @Override
  public Set<StopLocation> getAllFlexStopsFlat() {
    return this.graph.getAllFlexStopsFlat();
  }

  /** {@link GraphIndex#getAgencyForId(FeedScopedId)} */
  @Override
  public Agency getAgencyForId(FeedScopedId id) {
    return this.graphIndex.getAgencyForId(id);
  }

  /** {@link GraphIndex#getStopForId(FeedScopedId)} */
  @Override
  public StopLocation getStopForId(FeedScopedId id) {
    return this.graphIndex.getStopForId(id);
  }

  /** {@link GraphIndex#getRouteForId(FeedScopedId)} */
  @Override
  public Route getRouteForId(FeedScopedId id) {
    return this.graphIndex.getRouteForId(id);
  }

  /** {@link GraphIndex#addRoutes(Route)} */
  @Override
  public void addRoutes(Route route) {
    this.graphIndex.addRoutes(route);
  }

  /** {@link GraphIndex#getRoutesForStop(StopLocation)} */
  @Override
  public Set<Route> getRoutesForStop(StopLocation stop) {
    return this.graphIndex.getRoutesForStop(stop);
  }

  /** {@link GraphIndex#getPatternsForStop(StopLocation)} */
  @Override
  public Collection<TripPattern> getPatternsForStop(StopLocation stop) {
    return this.graphIndex.getPatternsForStop(stop);
  }

  /** {@link GraphIndex#getPatternsForStop(StopLocation, TimetableSnapshot)} */
  @Override
  public Collection<TripPattern> getPatternsForStop(
    StopLocation stop,
    TimetableSnapshot timetableSnapshot
  ) {
    return this.graphIndex.getPatternsForStop(stop, timetableSnapshot);
  }

  /** {@link GraphIndex#getAllOperators()} */
  @Override
  public Collection<Operator> getAllOperators() {
    return this.graphIndex.getAllOperators();
  }

  /** {@link GraphIndex#getOperatorForId()} */
  @Override
  public Map<FeedScopedId, Operator> getOperatorForId() {
    return this.graphIndex.getOperatorForId();
  }

  /** {@link GraphIndex#getAllStops()} */
  @Override
  public Collection<StopLocation> getAllStops() {
    return this.graphIndex.getAllStops();
  }

  /** {@link GraphIndex#getTripForId()} */
  @Override
  public Map<FeedScopedId, Trip> getTripForId() {
    return this.graphIndex.getTripForId();
  }

  /** {@link GraphIndex#getAllRoutes()} */
  @Override
  public Collection<Route> getAllRoutes() {
    return this.graphIndex.getAllRoutes();
  }

  /** {@link GraphIndex#getPatternForTrip()} */
  @Override
  public Map<Trip, TripPattern> getPatternForTrip() {
    return this.graphIndex.getPatternForTrip();
  }

  /** {@link GraphIndex#getPatternsForFeedId()} */
  @Override
  public Multimap<String, TripPattern> getPatternsForFeedId() {
    return this.graphIndex.getPatternsForFeedId();
  }

  /** {@link GraphIndex#getPatternsForRoute()} */
  @Override
  public Multimap<Route, TripPattern> getPatternsForRoute() {
    return this.graphIndex.getPatternsForRoute();
  }

  /** {@link GraphIndex#getMultiModalStationForStations()} */
  @Override
  public Map<Station, MultiModalStation> getMultiModalStationForStations() {
    return this.graphIndex.getMultiModalStationForStations();
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
   * @param startTime             Start time for the search. Seconds from UNIX epoch
   * @param timeRange             Searches forward for timeRange seconds from startTime
   * @param numberOfDepartures    Number of departures to fetch per pattern
   * @param arrivalDeparture      Filter by arrivals, departures, or both
   * @param includeCancelledTrips If true, cancelled trips will also be included in result.
   */
  @Override
  public List<StopTimesInPattern> stopTimesForStop(
    StopLocation stop,
    long startTime,
    int timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancelledTrips
  ) {
    return StopTimesHelper.stopTimesForStop(
      this,
      lazyGetTimeTableSnapShot(),
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
    ServiceDate serviceDate,
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
   * @param startTime          Start time for the search. Seconds from UNIX epoch
   * @param timeRange          Searches forward for timeRange seconds from startTime
   * @param numberOfDepartures Number of departures to fetch per pattern
   * @param arrivalDeparture   Filter by arrivals, departures, or both
   */
  @Override
  public List<TripTimeOnDate> stopTimesForPatternAtStop(
    StopLocation stop,
    TripPattern pattern,
    long startTime,
    int timeRange,
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture
  ) {
    return StopTimesHelper.stopTimesForPatternAtStop(
      this,
      lazyGetTimeTableSnapShot(),
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
    return graph.index.getPatternsForStop(
      stop,
      includeRealtimeUpdates ? lazyGetTimeTableSnapShot() : null
    );
  }

  /**
   * Get the most up-to-date timetable for the given TripPattern, as of right now. There should
   * probably be a less awkward way to do this that just gets the latest entry from the resolver
   * without making a fake routing request.
   */
  @Override
  public Timetable getTimetableForTripPattern(TripPattern tripPattern, ServiceDate serviceDate) {
    TimetableSnapshot timetableSnapshot = lazyGetTimeTableSnapShot();
    return timetableSnapshot != null
      ? timetableSnapshot.resolve(
        tripPattern,
        serviceDate == null ? new ServiceDate(Calendar.getInstance().getTime()) : serviceDate
      )
      : tripPattern.getScheduledTimetable();
  }

  /**
   * Lazy-initialization of TimetableSnapshot
   *
   * @return The same TimetableSnapshot is returned throughout the lifecycle of this object.
   */
  private TimetableSnapshot lazyGetTimeTableSnapShot() {
    if (this.timetableSnapshot == null) {
      timetableSnapshot = graph.getTimetableSnapshot();
    }
    return this.timetableSnapshot;
  }

  @Override
  public TripOnServiceDate getTripOnServiceDateForTripAndDay(
    FeedScopedId tripId,
    ServiceDate serviceDate
  ) {
    return DatedServiceJourneyHelper.getTripOnServiceDate(this, tripId, serviceDate);
  }

  @Override
  public TripOnServiceDate getTripOnServiceDateById(FeedScopedId datedServiceJourneyId) {
    return DatedServiceJourneyHelper.getTripOnServiceDate(this, datedServiceJourneyId);
  }

  @Override
  public Map<TripIdAndServiceDate, TripOnServiceDate> getTripOnServiceDateForTripAndDay() {
    return graphIndex.getTripOnServiceDateForTripAndDay();
  }

  @Override
  public Map<FeedScopedId, TripOnServiceDate> getTripOnServiceDateById() {
    return graphIndex.getTripOnServiceDateById();
  }

  /** {@link Graph#addTransitMode(TransitMode)} */
  @Override
  public void addTransitMode(TransitMode mode) {
    this.graph.addTransitMode(mode);
  }

  /** {@link Graph#getTransitModes()} */
  @Override
  public HashSet<TransitMode> getTransitModes() {
    return this.graph.getTransitModes();
  }

  /** {@link Graph#getTransfersByStop(StopLocation)} */
  @Override
  public Collection<PathTransfer> getTransfersByStop(StopLocation stop) {
    return this.graph.getTransfersByStop(stop);
  }

  /** {@link Graph#getTimetableSnapshot()} */
  @Override
  public TimetableSnapshot getTimetableSnapshot() {
    return this.graph.getTimetableSnapshot();
  }

  /** {@link Graph#getOrSetupTimetableSnapshotProvider(Function)} */
  @Override
  public <T extends TimetableSnapshotProvider> T getOrSetupTimetableSnapshotProvider(
    Function<Graph, T> creator
  ) {
    return this.graph.getOrSetupTimetableSnapshotProvider(creator);
  }

  /** {@link Graph#getTransitLayer()} */
  @Override
  public TransitLayer getTransitLayer() {
    return this.graph.getTransitLayer();
  }

  /** {@link Graph#setTransitLayer(TransitLayer)} */
  @Override
  public void setTransitLayer(TransitLayer transitLayer) {
    this.graph.setTransitLayer(transitLayer);
  }

  /** {@link Graph#getCalendarService()} */
  @Override
  public CalendarService getCalendarService() {
    return this.graph.getCalendarService();
  }

  /** {@link Graph#getTimeZone()} */
  @Override
  public TimeZone getTimeZone() {
    return this.graph.getTimeZone();
  }
}
