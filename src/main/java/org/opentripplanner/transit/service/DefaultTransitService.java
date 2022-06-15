package org.opentripplanner.transit.service;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitEntity;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.timetable.Trip;

public class DefaultTransitService implements TransitEditorService {

  private final Graph graph;

  private final GraphIndex graphIndex;

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
}
