package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.OptionalInt;
import java.util.Set;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.graph_builder.module.nearbystops.StopResolver;
import org.opentripplanner.graph_builder.module.nearbystops.StreetNearbyStopFinder;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ExtensionRequestContext;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.utils.collection.ListUtils;

public class ViaAccessEgressRouter extends AccessEgressRouter {

  private final StopResolver stopResolver;
  private final ViaDirectStreetRouter directRouter;

  ViaAccessEgressRouter(StopResolver stopResolver) {
    super(stopResolver);
    this.stopResolver = stopResolver;
    this.directRouter = new ViaDirectStreetRouter();
  }

  @Override
  Collection<NearbyStop> findStreetAccessEgresses(
    RouteRequest request,
    StreetMode streetMode,
    TraverseVisitor<State, Edge> traverseVisitor,
    Collection<ExtensionRequestContext> extensionRequestContexts,
    AccessEgressType accessOrEgress,
    Duration durationLimit,
    int maxStopCount,
    LinkingContext linkingContext,
    Set<Vertex> ignoreVertices,
    float maxCarSpeed
  ) {
    var accessEgressRequest = getViaFriendlyRequest(request);
    var firstVertices = accessOrEgress.isAccess()
      ? linkingContext.findVertices(accessEgressRequest.from())
      : linkingContext.findVertices(accessEgressRequest.to());
    var stopsFromFirstLocation = StreetNearbyStopFinder.of(
      stopResolver,
      durationLimit,
      maxStopCount
    )
      .withIgnoreVertices(ignoreVertices)
      .withExtensionRequestContexts(extensionRequestContexts)
      .build()
      .findNearbyStops(firstVertices, accessEgressRequest, streetMode, accessOrEgress.isEgress());
    var lastViaWithCoordinates = lastCoordinateViaLocationIndex(request, accessOrEgress);
    // There are no coordinate locations to route to/from
    if (lastViaWithCoordinates.isEmpty()) {
      return stopsFromFirstLocation;
    }
    var graphPathFinder = new GraphPathFinder(
      traverseVisitor,
      extensionRequestContexts,
      maxCarSpeed
    );
    var directRequest = getDirectRequest(
      request,
      accessOrEgress,
      lastViaWithCoordinates.getAsInt()
    );
    return accessOrEgress.isAccess()
      ? findStreetAccesses(
          accessEgressRequest,
          directRequest,
          streetMode,
          extensionRequestContexts,
          stopsFromFirstLocation,
          durationLimit,
          maxStopCount,
          linkingContext,
          ignoreVertices,
          graphPathFinder
        )
      : findStreetEgresses(
          accessEgressRequest,
          directRequest,
          streetMode,
          extensionRequestContexts,
          stopsFromFirstLocation,
          durationLimit,
          maxStopCount,
          linkingContext,
          ignoreVertices,
          graphPathFinder
        );
  }

  private Collection<NearbyStop> findStreetAccesses(
    RouteRequest accessEgressRequest,
    RouteRequest directRequest,
    StreetMode streetMode,
    Collection<ExtensionRequestContext> extensionRequestContexts,
    Collection<NearbyStop> stopsFromFirstLocation,
    Duration durationLimit,
    int maxStopCount,
    LinkingContext linkingContext,
    Set<Vertex> ignoreVertices,
    GraphPathFinder graphPathFinder
  ) {
    var nearbyStops = new ArrayList<>(stopsFromFirstLocation);
    var paths = directRouter.findDepartAfterPaths(
      linkingContext,
      graphPathFinder,
      directRequest,
      true,
      true
    );
    var vias = accessEgressRequest.listViaLocationsWithCoordinates();
    var durationLeft = durationLimit;
    var i = 0;
    var accumulatedDistance = 0.0;
    var accumulatedEdges = new ArrayList<Edge>();
    var accumulatedLastStates = new ArrayList<State>();
    while (i < paths.size() && durationLeft.isPositive()) {
      var path = paths.get(i);
      accumulatedDistance += path.edges.stream().mapToDouble(Edge::getDistanceMeters).sum();
      accumulatedEdges.addAll(path.edges);
      accumulatedLastStates.add(path.states.getLast());
      durationLeft = durationLeft.minus(Duration.ofSeconds(path.getDuration()));
      var via = vias.get(i);
      var vertices = linkingContext.findVertices(via);
      var viaRequest = getViaRequest(
        accessEgressRequest,
        via,
        Instant.ofEpochSecond(path.getEndTime())
      );
      // TODO implement some algorithm for lowering the maximum stop count.
      var stopsFromVia = StreetNearbyStopFinder.of(stopResolver, durationLeft, maxStopCount)
        .withIgnoreVertices(ignoreVertices)
        .withExtensionRequestContexts(extensionRequestContexts)
        .build()
        .findNearbyStops(vertices, viaRequest, streetMode, false);
      for (NearbyStop nearbyStop : stopsFromVia) {
        var distance = accumulatedDistance + nearbyStop.distance;
        var edges = ListUtils.combine(accumulatedEdges, nearbyStop.edges);
        var lastStates = ListUtils.combine(accumulatedLastStates, nearbyStop.lastStates);
        var adjustedStop = new NearbyStop(nearbyStop.stop, distance, edges, lastStates);
        nearbyStops.add(adjustedStop);
      }
      i += 1;
    }
    return nearbyStops;
  }

