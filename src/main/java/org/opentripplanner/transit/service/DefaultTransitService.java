package org.opentripplanner.transit.service;

import com.google.common.collect.Multimap;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.ext.flex.FlexIndex;
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
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.routing.stoptimes.StopTimesHelper;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.TransitEntity;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
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

  private final TransitModel transitModel;

  private final TransitModelIndex transitModelIndex;

  /**
   * This should only be accessed through the getTimetableSnapshot method.
   */
  private TimetableSnapshot timetableSnapshot;

  public DefaultTransitService(TransitModel transitModel) {
    this.transitModel = transitModel;
    this.transitModelIndex = transitModel.index;
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
  public Multimap<TransitEntity, Notice> getNoticesByElement() {
    return this.transitModel.getNoticesByElement();
  }

  /** {@link TransitModel#addNoticeAssignments(Multimap)} */
  @Override
  public void addNoticeAssignments(Multimap<TransitEntity, Notice> noticesByElement) {
    this.transitModel.addNoticeAssignments(noticesByElement);
  }

  /** {@link TransitModel#getNoticesByEntity(TransitEntity)} */
  @Override
  public Collection<Notice> getNoticesByEntity(TransitEntity entity) {
    return this.transitModel.getNoticesByEntity(entity);
  }

  /** {@link TransitModel#getTripPatternForId(FeedScopedId)} */
  @Override
  public TripPattern getTripPatternForId(FeedScopedId id) {
    return this.transitModel.getTripPatternForId(id);
  }

  /** {@link TransitModel#getTripPatterns()} */
  @Override
  public Collection<TripPattern> getTripPatterns() {
    return this.transitModel.getTripPatterns();
  }

  /** {@link TransitModel#getNotices()} */
  @Override
  public Collection<Notice> getNotices() {
    return this.transitModel.getNotices();
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
  public Map<FeedScopedId, Integer> getServiceCodes() {
    return this.transitModel.getServiceCodes();
  }

  /** {@link StopModel#getLocationById(FeedScopedId)} */
  @Override
  public FlexStopLocation getLocationById(FeedScopedId id) {
    return this.transitModel.getStopModel().getLocationById(id);
  }

  /** {@link TransitModel#getAllFlexStopsFlat()} */
  @Override
  public Set<StopLocation> getAllFlexStopsFlat() {
    return this.transitModel.getAllFlexStopsFlat();
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

  /** {@link TransitModelIndex#getPatternsForStop(StopLocation, TimetableSnapshot)} */
  @Override
  public Collection<TripPattern> getPatternsForStop(
    StopLocation stop,
    TimetableSnapshot timetableSnapshot
  ) {
    return this.transitModelIndex.getPatternsForStop(stop, timetableSnapshot);
  }

  /** {@link TransitModelIndex#getAllOperators()} */
  @Override
  public Collection<Operator> getAllOperators() {
    return this.transitModelIndex.getAllOperators();
  }

  /** {@link TransitModelIndex#getOperatorForId()} */
  @Override
  public Map<FeedScopedId, Operator> getOperatorForId() {
    return this.transitModelIndex.getOperatorForId();
  }

  /** {@link StopModelIndex#getAllStops()} */
  @Override
  public Collection<StopLocation> getAllStops() {
    return this.transitModel.getStopModel().getStopModelIndex().getAllStops();
  }

  /** {@link TransitModelIndex#getTripForId()} */
  @Override
  public Map<FeedScopedId, Trip> getTripForId() {
    return this.transitModelIndex.getTripForId();
  }

  /** {@link TransitModelIndex#getAllRoutes()} */
  @Override
  public Collection<Route> getAllRoutes() {
    return this.transitModelIndex.getAllRoutes();
  }

  /** {@link TransitModelIndex#getPatternForTrip()} */
  @Override
  public Map<Trip, TripPattern> getPatternForTrip() {
    return this.transitModelIndex.getPatternForTrip();
  }

  /** {@link TransitModelIndex#getPatternsForFeedId()} */
  @Override
  public Multimap<String, TripPattern> getPatternsForFeedId() {
    return this.transitModelIndex.getPatternsForFeedId();
  }

  /** {@link TransitModelIndex#getPatternsForRoute()} */
  @Override
  public Multimap<Route, TripPattern> getPatternsForRoute() {
    return this.transitModelIndex.getPatternsForRoute();
  }

  /** {@link StopModelIndex#getMultiModalStationForStations()} */
  @Override
  public Map<Station, MultiModalStation> getMultiModalStationForStations() {
    return this.transitModel.getStopModel().getStopModelIndex().getMultiModalStationForStations();
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
    return transitModel.index.getPatternsForStop(
      stop,
      includeRealtimeUpdates ? lazyGetTimeTableSnapShot() : null
    );
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
      timetableSnapshot = transitModel.getTimetableSnapshot();
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
    return transitModelIndex.getTripOnServiceDateForTripAndDay();
  }

  @Override
  public Map<FeedScopedId, TripOnServiceDate> getTripOnServiceDateById() {
    return transitModelIndex.getTripOnServiceDateById();
  }

  /** {@link TransitModel#addTransitMode(TransitMode)} */
  @Override
  public void addTransitMode(TransitMode mode) {
    this.transitModel.addTransitMode(mode);
  }

  /** {@link TransitModel#getTransitModes()} */
  @Override
  public HashSet<TransitMode> getTransitModes() {
    return this.transitModel.getTransitModes();
  }

  /** {@link TransitModel#getTransfersByStop(StopLocation)} */
  @Override
  public Collection<PathTransfer> getTransfersByStop(StopLocation stop) {
    return this.transitModel.getTransfersByStop(stop);
  }

  /** {@link TransitModel#getTimetableSnapshot()} */
  @Override
  public TimetableSnapshot getTimetableSnapshot() {
    return this.transitModel.getTimetableSnapshot();
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
  public TimeZone getTimeZone() {
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
  public BitSet getServicesRunningForDate(ServiceDate parseString) {
    return transitModel.getServicesRunningForDate(parseString);
  }

  @Override
  public Long getTransitServiceEnds() {
    return transitModel.getTransitServiceEnds();
  }

  @Override
  public Long getTransitServiceStarts() {
    return transitModel.getTransitServiceStarts();
  }

  /** {@link StopModelIndex#getStopVertexForStop()} */
  @Override
  public Map<Stop, TransitStopVertex> getStopVertexForStop() {
    return transitModel.getStopModel().getStopVertexForStop();
  }

  /** {@link StopModelIndex#getStopSpatialIndex()} */

  @Override
  public HashGridSpatialIndex<TransitStopVertex> getStopSpatialIndex() {
    return transitModel.getStopModel().getStopModelIndex().getStopSpatialIndex();
  }
}
