package org.opentripplanner.routing;

import gnu.trove.set.TIntSet;
import java.io.Serializable;
import java.time.Instant;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource.DrivingDirection;
import org.opentripplanner.model.GraphBundle;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.RoutingWorker;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.services.RealtimeVehiclePositionService;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.util.WorldEnvelope;

/**
 * Entry point for requests towards the routing API.
 */
public class RoutingService {

  private final Graph graph;

  private final GraphIndex graphIndex;

  private final GraphFinder graphFinder;

  public RoutingService(Graph graph) {
    this.graph = graph;
    this.graphIndex = graph.index;
    this.graphFinder = GraphFinder.getInstance(graph);
  }

  // TODO We should probably not have the Router as a parameter here
  public RoutingResponse route(RoutingRequest request, Router router) {
    var zoneId = graph.getTimeZone().toZoneId();
    RoutingWorker worker = new RoutingWorker(router, request, zoneId);
    return worker.route();
  }

  /** {@link Graph#addVertex(Vertex)} */
  public void addVertex(Vertex v) {
    this.graph.addVertex(v);
  }

  /** {@link Graph#removeEdge(Edge)} */
  public void removeEdge(Edge e) {
    this.graph.removeEdge(e);
  }

  /** {@link Graph#getVertex(String)} */
  public Vertex getVertex(String label) {
    return this.graph.getVertex(label);
  }

  /** {@link Graph#getVertices()} */
  public Collection<Vertex> getVertices() {
    return this.graph.getVertices();
  }

  /** {@link Graph#getVerticesOfType(Class)} */
  public <T extends Vertex> List<T> getVerticesOfType(Class<T> cls) {
    return this.graph.getVerticesOfType(cls);
  }

  /** {@link Graph#getEdges()} */
  public Collection<Edge> getEdges() {
    return this.graph.getEdges();
  }

  /** {@link Graph#getEdgesOfType(Class)} */
  public <T extends Edge> List<T> getEdgesOfType(Class<T> cls) {
    return this.graph.getEdgesOfType(cls);
  }

  /** {@link Graph#getStreetEdges()} */
  public Collection<StreetEdge> getStreetEdges() {
    return this.graph.getStreetEdges();
  }

  /** {@link Graph#getRealtimeTransitLayer()} */
  public TransitLayer getRealtimeTransitLayer() {
    return this.graph.getRealtimeTransitLayer();
  }

  /** {@link Graph#setRealtimeTransitLayer(TransitLayer)} */
  public void setRealtimeTransitLayer(TransitLayer realtimeTransitLayer) {
    this.graph.setRealtimeTransitLayer(realtimeTransitLayer);
  }

  /** {@link Graph#hasRealtimeTransitLayer()} */
  public boolean hasRealtimeTransitLayer() {
    return this.graph.hasRealtimeTransitLayer();
  }

  /** {@link Graph#containsVertex(Vertex)} */
  public boolean containsVertex(Vertex v) {
    return this.graph.containsVertex(v);
  }

  /** {@link Graph#putService(Class, Serializable)} */
  public <T extends Serializable> T putService(Class<T> serviceType, T service) {
    return this.graph.putService(serviceType, service);
  }

  /** {@link Graph#hasService(Class)} */
  public boolean hasService(Class<? extends Serializable> serviceType) {
    return this.graph.hasService(serviceType);
  }

  /** {@link Graph#getService(Class)} */
  public <T extends Serializable> T getService(Class<T> serviceType) {
    return this.graph.getService(serviceType);
  }

  /** {@link Graph#getService(Class, boolean)} */
  public <T extends Serializable> T getService(Class<T> serviceType, boolean autoCreate) {
    return this.graph.getService(serviceType, autoCreate);
  }

  /** {@link Graph#remove(Vertex)} */
  public void remove(Vertex vertex) {
    this.graph.remove(vertex);
  }

  /** {@link Graph#removeIfUnconnected(Vertex)} */
  public void removeIfUnconnected(Vertex v) {
    this.graph.removeIfUnconnected(v);
  }

  /** {@link Graph#getExtent()} */
  public Envelope getExtent() {
    return this.graph.getExtent();
  }

