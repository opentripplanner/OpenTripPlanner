package org.opentripplanner.ext.carpooling.updater;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.PathComparator;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.EstimatedCall;
import uk.org.siri.siri21.EstimatedVehicleJourney;

public class CarpoolSiriMapper {

  private static final Logger LOG = LoggerFactory.getLogger(CarpoolSiriMapper.class);
  private static final String FEED_ID = "ENT";
  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
  private static final AtomicInteger COUNTER = new AtomicInteger(0);
  // Average driving speed in m/s (50 km/h = ~14 m/s) - conservative estimate for urban driving
  private static final double DEFAULT_DRIVING_SPEED_MS = 13.89;
  // Maximum duration for route calculation (10 minutes)
  private static final Duration MAX_ROUTE_DURATION = Duration.ofMinutes(10);

  private final Graph graph;
  private final VertexLinker vertexLinker;
  private final StreetLimitationParametersService streetLimitationParametersService;

  public CarpoolSiriMapper(
    Graph graph,
    VertexLinker vertexLinker,
    StreetLimitationParametersService streetLimitationParametersService
  ) {
    this.graph = graph;
    this.vertexLinker = vertexLinker;
    this.streetLimitationParametersService = streetLimitationParametersService;
  }

  public CarpoolTrip mapSiriToCarpoolTrip(EstimatedVehicleJourney journey) {
    var calls = journey.getEstimatedCalls().getEstimatedCalls();
    if (calls.size() != 2) {
      throw new IllegalArgumentException("Carpool trips must have exactly 2 stops for now.");
    }

    var boardingCall = calls.getFirst();
    var alightingCall = calls.getLast();

    String lineRef = journey.getLineRef().getValue();
    String tripId = lineRef.substring(lineRef.lastIndexOf(':') + 1);

    AreaStop boardingArea = buildAreaStop(boardingCall, tripId + "_boarding");
    AreaStop alightingArea = buildAreaStop(alightingCall, tripId + "_alighting");

    ZonedDateTime startTime = boardingCall.getExpectedDepartureTime() != null
      ? boardingCall.getExpectedDepartureTime()
      : boardingCall.getAimedDepartureTime();

    // Use provided end time if available, otherwise estimate based on drive time
    ZonedDateTime endTime = alightingCall.getExpectedArrivalTime() != null
      ? alightingCall.getExpectedArrivalTime()
      : alightingCall.getAimedArrivalTime();

    var scheduledDuration = Duration.between(startTime, endTime);

    // Calculate estimated drive time between stops for deviation budget
    var estimatedDriveTime = calculateDriveTimeWithRouting(boardingArea, alightingArea);

    var deviationBudget = scheduledDuration.minus(estimatedDriveTime);

    String provider = journey.getOperatorRef().getValue();

    return new CarpoolTripBuilder(new FeedScopedId(FEED_ID, tripId))
      .withBoardingArea(boardingArea)
      .withAlightingArea(alightingArea)
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withProvider(provider)
      .withDeviationBudget(deviationBudget)
      .withAvailableSeats(1) // Default value, could be enhanced if data available
      .build();
  }

  /**
   * Calculate the estimated drive time between two area stops using A* routing.
   * Falls back to straight-line distance estimation if routing fails.
   *
   * @param boardingArea the boarding area stop
   * @param alightingArea the alighting area stop
   * @return the estimated drive time as a Duration
   */
  private Duration calculateDriveTimeWithRouting(AreaStop boardingArea, AreaStop alightingArea) {
    try {
      var tempVertices = new TemporaryVerticesContainer(
        graph,
        vertexLinker,
        null,
        GenericLocation.fromCoordinate(boardingArea.getLat(), boardingArea.getLon()),
        GenericLocation.fromCoordinate(alightingArea.getLat(), alightingArea.getLon()),
        StreetMode.CAR,
        StreetMode.CAR
      );

      // Perform A* routing
      GraphPath<State, Edge, Vertex> path = performCarpoolRouting(
        tempVertices.getFromVertices(),
        tempVertices.getToVertices()
      );

      if (path != null) {
        // Get duration from the path
        long durationSeconds = path.getDuration();
        return Duration.ofSeconds(durationSeconds);
      } else {
        LOG.debug("No route found between carpool stops, using straight-line estimate");
        return calculateDriveTimeFromDistance(boardingArea, alightingArea);
      }
    } catch (Exception e) {
      LOG.error("Error calculating drive time with routing, falling back to distance estimate", e);
      return calculateDriveTimeFromDistance(boardingArea, alightingArea);
    }
  }

