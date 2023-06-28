package org.opentripplanner.graph_builder.module;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.strategy.ComposingSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.vehicletostopheuristics.BikeToStopSkipEdgeStrategy;
import org.opentripplanner.ext.vehicletostopheuristics.VehicleToStopSkipEdgeStrategy;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graphfinder.DirectGraphFinder;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.StreetSearchRequestMapper;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;

/**
 * These library functions are used by the streetless and streetful stop linkers, and in profile
 * transfer generation.
 * TODO OTP2 Fold these into org.opentripplanner.routing.graphfinder.StreetGraphFinder
 *           These are not library functions, this is instantiated as an object. Define lifecycle of the object (reuse?).
 *           Because AStar instances should only be used once, NearbyStopFinder should only be used once.
 * Ideally they could also be used in long distance mode and profile routing for the street segments.
 * For each stop, it finds the closest stops on all other patterns. This reduces the number of transfer edges
 * significantly compared to simple radius-constrained all-to-all stop linkage.
 */
public class NearbyStopFinder {

  public final boolean useStreets;

  private final TransitService transitService;

  private final Duration durationLimit;
  private final DataOverlayContext dataOverlayContext;

  private DirectGraphFinder directGraphFinder;

  /**
   * Construct a NearbyStopFinder for the given graph and search radius.
   *
   * @param useStreets if true, search via the street network instead of using straight-line
   *                   distance.
   */
  public NearbyStopFinder(
    TransitService transitService,
    Duration durationLimit,
    DataOverlayContext dataOverlayContext,
    boolean useStreets
  ) {
    this.transitService = transitService;
    this.dataOverlayContext = dataOverlayContext;
    this.useStreets = useStreets;
    this.durationLimit = durationLimit;

    if (!useStreets) {
      // We need to accommodate straight line distance (in meters) but when streets are present we
      // use an earliest arrival search, which optimizes on time. Ideally we'd specify in meters,
      // but we don't have much of a choice here. Use the default walking speed to convert.
      this.directGraphFinder = new DirectGraphFinder(transitService::findRegularStop);
    }
  }

  /**
   * Find all unique nearby stops that are the closest stop on some trip pattern or flex trip. Note
   * that the result will include the origin vertex if it is an instance of StopVertex. This is
   * intentional: we don't want to return the next stop down the line for trip patterns that pass
   * through the origin vertex.
   */
  public Set<NearbyStop> findNearbyStopsConsideringPatterns(
    Vertex vertex,
    RouteRequest routingRequest,
    StreetRequest streetRequest,
    boolean reverseDirection
  ) {
    /* Track the closest stop on each pattern passing nearby. */
    MinMap<TripPattern, NearbyStop> closestStopForPattern = new MinMap<>();

    /* Track the closest stop on each flex trip nearby. */
    MinMap<FlexTrip<?, ?>, NearbyStop> closestStopForFlexTrip = new MinMap<>();

    /* Iterate over nearby stops via the street network or using straight-line distance, depending on the graph. */
    for (NearbyStop nearbyStop : findNearbyStops(
      vertex,
      routingRequest,
      streetRequest,
      reverseDirection
    )) {
      StopLocation ts1 = nearbyStop.stop;

      if (ts1 instanceof RegularStop) {
        /* Consider this destination stop as a candidate for every trip pattern passing through it. */
        for (TripPattern pattern : transitService.getPatternsForStop(ts1)) {
          if (
            reverseDirection
              ? pattern.canAlight(nearbyStop.stop)
              : pattern.canBoard(nearbyStop.stop)
          ) {
            closestStopForPattern.putMin(pattern, nearbyStop);
          }
        }
      }

      if (OTPFeature.FlexRouting.isOn()) {
        for (FlexTrip<?, ?> trip : transitService.getFlexIndex().getFlexTripsByStop(ts1)) {
          if (
            reverseDirection
              ? trip.isAlightingPossible(nearbyStop)
              : trip.isBoardingPossible(nearbyStop)
          ) {
            closestStopForFlexTrip.putMin(trip, nearbyStop);
          }
        }
      }
    }

    /* Make a transfer from the origin stop to each destination stop that was the closest stop on any pattern. */
    Set<NearbyStop> uniqueStops = new HashSet<>();
    uniqueStops.addAll(closestStopForFlexTrip.values());
    uniqueStops.addAll(closestStopForPattern.values());
    return uniqueStops;
  }

  /**
   * Return all stops within a certain radius of the given vertex, using network distance along
   * streets. Use the correct method depending on whether the graph has street data or not. If the
   * origin vertex is a StopVertex, the result will include it; this characteristic is essential for
   * associating the correct stop with each trip pattern in the vicinity.
   */
  public List<NearbyStop> findNearbyStops(
    Vertex vertex,
    RouteRequest routingRequest,
    StreetRequest streetRequest,
    boolean reverseDirection
  ) {
    if (useStreets) {
      return findNearbyStopsViaStreets(
        Set.of(vertex),
        reverseDirection,
        routingRequest,
        streetRequest
      );
    } else {
      return findNearbyStopsViaDirectTransfers(vertex);
    }
  }