  /** {@link Graph#getTransferService()} */
  public TransferService getTransferService() {
    return this.graph.getTransferService();
  }

  /** {@link Graph#updateTransitFeedValidity(CalendarServiceData, DataImportIssueStore)} */
  public void updateTransitFeedValidity(CalendarServiceData data, DataImportIssueStore issueStore) {
    this.graph.updateTransitFeedValidity(data, issueStore);
  }

  /** {@link Graph#transitFeedCovers(Instant)} */
  public boolean transitFeedCovers(Instant time) {
    return this.graph.transitFeedCovers(time);
  }

  /** {@link Graph#getBundle()} */
  public GraphBundle getBundle() {
    return this.graph.getBundle();
  }

  /** {@link Graph#setBundle(GraphBundle)} */
  public void setBundle(GraphBundle bundle) {
    this.graph.setBundle(bundle);
  }

  /** {@link Graph#countVertices()} */
  public int countVertices() {
    return this.graph.countVertices();
  }

  /** {@link Graph#countEdges()} */
  public int countEdges() {
    return this.graph.countEdges();
  }

  // /** {@link Graph#index()} */
  // public void index() {this.graph.index();}

  /** {@link Graph#getCalendarDataService()} */
  public CalendarServiceData getCalendarDataService() {
    return this.graph.getCalendarDataService();
  }

  /** {@link Graph#clearCachedCalenderService()} */
  public void clearCachedCalenderService() {
    this.graph.clearCachedCalenderService();
  }

  /** {@link Graph#getStreetIndex()} */
  public StreetVertexIndex getStreetIndex() {
    return this.graph.getStreetIndex();
  }

  /** {@link Graph#getLinker()} */
  public VertexLinker getLinker() {
    return this.graph.getLinker();
  }

  /** {@link Graph#getOrCreateServiceIdForDate(ServiceDate)} */
  public FeedScopedId getOrCreateServiceIdForDate(ServiceDate serviceDate) {
    return this.graph.getOrCreateServiceIdForDate(serviceDate);
  }

  /** {@link Graph#removeEdgelessVertices()} */
  public int removeEdgelessVertices() {
    return this.graph.removeEdgelessVertices();
  }

  /** {@link Graph#getTimeZone()} */
  public TimeZone getTimeZone() {
    return this.graph.getTimeZone();
  }

  /** {@link Graph#clearTimeZone()} */
  public void clearTimeZone() {
    this.graph.clearTimeZone();
  }

  /** {@link Graph#calculateEnvelope()} */
  public void calculateEnvelope() {
    this.graph.calculateEnvelope();
  }

  /** {@link Graph#calculateConvexHull()} */
  public void calculateConvexHull() {
    this.graph.calculateConvexHull();
  }

  /** {@link Graph#getConvexHull()} */
  public Geometry getConvexHull() {
    return this.graph.getConvexHull();
  }

  /** {@link Graph#expandToInclude(double, double)} ()} */
  public void expandToInclude(double x, double y) {
    this.graph.expandToInclude(x, y);
  }

  /** {@link Graph#getEnvelope()} */
  public WorldEnvelope getEnvelope() {
    return this.graph.getEnvelope();
  }

  /** {@link Graph#calculateTransitCenter()} */
  public void calculateTransitCenter() {
    this.graph.calculateTransitCenter();
  }

  /** {@link Graph#getCenter()} */
  public Optional<Coordinate> getCenter() {
    return this.graph.getCenter();
  }

  /** {@link Graph#getTransitServiceStarts()} */
  public long getTransitServiceStarts() {
    return this.graph.getTransitServiceStarts();
  }

  /** {@link Graph#getTransitServiceEnds()} */
  public long getTransitServiceEnds() {
    return this.graph.getTransitServiceEnds();
  }

  /** {@link Graph#getDistanceBetweenElevationSamples()} */
  public double getDistanceBetweenElevationSamples() {
    return this.graph.getDistanceBetweenElevationSamples();
  }

