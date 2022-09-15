package org.opentripplanner.routing;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource.DrivingDirection;
import org.opentripplanner.routing.algorithm.RoutingWorker;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.request.RouteViaRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.services.RealtimeVehiclePositionService;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.util.WorldEnvelope;

// TODO VIA: 2022-08-29 javadocs
/**
 * Entry point for requests towards the routing API.
 */
public class RoutingService implements org.opentripplanner.routing.api.request.RoutingService {

  private final OtpServerRequestContext serverContext;
  private final Graph graph;

  private final ZoneId timeZone;

  private final GraphFinder graphFinder;

  public RoutingService(OtpServerRequestContext serverContext) {
    this.serverContext = serverContext;
    this.graph = serverContext.graph();
    this.timeZone = serverContext.transitService().getTimeZone();
    this.graphFinder = serverContext.graphFinder();
  }

  @Override
  public RoutingResponse route(RouteRequest request) {
    RoutingWorker worker = new RoutingWorker(serverContext, request, timeZone);
    return worker.route();
  }

  @Override
  public RoutingResponse route(RouteViaRequest request, RoutingPreferences preferences) {
    throw new RuntimeException("Not implemented");
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

  /** {@link Graph#containsVertex(Vertex)} */
  public boolean containsVertex(Vertex v) {
    return this.graph.containsVertex(v);
  }

  /** {@link Graph#getExtent()} */
  public Envelope getExtent() {
    return this.graph.getExtent();
  }

  /** {@link Graph#countVertices()} */
  public int countVertices() {
    return this.graph.countVertices();
  }

  /** {@link Graph#countEdges()} */
  public int countEdges() {
    return this.graph.countEdges();
  }

  /** {@link Graph#getStreetIndex()} */
  public StreetVertexIndex getStreetIndex() {
    return this.graph.getStreetIndex();
  }

  /** {@link Graph#getConvexHull()} */
  public Geometry getConvexHull() {
    return this.graph.getConvexHull();
  }

  /** {@link Graph#getEnvelope()} */
  public WorldEnvelope getEnvelope() {
    return this.graph.getEnvelope();
  }

  /** {@link Graph#getDistanceBetweenElevationSamples()} */
  public double getDistanceBetweenElevationSamples() {
    return this.graph.getDistanceBetweenElevationSamples();
  }

  public RealtimeVehiclePositionService getVehiclePositionService() {
    return this.graph.getVehiclePositionService();
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

  /** {@link Graph#getIntersectionTraversalModel()} */
  public IntersectionTraversalCostModel getIntersectionTraversalModel() {
    return this.graph.getIntersectionTraversalModel();
  }

  /** {@link GraphFinder#findClosestStops(double, double, double)} */
  public List<NearbyStop> findClosestStops(double lat, double lon, double radiusMeters) {
    return this.graphFinder.findClosestStops(lat, lon, radiusMeters);
  }

  /**
   * {@link GraphFinder#findClosestPlaces(double, double, double, int, List, List, List, List, List, TransitService)}
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
        transitService
      );
  }
}