  /**
   * Return all stops within a certain radius of the given vertex, using network distance along
   * streets. If the origin vertex is a StopVertex, the result will include it.
   *
   * @param originVertices   the origin point of the street search
   * @param reverseDirection if true the paths returned instead originate at the nearby stops and
   *                         have the originVertex as the destination
   */
  public List<NearbyStop> findNearbyStopsViaStreets(
    Set<Vertex> originVertices,
    boolean reverseDirection,
    RouteRequest request,
    StreetRequest streetRequest
  ) {
    OTPRequestTimeoutException.checkForTimeout();

    List<NearbyStop> stopsFound = createDirectlyConnectedStops(
      originVertices,
      reverseDirection,
      request,
      streetRequest
    );

    // Return only the origin vertices if there are no valid street modes
    if (streetRequest.mode() == StreetMode.NOT_SET) {
      return stopsFound;
    }

    ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder
      .of()
      .setSkipEdgeStrategy(getSkipEdgeStrategy(reverseDirection, request))
      .setDominanceFunction(new DominanceFunctions.MinimumWeight())
      .setRequest(request)
      .setArriveBy(reverseDirection)
      .setStreetRequest(streetRequest)
      .setFrom(reverseDirection ? null : originVertices)
      .setTo(reverseDirection ? originVertices : null)
      .setDataOverlayContext(dataOverlayContext)
      .getShortestPathTree();

    // Only used if OTPFeature.FlexRouting.isOn()
    Multimap<AreaStop, State> locationsMap = ArrayListMultimap.create();

    if (spt != null) {
      // TODO use GenericAStar and a traverseVisitor? Add an earliestArrival switch to genericAStar?
      for (State state : spt.getAllStates()) {
        Vertex targetVertex = state.getVertex();
        if (originVertices.contains(targetVertex)) continue;
        if (targetVertex instanceof TransitStopVertex && state.isFinal()) {
          stopsFound.add(
            NearbyStop.nearbyStopForState(state, ((TransitStopVertex) targetVertex).getStop())
          );
        }
        if (
          OTPFeature.FlexRouting.isOn() &&
          targetVertex instanceof StreetVertex &&
          ((StreetVertex) targetVertex).areaStops != null
        ) {
          for (AreaStop areaStop : ((StreetVertex) targetVertex).areaStops) {
            // This is for a simplification, so that we only return one vertex from each
            // stop location. All vertices are added to the multimap, which is filtered
            // below, so that only the closest vertex is added to stopsFound
            if (canBoardFlex(state, reverseDirection)) {
              locationsMap.put(areaStop, state);
            }
          }
        }
      }
    }

    if (OTPFeature.FlexRouting.isOn()) {
      for (var locationStates : locationsMap.asMap().entrySet()) {
        AreaStop areaStop = locationStates.getKey();
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

        stopsFound.add(NearbyStop.nearbyStopForState(min, areaStop));
      }
    }

    return stopsFound;
  }

  private List<NearbyStop> findNearbyStopsViaDirectTransfers(Vertex vertex) {
    // It make sense for the directGraphFinder to use meters as a limit, so we convert first
    double limitMeters = durationLimit.toSeconds() * WalkPreferences.DEFAULT.speed();
    Coordinate c0 = vertex.getCoordinate();
    return directGraphFinder.findClosestStops(c0, limitMeters);
  }

  private SkipEdgeStrategy<State, Edge> getSkipEdgeStrategy(
    boolean reverseDirection,
    RouteRequest routingRequest
  ) {
    var durationSkipEdgeStrategy = new DurationSkipEdgeStrategy(durationLimit);

    // if we compute the accesses for Park+Ride, Bike+Ride and Bike+Transit we don't want to
    // search the full durationLimit as this returns way too many stops.
    // this is both slow and returns suboptimal results as it favours long drives with short
    // transit legs.
    // therefore, we use a heuristic based on the number of routes and their mode to determine
    // what are "good" stops for those accesses. if we have reached a threshold of "good" stops
    // we stop the access search.
    if (
      !reverseDirection &&
      OTPFeature.VehicleToStopHeuristics.isOn() &&
      VehicleToStopSkipEdgeStrategy.applicableModes.contains(
        routingRequest.journey().access().mode()
      )
    ) {
      var strategy = new VehicleToStopSkipEdgeStrategy(
        transitService::getPatternsForStop,
        routingRequest.journey().transit().filters()
      );

      return new ComposingSkipEdgeStrategy<>(strategy, durationSkipEdgeStrategy);
    } else if (
      OTPFeature.VehicleToStopHeuristics.isOn() &&
      routingRequest.journey().access().mode() == StreetMode.BIKE
    ) {
      var strategy = new BikeToStopSkipEdgeStrategy(transitService::getTripsForStop);
      return new ComposingSkipEdgeStrategy<>(strategy, durationSkipEdgeStrategy);
    } else {
      return durationSkipEdgeStrategy;
    }
  }

  private static List<NearbyStop> createDirectlyConnectedStops(
    Set<Vertex> originVertices,
    boolean reverseDirection,
    RouteRequest request,
    StreetRequest streetRequest
  ) {
    List<NearbyStop> stopsFound = new ArrayList<>();

    StreetSearchRequest streetSearchRequest = StreetSearchRequestMapper
      .mapToTransferRequest(request)
      .withArriveBy(reverseDirection)
      .withMode(streetRequest.mode())
      .build();

    /* Add the origin vertices if they are stops */
    for (Vertex vertex : originVertices) {
      if (vertex instanceof TransitStopVertex tsv) {
        stopsFound.add(
          new NearbyStop(
            tsv.getStop(),
            0,
            Collections.emptyList(),
            new State(vertex, streetSearchRequest)
          )
        );
      }
    }

    return stopsFound;
  }

  private boolean canBoardFlex(State state, boolean reverse) {
    Collection<Edge> edges = reverse
      ? state.getVertex().getIncoming()
      : state.getVertex().getOutgoing();

    return edges
      .stream()
      .anyMatch(e ->
        e instanceof StreetEdge && ((StreetEdge) e).getPermission().allows(TraverseMode.CAR)
      );
  }
}