  private Collection<NearbyStop> findStreetEgresses(
    RouteRequest accessEgressRequest,
    RouteRequest directRequest,
    StreetMode streetMode,
    Collection<ExtensionRequestContext> extensionRequestContexts,
    Collection<NearbyStop> stopsFromFirstLocation,
    Duration durationLimit,
    int maxStopCount,
    LinkingContext linkingContext,
    Set<Vertex> ignoreVertices,
    GraphPathFinder graphPathFinder
  ) {
    var nearbyStops = new ArrayList<>(stopsFromFirstLocation);
    var paths = directRouter.findArriveByPaths(
      linkingContext,
      graphPathFinder,
      directRequest,
      true,
      true
    );
    var vias = accessEgressRequest.listViaLocationsWithCoordinates();
    var durationLeft = durationLimit;
    var i = 0;
    var accumulatedDistance = 0.0;
    var accumulatedEdges = new ArrayList<Edge>();
    var accumulatedLastStates = new ArrayList<State>();
    while (i < paths.size() && durationLeft.isPositive()) {
      var path = paths.get(i);
      accumulatedDistance += path.edges.stream().mapToDouble(Edge::getDistanceMeters).sum();
      accumulatedEdges.addAll(path.edges);
      // TODO try to get rid of this reverse since now the state is reversed three times in total
      accumulatedLastStates.add(path.states.getLast().reverse());
      durationLeft = durationLeft.minus(Duration.ofSeconds(path.getDuration()));
      var via = vias.get(i);
      var vertices = linkingContext.findVertices(via);
      var viaRequest = getViaRequest(
        accessEgressRequest,
        via,
        Instant.ofEpochSecond(path.getStartTime())
      );
      // TODO implement some algorithm for lowering the maximum stop count.
      var stopsFromVia = StreetNearbyStopFinder.of(stopResolver, durationLeft, maxStopCount)
        .withIgnoreVertices(ignoreVertices)
        .withExtensionRequestContexts(extensionRequestContexts)
        .build()
        .findNearbyStops(vertices, viaRequest, streetMode, true);
      for (NearbyStop nearbyStop : stopsFromVia) {
        var distance = accumulatedDistance + nearbyStop.distance;
        var edges = ListUtils.combine(nearbyStop.edges, accumulatedEdges);
        var lastStates = ListUtils.combine(accumulatedLastStates, nearbyStop.lastStates);
        var adjustedStop = new NearbyStop(nearbyStop.stop, distance, edges, lastStates);
        nearbyStops.add(adjustedStop);
      }
      i += 1;
    }
    return nearbyStops;
  }

  /**
   * @return the last index of the last consecutive via location from the beginning (access) or from
   * the end (egress), or empty if the first via location doesn't have a coordinate.
   */
  private OptionalInt lastCoordinateViaLocationIndex(RouteRequest request, AccessEgressType type) {
    var vias = type.isAccess() ? request.listViaLocations() : request.listViaLocations().reversed();
    int viaIndex = -1;
    for (int i = 0; i < vias.size(); i++) {
      if (vias.get(i) instanceof VisitViaLocation visit) {
        if (visit.coordinate().isPresent()) {
          viaIndex = i;
        } else {
          break;
        }
      } else {
        break;
      }
    }
    return viaIndex == -1 ? OptionalInt.empty() : OptionalInt.of(viaIndex);
  }

  /**
   * TODO we might want to continue on a vehicle if there is no wait time defined for a via point.
   */
  private RouteRequest getViaFriendlyRequest(RouteRequest originalRequest) {
    return originalRequest
      .copyOf()
      // TODO we might want to change this behaviour
      .withPreferences(preferences ->
        preferences
          .withBike(bike ->
            bike.withRental(rental -> rental.withAllowArrivingInRentedVehicleAtDestination(false))
          )
          .withScooter(scooter ->
            scooter.withRental(rental ->
              rental.withAllowArrivingInRentedVehicleAtDestination(false)
            )
          )
          .withCar(car ->
            car.withRental(rental -> rental.withAllowArrivingInRentedVehicleAtDestination(false))
          )
      )
      .buildRequest();
  }

  private RouteRequest getDirectRequest(
    RouteRequest request,
    AccessEgressType type,
    int lastViaWithCoordinates
  ) {
    var originalVias = request.listViaLocations();
    // Filter out all via locations after the last consecutive one with coordinates
    var vias = type.isAccess()
      ? originalVias.subList(0, lastViaWithCoordinates + 1)
      : originalVias.subList(originalVias.size() - lastViaWithCoordinates - 1, originalVias.size());
    var mode = getStreetModeAfterOrigin(request.journey().access().mode());
    var maxDuration = getMaxDuration(request);
    return request
      .copyOf()
      .withJourney(journey -> journey.withDirect(new StreetRequest(mode)))
      .withPreferences(preferences ->
        preferences.withStreet(street ->
          street.withMaxDirectDuration(streetModeBuilder ->
            streetModeBuilder.with(mode, maxDuration)
          )
        )
      )
      .withViaLocations(vias)
      .withArriveBy(type.isEgress())
      .buildRequest();
  }

  private RouteRequest getViaRequest(RouteRequest request, GenericLocation via, Instant startTime) {
    return request.copyOf().withDateTime(startTime).withFrom(via).buildRequest();
  }

  /**
   * TODO we might want to continue on a vehicle if there is no wait time defined for a via point.
   */
  private StreetMode getStreetModeAfterOrigin(StreetMode mode) {
    if (mode.includesParking() || mode.includesRenting()) {
      return StreetMode.WALK;
    }
    return mode;
  }

  private Duration getMaxDuration(RouteRequest request) {
    return request
      .preferences()
      .street()
      .accessEgress()
      .maxDuration()
      .valueOf(request.journey().access().mode());
  }
}
