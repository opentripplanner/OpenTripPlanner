package org.opentripplanner.ext.carpooling.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.model.CarpoolTransitLeg;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.transit.model.site.AreaStop;

public class DefaultCarpoolingService implements CarpoolingService {

  private final CarpoolingRepository repository;
  private final Graph graph;

  public DefaultCarpoolingService(CarpoolingRepository repository, Graph graph) {
    this.repository = repository;
    this.graph = graph;
  }

  /**
   * TERMINOLOGY
   * - Boarding and alighting area stops
   *
   *
   * ALGORITHM OUTLINE
   *
   * <pre>
   *   DIRECT_DISTANCE = SphericalDistanceLibrary.fastDistance(fromLocation, toLocation)
   *   // 3000m is about 45 minutes of walking
   *   MAX_WALK_DISTANCE = max(DIRECT_DISTANCE, 3000m)
   *   MAX_COST = MAX_WALK_DISTANCE * walkReluctance + DIRECT_DISTANCE - MAX_WALK_DISTANCE
   *
   * Search for access / egress candidates (AreaStops) using
   * - accessDistance = SphericalDistanceLibrary.fastDistance(fromLocation, stop.center);
   * - Drop candidates where accessDistance greater then MAX_WALK_DISTANCE and is not within time constraints
   * - egressDistance = SphericalDistanceLibrary.fastDistance(toLocation, stop.center);
   * - Drop candidates where (accessDistance + egressDistance) greater then MAX_WALK_DISTANCE (no time check)
   * - Sort candidates on estimated cost, where we use direct distance instead of actual distance
   *
   * FOR EACH CANDIDATE (C)
   * - Use AStar to find the actual distance for:
   *   - access path
   *   - transit path
   *   - egress path
   * - Drop candidates where (access+carpool+egress) cost > MAX_COST
   * [- Abort when no more optimal results can be obtained (pri2)]
   *
   * Create Itineraries for the top 3 results and return
   * </pre>
   */
  @Override
  public List<Itinerary> route(RouteRequest request) throws RoutingValidationException {
    if (
      Objects.requireNonNull(request.from()).lat == null ||
      Objects.requireNonNull(request.from()).lng == null
    ) {
      throw new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, InputField.FROM_PLACE))
      );
    }
    if (
      Objects.requireNonNull(request.to()).lat == null ||
      Objects.requireNonNull(request.to()).lng == null
    ) {
      throw new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, InputField.TO_PLACE))
      );
    }

    double fromLat = request.from().lat;
    double fromLng = request.from().lng;
    double toLat = request.to().lat;
    double toLng = request.to().lng;

    // Calculate direct distance and maximum walk distance
    double directDistance = SphericalDistanceLibrary.fastDistance(fromLat, fromLng, toLat, toLng);

    // 3000m is about 45 minutes of walking
    double maxWalkDistance = Math.max(directDistance, 3000.0);
    double walkReluctance = request.preferences().walk().reluctance();
    double maxCost = maxWalkDistance * walkReluctance + directDistance - maxWalkDistance;

    // Find candidate area stops (carpooling boarding/alighting points)
    Collection<CarpoolTrip> availableTrips = repository.getCarpoolTrips();
    List<CarpoolTripCandidate> candidates = new ArrayList<>();

    for (CarpoolTrip trip : availableTrips) {
      AreaStop boardingArea = trip.getBoardingArea();
      AreaStop alightingArea = trip.getAlightingArea();

      if (boardingArea != null && alightingArea != null) {
        // Calculate access distance from origin to boarding area
        double accessDistance = SphericalDistanceLibrary.fastDistance(
          fromLat,
          fromLng,
          boardingArea.getCoordinate().latitude(),
          boardingArea.getCoordinate().longitude()
        );

        // Drop candidates where access distance is too far
        if (accessDistance > maxWalkDistance) {
          continue;
        }

        // Calculate egress distance from alighting area to destination
        double egressDistance = SphericalDistanceLibrary.fastDistance(
          alightingArea.getCoordinate().latitude(),
          alightingArea.getCoordinate().longitude(),
          toLat,
          toLng
        );

        // Drop candidates where total walking distance is too far
        if ((accessDistance + egressDistance) > maxWalkDistance) {
          continue;
        }

        // Calculate estimated cost using direct distances
        double estimatedCost =
          accessDistance * walkReluctance + directDistance + egressDistance * walkReluctance;

        candidates.add(
          new CarpoolTripCandidate(trip, accessDistance, egressDistance, estimatedCost)
        );
      }
    }

    // Sort candidates by estimated cost
    candidates.sort(Comparator.comparingDouble(a -> a.estimatedCost));

    // Perform A* routing for the top candidates and create itineraries
    List<Itinerary> itineraries = new ArrayList<>();
    int maxResults = Math.min(candidates.size(), 3);

    for (int i = 0; i < maxResults; i++) {
      CarpoolTripCandidate candidate = candidates.get(i);

      // Perform A* routing for all three segments
      CarpoolRouting routing = performCarpoolRouting(candidate.trip, request);

      // Only create itinerary if all routing segments succeeded
      if (routing.isComplete()) {
        Itinerary itinerary = createItineraryFromRouting(candidate.trip, routing);
        if (itinerary != null) {
          itineraries.add(itinerary);
        }
      }
    }

    return itineraries;
  }

  /**
   * Performs A* routing for all three segments of a carpool trip:
   * 1. Access path (walking from origin to pickup)
   * 2. Carpool path (driving from pickup to dropoff)
   * 3. Egress path (walking from dropoff to destination)
   */
  private CarpoolRouting performCarpoolRouting(CarpoolTrip trip, RouteRequest request) {
    Coordinate origin = request.from().getCoordinate();
    Coordinate destination = request.to().getCoordinate();
    Coordinate pickup = trip.getBoardingArea().getCoordinate().asJtsCoordinate();
    Coordinate dropoff = trip.getAlightingArea().getCoordinate().asJtsCoordinate();

    // 1. Access path: Walk from origin to pickup
    GraphPath<State, Edge, Vertex> accessPath = routeBetweenCoordinates(
      origin,
      pickup,
      StreetMode.WALK,
      request
    );

    // 2. Carpool path: Drive from pickup to dropoff
    GraphPath<State, Edge, Vertex> carpoolPath = routeBetweenCoordinates(
      pickup,
      dropoff,
      StreetMode.CAR,
      request
    );

    // 3. Egress path: Walk from dropoff to destination
    GraphPath<State, Edge, Vertex> egressPath = routeBetweenCoordinates(
      dropoff,
      destination,
      StreetMode.WALK,
      request
    );

    return new CarpoolRouting(accessPath, carpoolPath, egressPath);
  }

  /**
   * Creates a vertex from the given coordinate using the graph's vertex linker.
   * This reuses OTP's existing vertex resolution functionality.
   */
  private Vertex createVertexFromCoordinate(Coordinate coordinate, StreetMode streetMode) {
    GenericLocation location = GenericLocation.fromCoordinate(coordinate.y, coordinate.x);

    try (
      TemporaryVerticesContainer container = new TemporaryVerticesContainer(
        graph,
        location,
        location,
        streetMode,
        streetMode
      )
    ) {
      // For origin vertices, use fromVertices; for destination vertices, use toVertices
      // Since we're creating individual vertices, we can use either set
      return container.getFromVertices().iterator().next();
    }
  }

  /**
   * Performs A* street routing between two coordinates using the specified street mode.
   * Returns the routing result with distance, time, and geometry.
   */
  @Nullable
  private GraphPath<State, Edge, Vertex> routeBetweenCoordinates(
    Coordinate from,
    Coordinate to,
    StreetMode streetMode,
    RouteRequest request
  ) {
    try {
      Vertex fromVertex = createVertexFromCoordinate(from, streetMode);
      Vertex toVertex = createVertexFromCoordinate(to, streetMode);

      return StreetSearchBuilder.of()
        .setHeuristic(new EuclideanRemainingWeightHeuristic())
        .setRequest(request)
        .setStreetRequest(new StreetRequest(streetMode))
        .setFrom(fromVertex)
        .setTo(toVertex)
        .getPathsToTarget()
        .stream()
        .findFirst()
        .orElse(null);
    } catch (Exception e) {
      // If A* routing fails (e.g., WALKING_BETTER_THAN_TRANSIT), return null
      // This allows the service to gracefully handle problematic coordinates
      return null;
    }
  }

  /**
   * Creates a complete itinerary from A* routing results with proper walking and carpool legs
   */
  private Itinerary createItineraryFromRouting(CarpoolTrip trip, CarpoolRouting routing) {
    List<Leg> legs = new ArrayList<>();

    // 1. Access walking leg (origin to pickup)
    if (routing.accessPath != null) {
      Leg accessLeg = createWalkingLegFromPath(routing.accessPath, "Walk to pickup");
      if (accessLeg != null) {
        legs.add(accessLeg);
      }
    }

    // 2. Carpool transit leg (pickup to dropoff)
    CarpoolTransitLeg carpoolLeg = CarpoolTransitLeg.of()
      .withStartTime(trip.getStartTime())
      .withEndTime(trip.getEndTime())
      .withTrip(trip.getTrip())
      .withGeneralizedCost((int) routing.totalCost)
      .build();
    legs.add(carpoolLeg);

    // 3. Egress walking leg (dropoff to destination)
    if (routing.egressPath != null) {
      Leg egressLeg = createWalkingLegFromPath(routing.egressPath, "Walk from dropoff");
      if (egressLeg != null) {
        legs.add(egressLeg);
      }
    }

    return Itinerary.ofDirect(legs)
      .withGeneralizedCost(Cost.costOfSeconds((int) routing.totalCost))
      .build();
  }

  /**
   * Creates a walking leg from a GraphPath with proper geometry and timing.
   * This reuses the same pattern as OTP's GraphPathToItineraryMapper but simplified
   * for carpooling service use.
   */
  @Nullable
  private Leg createWalkingLegFromPath(GraphPath<State, Edge, Vertex> path, String name) {
    if (path == null || path.states.isEmpty()) {
      return null;
    }

    List<State> states = path.states;
    State firstState = states.getFirst();
    State lastState = states.getLast();

    // Extract edges (skip first state which doesn't have a back edge)
    List<Edge> edges = states
      .stream()
      .skip(1)
      .map(State::getBackEdge)
      .filter(Objects::nonNull)
      .toList();

    if (edges.isEmpty()) {
      return null;
    }

    // Calculate total distance
    double distanceMeters = edges.stream().mapToDouble(Edge::getDistanceMeters).sum();

    // Create geometry from edges
    LineString geometry = GeometryUtils.concatenateLineStrings(edges, Edge::getGeometry);

    // Build the walking leg
    return StreetLeg.of()
      .withMode(TraverseMode.WALK)
      .withStartTime(firstState.getTime().atZone(java.time.ZoneId.systemDefault()))
      .withEndTime(lastState.getTime().atZone(java.time.ZoneId.systemDefault()))
      .withFrom(createPlaceFromState(firstState, name + " start"))
      .withTo(createPlaceFromState(lastState, name + " end"))
      .withDistanceMeters(distanceMeters)
      .withGeneralizedCost((int) (lastState.getWeight() - firstState.getWeight()))
      .withGeometry(geometry)
      .withWalkSteps(List.of()) // Simplified - no detailed walk steps for now
      .build();
  }

  /**
   * Creates a Place from a State, similar to GraphPathToItineraryMapper.makePlace
   * but simplified for carpooling service use.
   */
  private Place createPlaceFromState(State state, String defaultName) {
    Vertex vertex = state.getVertex();
    I18NString name = vertex.getName();

    // Use intersection name for street vertices to get better names
    if (vertex instanceof StreetVertex && !(vertex instanceof TemporaryStreetLocation)) {
      name = ((StreetVertex) vertex).getIntersectionName();
    }

    // If no name available, use default
    if (name == null || name.toString().trim().isEmpty()) {
      name = new NonLocalizedString(defaultName);
    }

    return Place.normal(vertex, name);
  }

  /**
   * Holds the A* routing results for all three segments of a carpool journey
   */
  private static class CarpoolRouting {

    @Nullable
    final GraphPath<State, Edge, Vertex> accessPath;

    @Nullable
    final GraphPath<State, Edge, Vertex> carpoolPath;

    @Nullable
    final GraphPath<State, Edge, Vertex> egressPath;

    final double totalCost;
    final int totalDuration;
    final double totalDistance;

    CarpoolRouting(
      @Nullable GraphPath<State, Edge, Vertex> accessPath,
      @Nullable GraphPath<State, Edge, Vertex> carpoolPath,
      @Nullable GraphPath<State, Edge, Vertex> egressPath
    ) {
      this.accessPath = accessPath;
      this.carpoolPath = carpoolPath;
      this.egressPath = egressPath;

      // Calculate totals from the paths
      double cost = 0;
      int duration = 0;
      double distance = 0;

      if (accessPath != null) {
        cost += accessPath.getWeight();
        duration += accessPath.getDuration();
        distance += accessPath.edges.stream().mapToDouble(Edge::getDistanceMeters).sum();
      }
      if (carpoolPath != null) {
        cost += carpoolPath.getWeight();
        duration += carpoolPath.getDuration();
        distance += carpoolPath.edges.stream().mapToDouble(Edge::getDistanceMeters).sum();
      }
      if (egressPath != null) {
        cost += egressPath.getWeight();
        duration += egressPath.getDuration();
        distance += egressPath.edges.stream().mapToDouble(Edge::getDistanceMeters).sum();
      }

      this.totalCost = cost;
      this.totalDuration = duration;
      this.totalDistance = distance;
    }

    boolean isComplete() {
      return accessPath != null && carpoolPath != null && egressPath != null;
    }
  }

  private static class CarpoolTripCandidate {

    final CarpoolTrip trip;
    final double accessDistance;
    final double egressDistance;
    final double estimatedCost;

    CarpoolTripCandidate(
      CarpoolTrip trip,
      double accessDistance,
      double egressDistance,
      double estimatedCost
    ) {
      this.trip = trip;
      this.accessDistance = accessDistance;
      this.egressDistance = egressDistance;
      this.estimatedCost = estimatedCost;
    }
  }
}
