package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.LegsToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.StreetPathToLegsMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.path.ElevationChange;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.transit.service.TransitServiceResolver;

public class ViaDirectStreetRouter extends DirectStreetRouter {

  @Override
  List<Itinerary> findItineraries(
    Graph graph,
    TransitService transitService,
    StreetDetailsService streetDetailsService,
    GraphPathFinder graphPathFinder,
    LinkingContext linkingContext,
    RouteRequest request
  ) {
    var paths = request.arriveBy()
      ? findArriveByPaths(linkingContext, graphPathFinder, request, false, false)
      : findDepartAfterPaths(linkingContext, graphPathFinder, request, false, false);
    return mapToItineraries(graph, transitService, streetDetailsService, request, paths);
  }

  @Override
  boolean isRequestInvalidForRouting(RouteRequest request) {
    // No support for pass-through locations or visit via locations with just stops as they force
    // you to use transit.
    return (
      request.listViaLocations().size() != request.listViaLocationsWithCoordinates().size() ||
      request.journey().direct().mode() == StreetMode.NOT_SET
    );
  }

  @Override
  boolean isStraightLineDistanceWithinLimit(
    LinkingContext linkingContext,
    RouteRequest request,
    double maxDistanceLimit
  ) {
    var vias = request.listViaLocationsWithCoordinates();
    // TODO This currently only calculates the distances between the first vertex from each
    //  location
    double distance = SphericalDistanceLibrary.distance(
      getFirstCoordinateForLocation(linkingContext, request.from()),
      getFirstCoordinateForLocation(linkingContext, vias.getFirst())
    );
    for (int i = 0; i < vias.size() - 1; i++) {
      distance += SphericalDistanceLibrary.distance(
        getFirstCoordinateForLocation(linkingContext, vias.get(i)),
        getFirstCoordinateForLocation(linkingContext, vias.get(i + 1))
      );
    }
    distance += SphericalDistanceLibrary.distance(
      getFirstCoordinateForLocation(linkingContext, vias.getLast()),
      getFirstCoordinateForLocation(linkingContext, request.to())
    );
    return distance < maxDistanceLimit;
  }

  List<StreetPath> findArriveByPaths(
    LinkingContext linkingContext,
    GraphPathFinder graphPathFinder,
    RouteRequest request,
    boolean allowPartialResults,
    boolean skipLastLeg
  ) {
    var baseRequest = getViaFriendlyRequest(request);
    var mode = baseRequest.journey().direct().mode();
    var newStreetRequest = getStreetRequestAfterFirstVia(mode);
    var requestWithNewMode = getRequestWithNewMode(baseRequest, newStreetRequest);

    var lastLocations = new ArrayList<>(request.listViaLocationsWithCoordinates());
    lastLocations.add(baseRequest.to());
    var minimumWaitTimes = getMinimumWaitTimes(baseRequest);

    var paths = new ArrayList<StreetPath>();
    var newStartTime = request.dateTime();
    var maxDurationLeft = getMaximumDirectDuration(request, mode);
    int i = lastLocations.size() - 2;
    try {
      while (i >= 0 && maxDurationLeft.isPositive()) {
        var from = lastLocations.get(i);
        var to = lastLocations.get(i + 1);
        var patchedRequest = getRequest(
          requestWithNewMode,
          from,
          to,
          newStartTime,
          newStreetRequest.mode(),
          maxDurationLeft
        );
        var path = findPath(linkingContext, graphPathFinder, patchedRequest);
        paths.add(path);

        var minimumWaitTime = minimumWaitTimes.get(i);
        newStartTime = path.startTime().minus(minimumWaitTime);
        // Wait time is not counted here as it doesn't slow down routing or inconvenience travelers
        // like travel time does
        maxDurationLeft = maxDurationLeft.minus(path.duration());
        i--;
      }

      if (!skipLastLeg) {
        var firstRequest = getRequest(
          baseRequest,
          baseRequest.from(),
          baseRequest.listViaLocationsWithCoordinates().getFirst(),
          newStartTime,
          mode,
          maxDurationLeft
        );
        var path = findPath(linkingContext, graphPathFinder, firstRequest);
        paths.add(path);
      }
    } catch (PathNotFoundException e) {
      if (!allowPartialResults) {
        throw e;
      }
    }
    return paths.reversed();
  }