  /**
   * Performs A* street routing between two vertices using CAR mode.
   * Returns the routing result with distance, time, and geometry.
   */
  @Nullable
  private GraphPath<State, Edge, Vertex> performCarpoolRouting(Set<Vertex> from, Set<Vertex> to) {
    try {
      // Create a basic route request for car routing
      RouteRequest request = RouteRequest.defaultValue();

      // Set up street request for CAR mode
      StreetRequest streetRequest = new StreetRequest(StreetMode.CAR);

      float maxCarSpeed = streetLimitationParametersService.getMaxCarSpeed();

      var streetSearch = StreetSearchBuilder.of()
        .setHeuristic(new EuclideanRemainingWeightHeuristic(maxCarSpeed))
        .setSkipEdgeStrategy(new DurationSkipEdgeStrategy<>(MAX_ROUTE_DURATION))
        .setDominanceFunction(new DominanceFunctions.MinimumWeight())
        .setRequest(request)
        .setStreetRequest(streetRequest)
        .setFrom(from)
        .setTo(to);

      List<GraphPath<State, Edge, Vertex>> paths = streetSearch.getPathsToTarget();

      if (paths.isEmpty()) {
        return null;
      }

      paths.sort(new PathComparator(request.arriveBy()));
      return paths.getFirst();
    } catch (Exception e) {
      LOG.error("Error performing carpool routing", e);
      return null;
    }
  }

  /**
   * Calculate the estimated drive time based on straight-line distance.
   * Used as a fallback when A* routing is not available.
   *
   * @param boardingArea the boarding area stop
   * @param alightingArea the alighting area stop
   * @return the estimated drive time as a Duration
   */
  private Duration calculateDriveTimeFromDistance(AreaStop boardingArea, AreaStop alightingArea) {
    double distanceInMeters = calculateDistance(boardingArea, alightingArea);

    // Add a buffer factor for traffic, stops, etc (30% additional time for straight-line)
    double adjustedDistanceInMeters = distanceInMeters * 1.3;

    // Calculate time in seconds
    double timeInSeconds = adjustedDistanceInMeters / DEFAULT_DRIVING_SPEED_MS;

    // Round up to nearest minute for more realistic estimates
    long timeInMinutes = (long) Math.ceil(timeInSeconds / 60.0);

    return Duration.ofMinutes(timeInMinutes);
  }

  /**
   * Calculate the straight-line distance between two area stops using their centroids.
   *
   * @param boardingArea the boarding area stop
   * @param alightingArea the alighting area stop
   * @return the distance in meters
   */
  private double calculateDistance(AreaStop boardingArea, AreaStop alightingArea) {
    var boardingCoord = boardingArea.getCoordinate();
    var alightingCoord = alightingArea.getCoordinate();

    // Convert WgsCoordinate to JTS Coordinate for SphericalDistanceLibrary
    Coordinate from = new Coordinate(boardingCoord.longitude(), boardingCoord.latitude());
    Coordinate to = new Coordinate(alightingCoord.longitude(), alightingCoord.latitude());

    return SphericalDistanceLibrary.distance(from, to);
  }

  private AreaStop buildAreaStop(EstimatedCall call, String id) {
    var stopAssignments = call.getDepartureStopAssignments();
    if (stopAssignments == null || stopAssignments.size() != 1) {
      throw new IllegalArgumentException("Expected exactly one stop assignment for call: " + call);
    }
    var flexibleArea = stopAssignments.getFirst().getExpectedFlexibleArea();

    if (flexibleArea == null || flexibleArea.getPolygon() == null) {
      throw new IllegalArgumentException("Missing flexible area for stop");
    }

    var polygon = createPolygonFromGml(flexibleArea.getPolygon());

    return AreaStop.of(new FeedScopedId(FEED_ID, id), COUNTER::getAndIncrement)
      .withName(I18NString.of(call.getStopPointNames().getFirst().getValue()))
      .withGeometry(polygon)
      .build();
  }

  private Polygon createPolygonFromGml(net.opengis.gml._3.PolygonType gmlPolygon) {
    var abstractRing = gmlPolygon.getExterior().getAbstractRing().getValue();

    if (!(abstractRing instanceof net.opengis.gml._3.LinearRingType linearRing)) {
      throw new IllegalArgumentException("Expected LinearRingType for polygon exterior");
    }

    List<Double> values = linearRing.getPosList().getValue();

    // Convert to JTS coordinates (lon lat pairs)
    Coordinate[] coords = new Coordinate[values.size() / 2];
    for (int i = 0; i < values.size(); i += 2) {
      coords[i / 2] = new Coordinate(values.get(i), values.get(i + 1));
    }

    LinearRing shell = GEOMETRY_FACTORY.createLinearRing(coords);
    return GEOMETRY_FACTORY.createPolygon(shell);
  }
}
