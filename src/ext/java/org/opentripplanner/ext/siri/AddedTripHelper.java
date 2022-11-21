package org.opentripplanner.ext.siri;

import static org.opentripplanner.ext.siri.SiriTransportModeMapper.mapTransitMainMode;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.NaturalLanguageStringStructure;
import uk.org.siri.siri20.VehicleModesEnumeration;

public class AddedTripHelper {

  private static final Logger LOG = LoggerFactory.getLogger(AddedTripHelper.class);

  /**
   * Method to create a Route. Commonly used to create a route if a realtime message
   * refers to a route that is not in the transit model.
   *
   * We will find the first Route with same Operator, and use the same Authority
   * If no operator found, copy the agency from replaced route
   *
   * If no name is given for the route, an empty string will be set as the name.
   *
   * @param routes routes currently in the transit model
   * @param names the published names for the route. Only the first element in the list is used
   * @param operator the operator for this line
   * @param replacedRoute the original route if it exists
   * @param routeId id of the route
   * @param transitMode the mode of the route
   * @return a new Route
   */
  public static Route getRoute(
    Collection<Route> routes,
    List<NaturalLanguageStringStructure> names,
    Operator operator,
    Route replacedRoute,
    FeedScopedId routeId,
    T2<TransitMode, String> transitMode
  ) {
    var routeBuilder = Route.of(routeId);
    routeBuilder.withMode(transitMode.first);
    routeBuilder.withNetexSubmode(transitMode.second);
    routeBuilder.withOperator(operator);

    // TODO - SIRI: Is there a better way to find authority/Agency?
    Agency agency = routes
      .stream()
      .filter(route1 ->
        route1 != null && route1.getOperator() != null && route1.getOperator().equals(operator)
      )
      .findFirst()
      .map(Route::getAgency)
      .orElseGet(replacedRoute::getAgency);
    routeBuilder.withAgency(agency);

    if (isNotNullOrEmpty(names)) {
      var name = names
        .stream()
        .findFirst()
        .map(NaturalLanguageStringStructure::getValue)
        .orElse("");
      routeBuilder.withShortName(name);
    }

    return routeBuilder.build();
  }

  private static boolean isNotNullOrEmpty(List<NaturalLanguageStringStructure> list) {
    return list != null;
  }

  /**
   * Resolves TransitMode from SIRI VehicleMode
   */
  public static T2<TransitMode, String> getTransitMode(
    List<VehicleModesEnumeration> vehicleModes,
    Route replacedRoute
  ) {
    TransitMode transitMode = mapTransitMainMode(vehicleModes);

    String transitSubMode = resolveTransitSubMode(transitMode, replacedRoute);

    return new T2<>(transitMode, transitSubMode);
  }

  /**
   * Resolves submode based on added trips's mode and replacedRoute's mode
   *
   * @param transitMode   Mode of the added trip
   * @param replacedRoute Route that is being replaced
   * @return String-representation of submode
   */
  private static String resolveTransitSubMode(TransitMode transitMode, Route replacedRoute) {
    if (replacedRoute != null) {
      TransitMode replacedRouteMode = replacedRoute.getMode();

      if (replacedRouteMode == TransitMode.RAIL) {
        if (transitMode.equals(TransitMode.RAIL)) {
          // Replacement-route is also RAIL
          return RailSubmodeEnumeration.REPLACEMENT_RAIL_SERVICE.value();
        } else if (transitMode.equals(TransitMode.BUS)) {
          // Replacement-route is BUS
          return BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS.value();
        }
      }
    }
    return null;
  }
}