  List<StreetPath> findDepartAfterPaths(
    LinkingContext linkingContext,
    GraphPathFinder graphPathFinder,
    RouteRequest request,
    boolean allowPartialResults,
    boolean skipLastLeg
  ) {
    var vias = request.listViaLocationsWithCoordinates();
    var baseRequest = getViaFriendlyRequest(request);
    var firstRequest = baseRequest.copyOf().withTo(vias.getFirst()).buildRequest();
    List<StreetPath> paths = new ArrayList<>();
    try {
      paths.add(findPath(linkingContext, graphPathFinder, firstRequest));

      var mode = baseRequest.journey().direct().mode();
      var newStreetRequest = getStreetRequestAfterFirstVia(mode);
      var requestWithNewMode = getRequestWithNewMode(firstRequest, newStreetRequest);

      var lastLocations = new ArrayList<>(vias);
      if (!skipLastLeg) {
        lastLocations.add(baseRequest.to());
      }
      var minimumWaitTimes = getMinimumWaitTimes(baseRequest);

      var maxDurationLeft = getMaximumDirectDuration(request, mode).minus(
        paths.getFirst().duration()
      );
      int i = 1;
      while (i < lastLocations.size() && maxDurationLeft.isPositive()) {
        var from = lastLocations.get(i - 1);
        var to = lastLocations.get(i);
        var minimumWaitTime = minimumWaitTimes.get(i - 1);
        var newStartTime = paths.getLast().endTime().plus(minimumWaitTime);
        var patchedRequest = getRequest(
          requestWithNewMode,
          from,
          to,
          newStartTime,
          newStreetRequest.mode(),
          maxDurationLeft
        );
        var path = findPath(linkingContext, graphPathFinder, patchedRequest);
        paths.add(path);
        // Wait time is not counted here as it doesn't slow down routing or inconvenience travelers
        // like travel time does
        maxDurationLeft = maxDurationLeft.minus(path.duration());
        i++;
      }
    } catch (PathNotFoundException e) {
      if (!allowPartialResults) {
        throw e;
      }
    }
    return paths;
  }

  private StreetPath findPath(
    LinkingContext linkingContext,
    GraphPathFinder graphPathFinder,
    RouteRequest request
  ) {
    // We don't really support multiple results from A*
    return graphPathFinder.find(request, linkingContext).getFirst();
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

  private List<Itinerary> mapToItineraries(
    Graph graph,
    TransitService transitService,
    StreetDetailsService streetDetailsService,
    RouteRequest request,
    List<StreetPath> paths
  ) {
    StreetPathToLegsMapper streetPathToLegsMapper = new StreetPathToLegsMapper(
      new TransitServiceResolver(transitService),
      transitService.getTimeZone(),
      streetDetailsService,
      graph.ellipsoidToGeoidDifference
    );
    var legs = paths
      .stream()
      .flatMap(path -> streetPathToLegsMapper.map(path, request).stream())
      .toList();
    var elevationChanges = paths.stream().map(StreetPath::calculateElevations).toList();
    var elevationGained = elevationChanges
      .stream()
      .mapToDouble(ElevationChange::elevationGainedMeters)
      .sum();
    var elevationLost = elevationChanges
      .stream()
      .mapToDouble(ElevationChange::elevationLostMeters)
      .sum();
    var itinerary = LegsToItineraryMapper.map(
      legs,
      paths.getLast().lastState().isRentingVehicleFromStation(),
      new ElevationChange(elevationGained, elevationLost)
    );
    return itinerary.map(List::of).orElse(List.of());
  }

  /**
   * TODO we might want to continue on a vehicle if there is no wait time defined for a via point.
   */
  private RouteRequest getRequestWithNewMode(
    RouteRequest originalRequest,
    StreetRequest newStreetRequest
  ) {
    return originalRequest
      .copyOf()
      .withJourney(journeyRequestBuilder -> journeyRequestBuilder.withDirect(newStreetRequest))
      .buildRequest();
  }

  private RouteRequest getRequest(
    RouteRequest originalRequest,
    GenericLocation from,
    GenericLocation to,
    Instant newStartTime,
    StreetMode mode,
    Duration maxDuration
  ) {
    return originalRequest
      .copyOf()
      .withFrom(from)
      .withTo(to)
      .withDateTime(newStartTime)
      .withPreferences(preferences ->
        preferences.withStreet(street ->
          street.withMaxDirectDuration(streetModeBuilder ->
            streetModeBuilder.with(mode, maxDuration)
          )
        )
      )
      .buildRequest();
  }

  /**
   * TODO we might want to continue on a vehicle if there is no wait time defined for a via point.
   */
  private StreetRequest getStreetRequestAfterFirstVia(StreetMode mode) {
    if (mode.includesParking() || mode.includesRenting()) {
      return new StreetRequest(StreetMode.WALK);
    }
    return new StreetRequest(mode);
  }

  private List<Duration> getMinimumWaitTimes(RouteRequest request) {
    return request.listViaLocations().stream().map(ViaLocation::minimumWaitTime).toList();
  }

  private Duration getMaximumDirectDuration(RouteRequest request, StreetMode mode) {
    return request.preferences().street().maxDirectDuration().valueOf(mode);
  }
}