  /** {@link Graph#setDistanceBetweenElevationSamples(double)} */
  public void setDistanceBetweenElevationSamples(double distanceBetweenElevationSamples) {
    this.graph.setDistanceBetweenElevationSamples(distanceBetweenElevationSamples);
  }

  /** {@link Graph#getTransitAlertService()} */
  public TransitAlertService getTransitAlertService() {
    return this.graph.getTransitAlertService();
  }

  public RealtimeVehiclePositionService getVehiclePositionService() {
    return this.graph.getVehiclePositionService();
  }

  /** {@link Graph#getStopVerticesById(FeedScopedId)} */
  public Set<Vertex> getStopVerticesById(FeedScopedId id) {
    return this.graph.getStopVerticesById(id);
  }

  /** {@link Graph#getServicesRunningForDate(ServiceDate)} */
  public BitSet getServicesRunningForDate(ServiceDate date) {
    return this.graph.getServicesRunningForDate(date);
  }

  /** {@link Graph#getVehicleRentalStationService()} */
  public VehicleRentalStationService getVehicleRentalStationService() {
    return this.graph.getVehicleRentalStationService();
  }

  /** {@link Graph#getVehicleParkingService()} */
  public VehicleParkingService getVehicleParkingService() {
    return this.graph.getVehicleParkingService();
  }

  /** {@link Graph#getDrivingDirection()} */
  public DrivingDirection getDrivingDirection() {
    return this.graph.getDrivingDirection();
  }

  /** {@link Graph#setDrivingDirection(DrivingDirection)} */
  public void setDrivingDirection(DrivingDirection drivingDirection) {
    this.graph.setDrivingDirection(drivingDirection);
  }

  /** {@link Graph#getIntersectionTraversalModel()} */
  public IntersectionTraversalCostModel getIntersectionTraversalModel() {
    return this.graph.getIntersectionTraversalModel();
  }

  /** {@link Graph#setIntersectionTraversalCostModel(IntersectionTraversalCostModel)} */
  public void setIntersectionTraversalCostModel(
    IntersectionTraversalCostModel intersectionTraversalCostModel
  ) {
    this.graph.setIntersectionTraversalCostModel(intersectionTraversalCostModel);
  }

  /** {@link GraphIndex#getStopVertexForStop()} */
  public Map<Stop, TransitStopVertex> getStopVertexForStop() {
    return this.graphIndex.getStopVertexForStop();
  }

  /** {@link GraphIndex#getStopSpatialIndex()} */
  public HashGridSpatialIndex<TransitStopVertex> getStopSpatialIndex() {
    return this.graphIndex.getStopSpatialIndex();
  }

  /** {@link GraphIndex#getServiceCodesRunningForDate()} */
  public Map<ServiceDate, TIntSet> getServiceCodesRunningForDate() {
    return this.graphIndex.getServiceCodesRunningForDate();
  }

  /** {@link GraphIndex#getFlexIndex()} */
  public FlexIndex getFlexIndex() {
    return this.graphIndex.getFlexIndex();
  }

  /** {@link GraphFinder#findClosestStops(double, double, double)} */
  public List<NearbyStop> findClosestStops(double lat, double lon, double radiusMeters) {
    return this.graphFinder.findClosestStops(lat, lon, radiusMeters);
  }

  /**
   * {@link GraphFinder#findClosestPlaces(double, double, double, int, List, List, List, List, List,
   * RoutingService, TransitService)}
   */
  public List<PlaceAtDistance> findClosestPlaces(
    double lat,
    double lon,
    double radiusMeters,
    int maxResults,
    List<TransitMode> filterByModes,
    List<PlaceType> filterByPlaceTypes,
    List<FeedScopedId> filterByStops,
    List<FeedScopedId> filterByRoutes,
    List<String> filterByBikeRentalStations,
    List<String> filterByBikeParks,
    List<String> filterByCarParks,
    RoutingService routingService,
    TransitService transitService
  ) {
    return this.graphFinder.findClosestPlaces(
        lat,
        lon,
        radiusMeters,
        maxResults,
        filterByModes,
        filterByPlaceTypes,
        filterByStops,
        filterByRoutes,
        filterByBikeRentalStations,
        routingService,
        transitService
      );
  }
}
