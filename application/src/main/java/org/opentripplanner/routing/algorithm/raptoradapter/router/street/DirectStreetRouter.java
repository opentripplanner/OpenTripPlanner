package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.ItinerariesHelper;
import org.opentripplanner.routing.algorithm.mapping.LegsToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.StreetPathToLegsMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.transit.service.TransitServiceResolver;

/**
 * Generates "direct" street routes, i.e. those that do not use transit and are on the street
 * network for the entire itinerary.
 *
 * @see DirectFlexRouter
 */
public class DirectStreetRouter {

  public static List<Itinerary> route(
    Graph graph,
    TransitService transitService,
    StreetLimitationParametersService streetLimitationParametersService,
    VehicleRentalService vehicleRentalService,
    StreetDetailsService streetDetailsService,
    @Nullable DataOverlayParameterBindings dataOverlayParameterBindings,
    RouteRequest request,
    LinkingContext linkingContext
  ) {
    if (request.journey().direct().mode() == StreetMode.NOT_SET) {
      return Collections.emptyList();
    }
    OTPRequestTimeoutException.checkForTimeout();
    try {
      var maxCarSpeed = streetLimitationParametersService.maxCarSpeed();
      if (!straightLineDistanceIsWithinLimit(request, maxCarSpeed, linkingContext)) {
        return Collections.emptyList();
      }

      // we could also get a persistent router-scoped GraphPathFinder but there's no setup cost here
      GraphPathFinder gpFinder = new GraphPathFinder(
        DataOverlayContext.listExtensionRequestContexts(request, dataOverlayParameterBindings),
        streetLimitationParametersService,
        vehicleRentalService
      );
      var paths = gpFinder.find(request, linkingContext);

      // Convert the internal GraphPaths to itineraries
      final StreetPathToLegsMapper streetPathToLegsMapper = new StreetPathToLegsMapper(
        new TransitServiceResolver(transitService),
        transitService.getTimeZone(),
        graph.streetNotesService,
        streetDetailsService,
        graph.ellipsoidToGeoidDifference
      );
      List<Itinerary> itineraries = new ArrayList<>();
      for (var path : paths) {
        var legs = streetPathToLegsMapper.map(path, request);
        var itinerary = LegsToItineraryMapper.map(
          legs,
          path.lastState().isRentingVehicleFromStation(),
          path.calculateElevations()
        );
        itinerary.ifPresent(itineraries::add);
      }
      return ItinerariesHelper.decorateItinerariesWithRequestData(
        itineraries,
        request.journey().wheelchair(),
        request.preferences().wheelchair()
      );
    } catch (PathNotFoundException e) {
      return Collections.emptyList();
    }
  }

  private static boolean straightLineDistanceIsWithinLimit(
    RouteRequest request,
    float maxCarSpeed,
    LinkingContext linkingContext
  ) {
    // TODO This currently only calculates the distances between the first fromVertex
    //      and the first toVertex
    double distance = SphericalDistanceLibrary.distance(
      linkingContext.findVertices(request.from()).iterator().next().getCoordinate(),
      linkingContext.findVertices(request.to()).iterator().next().getCoordinate()
    );
    return distance < calculateDistanceMaxLimit(request, maxCarSpeed);
  }

  /**
   * Calculates the maximum distance in meters based on the maxDirectStreetDuration and the
   * fastest mode available. This assumes that it is not possible to exceed the speed defined in the
   * RouteRequest.
   */
  private static double calculateDistanceMaxLimit(RouteRequest request, float maxCarSpeed) {
    var preferences = request.preferences();
    double distanceLimit;
    StreetMode mode = request.journey().direct().mode();

    double durationLimit = preferences.street().maxDirectDuration().valueOf(mode).toSeconds();

    if (mode.includesDriving()) {
      distanceLimit = durationLimit * maxCarSpeed;
    } else if (mode.includesBiking()) {
      distanceLimit = durationLimit * preferences.bike().speed();
    } else if (mode.includesScooter()) {
      distanceLimit = durationLimit * preferences.scooter().speed();
    } else if (mode.includesWalking()) {
      distanceLimit = durationLimit * preferences.walk().speed();
    } else {
      throw new IllegalStateException("Could not set max limit for StreetMode");
    }

    return distanceLimit;
  }
}
