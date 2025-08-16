package org.opentripplanner.ext.carpooling.internal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.PathComparator;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.data.KristiansandCarpoolingData;
import org.opentripplanner.ext.carpooling.model.CarpoolLeg;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.framework.geometry.GeometryUtils;
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
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.transit.model.site.AreaStop;

public class DefaultCarpoolingService implements CarpoolingService {

  private final CarpoolingRepository repository;
  private final Graph graph;

  public DefaultCarpoolingService(CarpoolingRepository repository, Graph graph) {
    KristiansandCarpoolingData.populateRepository(repository, graph);
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
  public List<Itinerary> route(OtpServerRequestContext serverContext, RouteRequest request)
    throws RoutingValidationException {
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

    List<CarpoolTrip> availableTrips;

    try (
      var temporaryVertices = new TemporaryVerticesContainer(
        serverContext.graph(),
        request.from(),
        request.to(),
        request.journey().direct().mode(),
        request.journey().direct().mode()
      )
    ) {
      // Prepare access/egress transfers
      Collection<NearbyStop> accessStops = getClosestAreaStopsToVertex(
        serverContext,
        request,
        temporaryVertices.getFromVertices(),
        repository.getBoardingAreasForVertex(),
        false
      );

      Collection<NearbyStop> egressStops = getClosestAreaStopsToVertex(
        serverContext,
        request,
        temporaryVertices.getToVertices(),
        repository.getAlightingAreasForVertex(),
        true
      );

      // Only keep stops that are actually carpool boarding/alighting areas
      accessStops = accessStops
        .stream()
        .filter(stop -> repository.isCarpoolBoardingArea(stop.stop.getId()))
        .toList();

      egressStops = egressStops
        .stream()
        .filter(stop -> repository.isCarpoolAlightingArea(stop.stop.getId()))
        .toList();

      Set<CarpoolTrip> boardingTrips = accessStops
        .stream()
        .map(stop -> {
          var trip = repository.getCarpoolTripByBoardingArea(stop.stop.getId());
          trip.setBoardingStop(stop);
          return trip;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

      Set<CarpoolTrip> alightingTrips = egressStops
        .stream()
        .map(stop -> {
          var trip = repository.getCarpoolTripByAlightingArea(stop.stop.getId());
          trip.setAlightingStop(stop);
          return trip;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

      // Relevant trips are in both the boarding and alighting sets
      List<CarpoolTrip> relevantTrips = boardingTrips
        .stream()
        .filter(alightingTrips::contains)
        .collect(Collectors.toList());

      // Now that we have the relevant trips, we can proceed with routing for access, egress, and carpool segments
      if (relevantTrips.isEmpty()) {
        // No relevant carpool trips found, return empty list
        return Collections.emptyList();
      }

      // Relevant trips leave between the arrival at the access and two hours from then
      relevantTrips = relevantTrips
        .stream()
        .filter(trip ->
          trip.getStartTime().toInstant().isAfter(trip.getBoardingStop().state.getTime())
            && trip.getStartTime().toInstant().isBefore(trip.getBoardingStop().state.getTime().plusSeconds(2 * 3600)))
        .collect(Collectors.toList());

      // More open questions:
      // - ArriveBy support?
      // - How to store AreaStops so they can be requested? New class?
      // - How to apply updates to trips?

      // At this point we have relevant trips with boarding and alighting stops as NearbyStops.
      // Now we need to route from starting point to the vertex in the nearby stops, between the vertices, and from the stop in the alighting stop to the destination
      if (relevantTrips.isEmpty()) {
        // No relevant carpool trips found within the next 2 hours, return empty list
        return Collections.emptyList();
      }
      availableTrips = relevantTrips;
    }

    // Perform A* routing for the top candidates and create itineraries
    List<Itinerary> itineraries = new ArrayList<>();
    int maxResults = Math.min(availableTrips.size(), 3);

    for (int i = 0; i < maxResults; i++) {
      CarpoolTrip trip = availableTrips.get(i);

      Coordinate pickup = trip.getBoardingStop().state.getVertex().getCoordinate();
      Coordinate dropoff = trip.getAlightingStop().state.getVertex().getCoordinate();
      GraphPath<State, Edge, Vertex> routing = routeBetweenCoordinates(
        serverContext,
        pickup,
        dropoff,
        StreetMode.CAR,
        request
      );

      // Only create itinerary if all routing segments succeeded
      Itinerary itinerary = createItineraryFromRouting(trip, routing);
      if (itinerary != null) {
        itineraries.add(itinerary);
      }
    }

    return itineraries;
  }

  private List<NearbyStop> getClosestAreaStopsToVertex(
    OtpServerRequestContext serverContext,
    RouteRequest request,
    Set<Vertex> originVertices,
    Multimap<StreetVertex, AreaStop> destinations,
    boolean reverseDirection
  ) {
    var streetSearch = StreetSearchBuilder.of()
      .setSkipEdgeStrategy(
        new DurationSkipEdgeStrategy<>(serverContext.flexParameters().maxAccessWalkDuration())
      )
      .setDominanceFunction(new DominanceFunctions.MinimumWeight())
      .setRequest(request)
      .setArriveBy(reverseDirection)
      .setStreetRequest(request.journey().direct())
      .setFrom(reverseDirection ? null : originVertices)
      .setTo(reverseDirection ? originVertices : null);

    ShortestPathTree<State, Edge, Vertex> spt = streetSearch.getShortestPathTree();

    if (spt == null) {
      return Collections.emptyList();
    }

    Multimap<AreaStop, State> locationsMap = ArrayListMultimap.create();

    for (State state : spt.getAllStates()) {
      Vertex targetVertex = state.getVertex();
      if (
        targetVertex instanceof StreetVertex streetVertex && destinations.containsKey(streetVertex)
      ) {
        for (AreaStop areaStop : destinations.get(streetVertex)) {
          locationsMap.put(areaStop, state);
        }
      }
    }

    List<NearbyStop> stopsFound = new ArrayList<>();
    for (var locationStates : locationsMap.asMap().entrySet()) {
      AreaStop areaStop = locationStates.getKey();
      State min = getMinState(locationStates);

      stopsFound.add(NearbyStop.nearbyStopForState(min, areaStop));
    }
    return stopsFound;
  }

  private static State getMinState(Map.Entry<AreaStop, Collection<State>> locationStates) {
    Collection<State> states = locationStates.getValue();
    // Select the vertex from all vertices that are reachable per AreaStop by taking
    // the minimum walking distance
    State min = Collections.min(states, Comparator.comparing(State::getWeight));

    // If the best state for this AreaStop is a SplitterVertex, we want to get the
    // TemporaryStreetLocation instead. This allows us to reach SplitterVertices in both
    // directions when routing later.
    if (min.getBackState().getVertex() instanceof TemporaryStreetLocation) {
      min = min.getBackState();
    }
    return min;
  }

  /**
   * Performs A* street routing between two coordinates using the specified street mode.
   * Returns the routing result with distance, time, and geometry.
   */
  @Nullable
  private GraphPath<State, Edge, Vertex> routeBetweenCoordinates(
    OtpServerRequestContext serverContext,
    Coordinate from,
    Coordinate to,
    StreetMode streetMode,
    RouteRequest request
  ) {
    try (
      var temporaryVertices = new TemporaryVerticesContainer(
        graph,
        GenericLocation.fromCoordinate(from.getY(), from.getX()),
        GenericLocation.fromCoordinate(to.getY(), to.getX()),
        streetMode,
        streetMode
      )
    ) {
      var maxCarSpeed = serverContext.streetLimitationParametersService().getMaxCarSpeed();

      StreetPreferences preferences = request.preferences().street();

      StreetSearchBuilder aStar = StreetSearchBuilder.of()
        .setHeuristic(new EuclideanRemainingWeightHeuristic(maxCarSpeed))
        .setSkipEdgeStrategy(
          new DurationSkipEdgeStrategy(
            preferences.maxDirectDuration().valueOf(request.journey().direct().mode())
          )
        )
        // FORCING the dominance function to weight only
        .setDominanceFunction(new DominanceFunctions.MinimumWeight())
        .setRequest(request)
        .setStreetRequest(new StreetRequest(streetMode))
        .setFrom(temporaryVertices.getFromVertices())
        .setTo(temporaryVertices.getToVertices());

      List<GraphPath<State, Edge, Vertex>> paths = aStar.getPathsToTarget();
      paths.sort(new PathComparator(request.arriveBy()));

      return paths.getFirst();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Creates a complete itinerary from A* routing results with proper walking and carpool legs
   */
  private Itinerary createItineraryFromRouting(CarpoolTrip trip, GraphPath<State, Edge, Vertex> carpoolPath) {
    List<Leg> legs = new ArrayList<>();

    // 1. Access walking leg (origin to pickup)
    Leg accessLeg = createWalkingLegFromPath(
      trip.getBoardingStop(),
      null,
      trip.getStartTime(),
      "Walk to pickup"
    );
    if (accessLeg != null) {
      legs.add(accessLeg);
    }

    var drivingEndTime = trip
      .getStartTime()
      .plus(
        Duration.between(
          carpoolPath.states.getFirst().getTime(),
          carpoolPath.states.getLast().getTime()
        )
      );

    // 2. Carpool transit leg (pickup to dropoff)
    CarpoolLeg carpoolLeg = CarpoolLeg.of()
      .withStartTime(trip.getStartTime())
      .withEndTime(drivingEndTime)
      .withFrom(
        createPlaceFromState(
          carpoolPath.states.getFirst(),
          "Pickup at " + trip.getBoardingArea().getName()
        )
      )
      .withTo(
        createPlaceFromState(
          carpoolPath.states.getLast(),
          "Dropoff at " + trip.getAlightingArea().getName()
        )
      )
      .withGeometry(
        GeometryUtils.concatenateLineStrings(carpoolPath.edges, Edge::getGeometry)
      )
      .withDistanceMeters(
        carpoolPath.edges.stream().mapToDouble(Edge::getDistanceMeters).sum()
      )
      .withGeneralizedCost((int) carpoolPath.getWeight())
      .build();
    legs.add(carpoolLeg);

    // 3. Egress walking leg (dropoff to destination)
    Leg egressLeg = createWalkingLegFromPath(
      trip.getAlightingStop(),
      drivingEndTime,
      null,
      "Walk from dropoff"
    );
    if (egressLeg != null) {
      legs.add(egressLeg);
    }

    return Itinerary.ofDirect(legs)
      .withGeneralizedCost(Cost.costOfSeconds(accessLeg.generalizedCost() +
        carpoolLeg.generalizedCost() + egressLeg.generalizedCost()))
      .build();
  }

  /**
   * Creates a walking leg from a GraphPath with proper geometry and timing.
   * This reuses the same pattern as OTP's GraphPathToItineraryMapper but simplified
   * for carpooling service use.
   */
  @Nullable
  private Leg createWalkingLegFromPath(
    NearbyStop nearbyStop,
    ZonedDateTime legStartTime,
    ZonedDateTime legEndTime,
    String name
  ) {
    if (nearbyStop == null || nearbyStop.edges.isEmpty()) {
      return null;
    }

    var graphPath = new GraphPath<>(nearbyStop.state);

    var firstState = graphPath.states.getFirst();
    var lastState = graphPath.states.getLast();

    // Extract edges (skip first state which doesn't have a back edge)
    List<Edge> edges = nearbyStop.edges;

    if (edges.isEmpty()) {
      return null;
    }

    // Create geometry from edges
    LineString geometry = GeometryUtils.concatenateLineStrings(edges, Edge::getGeometry);

    var legDuration = Duration.between(firstState.getTime(), lastState.getTime());
    if (legStartTime != null && legEndTime == null) {
      legEndTime = legStartTime.plus(legDuration);
    } else if (legEndTime != null && legStartTime == null) {
      legStartTime = legEndTime.minus(legDuration);
    }

    // Build the walking leg
    return StreetLeg.of()
      .withMode(TraverseMode.WALK)
      .withStartTime(legStartTime)
      .withEndTime(legEndTime)
      .withFrom(createPlaceFromState(firstState, name + " start"))
      .withTo(createPlaceFromState(lastState, name + " end"))
      .withDistanceMeters(nearbyStop.distance)
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
}
