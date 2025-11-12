package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.ItinerariesHelper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * Generates "direct" street routes, i.e. those that do not use transit and are on the street
 * network for the entire itinerary, that go through via locations.
 *
 * TODO Currently lacks support for delaying rental return or parking after first via.
 */
public class ViaDirectStreetRouter implements DirectStreetRouter {

  public List<Itinerary> route(
    OtpServerRequestContext serverContext,
    RouteRequest request,
    LinkingContext linkingContext
  ) {
    // No support for pass-through locations or visit via locations with just stops as they force
    // you to use transit.
    var vias = request.listViaLocationsWithCoordinates();
    if (request.listViaLocations().size() != vias.size()) {
      return Collections.emptyList();
    }
    if (request.journey().direct().mode() == StreetMode.NOT_SET) {
      return Collections.emptyList();
    }

    var maxCarSpeed = serverContext.streetLimitationParametersService().maxCarSpeed();
    if (!straightLineDistanceIsWithinLimit(request, maxCarSpeed, linkingContext)) {
      return Collections.emptyList();
    }

    OTPRequestTimeoutException.checkForTimeout();
    try {
      // we could also get a persistent router-scoped GraphPathFinder but there's no setup cost here
      GraphPathFinder gpFinder = new GraphPathFinder(
        serverContext.traverseVisitor(),
        serverContext.listExtensionRequestContexts(request),
        maxCarSpeed
      );
      var firstRequest = request.copyOf().withTo(vias.getFirst()).buildRequest();
      List<GraphPath<State, Edge, Vertex>> paths = new ArrayList<>(
        gpFinder.graphPathFinderEntryPoint(firstRequest, linkingContext)
      );

      var mode = request.journey().direct().mode();
      var newMode = getStreetRequestAfterFirstVia(mode);
      var requestWithNewMode = request
        .copyOf()
        .withJourney(journeyRequestBuilder -> journeyRequestBuilder.withDirect(newMode))
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

      var lastLocations = new ArrayList<>(vias);
      lastLocations.add(request.to());
      for (int i = 0; i < lastLocations.size() - 1; i++) {
        var from = lastLocations.get(i);
        var to = lastLocations.get(i + 1);
        var minimumWaitTime = request.listViaLocations().get(i).minimumWaitTime();
        var newStartTime = Instant.ofEpochSecond(paths.getFirst().getEndTime()).plus(
          minimumWaitTime
        );
        var lastDuration = minimumWaitTime.plusSeconds(paths.getFirst().getDuration());
        var patchedRequest = requestWithNewMode
          .copyOf()
          .withFrom(from)
          .withTo(to)
          .withDateTime(newStartTime)
          .withPreferences(preferences ->
            preferences.withStreet(street ->
              street.withMaxDirectDuration(streetModeBuilder ->
                streetModeBuilder.with(
                  newMode.mode(),
                  request
                    .preferences()
                    .street()
                    .maxDirectDuration()
                    .valueOf(mode)
                    .minus(lastDuration)
                )
              )
            )
          )
          .buildRequest();
        var lastState = paths.getFirst().states.getLast();
        paths = gpFinder.graphPathFinderEntryPoint(patchedRequest, linkingContext, lastState);
      }

      // Convert the internal GraphPaths to itineraries
      final GraphPathToItineraryMapper graphPathToItineraryMapper = new GraphPathToItineraryMapper(
        serverContext.transitService()::getRegularStop,
        serverContext.transitService().getTimeZone(),
        serverContext.graph().streetNotesService,
        serverContext.graph().ellipsoidToGeoidDifference
      );
      List<Itinerary> response = graphPathToItineraryMapper.mapItineraries(paths);
      response = ItinerariesHelper.decorateItinerariesWithRequestData(
        response,
        request.journey().wheelchair(),
        request.preferences().wheelchair()
      );
      return response;
    } catch (PathNotFoundException e) {
      return Collections.emptyList();
    }
  }

  /**
   * This uses the total straight line distance between locations (not just the distance between
   * from and to).
   */
  private static boolean straightLineDistanceIsWithinLimit(
    RouteRequest request,
    float maxCarSpeed,
    LinkingContext linkingContext
  ) {
    // TODO This currently only calculates the distances between the first vertex from each
    //  location
    double distance = SphericalDistanceLibrary.distance(
      linkingContext.findVertices(request.from()).iterator().next().getCoordinate(),
      linkingContext
        .findVertices(request.listViaLocationsWithCoordinates().getFirst())
        .iterator()
        .next()
        .getCoordinate()
    );
    for (int i = 0; i < request.listViaLocationsWithCoordinates().size() - 1; i++) {
      distance += SphericalDistanceLibrary.distance(
        linkingContext
          .findVertices(request.listViaLocationsWithCoordinates().get(i))
          .iterator()
          .next()
          .getCoordinate(),
        linkingContext
          .findVertices(request.listViaLocationsWithCoordinates().get(i + 1))
          .iterator()
          .next()
          .getCoordinate()
      );
    }
    distance += SphericalDistanceLibrary.distance(
      linkingContext
        .findVertices(request.listViaLocationsWithCoordinates().getLast())
        .iterator()
        .next()
        .getCoordinate(),
      linkingContext.findVertices(request.to()).iterator().next().getCoordinate()
    );
    return distance < request.getMaximumDirectDistance(maxCarSpeed);
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
}
