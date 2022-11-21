package org.opentripplanner.ext.siri;

import static org.opentripplanner.ext.siri.SiriTransportModeMapper.mapTransitMainMode;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NO_START_DATE;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.UpdateError;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
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

    routeBuilder.withShortName(getFirstNameFromList(names));

    return routeBuilder.build();
  }

  public static void getStopTime(StopLocation stop) {}

  public static String getFirstNameFromList(List<NaturalLanguageStringStructure> names) {
    if (isNotNullOrEmpty(names)) {
      return names.stream().findFirst().map(NaturalLanguageStringStructure::getValue).orElse("");
    }
    return "";
  }

  public static Result<Trip, UpdateError> getTrip(
    FeedScopedId tripId,
    Route route,
    Operator operator,
    T2<TransitMode, String> transitMode,
    List<NaturalLanguageStringStructure> destinationNames,
    LocalDate serviceDate,
    FeedScopedId calServiceId
  ) {
    var tripBuilder = Trip.of(tripId);
    tripBuilder.withRoute(route);

    // Explicitly set TransitMode on Trip - in case it differs from Route
    tripBuilder.withMode(transitMode.first);
    tripBuilder.withNetexSubmode(transitMode.second);

    if (serviceDate == null) {
      return UpdateError.result(tripId, NO_START_DATE);
    }

    if (calServiceId == null) {
      return UpdateError.result(tripId, NO_START_DATE);
    }

    tripBuilder.withServiceId(calServiceId);

    // Use destinationName as default headsign - if provided
    tripBuilder.withHeadsign(new NonLocalizedString(getFirstNameFromList(destinationNames)));

    tripBuilder.withOperator(operator);

    // TODO - SIRI: Populate these?
    tripBuilder.withShapeId(null); // Replacement-trip has different shape
    //        trip.setTripPrivateCode(null);
    //        trip.setTripPublicCode(null);
    tripBuilder.withGtfsBlockId(null);
    tripBuilder.withShortName(null);
    //        trip.setKeyValues(null);

    return Result.success(tripBuilder.build());
  }

  /**
   * Returns the aimedArrival and aimedDeparture times based on whether:
   * 1. it is the first stop - realtime data for arrivals to the first stop are generally poor,
   *    thus we use the departure time for both arrival and departure to avoid negative running times
   * 2. it is the last stop - realtime data for depatures from the last stop are generally poor
   *    thus we use the arrival time for both arrival and departure to avoid negative running times
   * 3. it is an intermediate stop - returns the times as is.
   *
   * @param aimedArrivalTime The planned arrival time in seconds from midnight
   * @param aimedDepartureTime The planned depature time in seconds from midnight
   * @param stopIndex the index of the current stop
   * @param numStops the total number of stops in the journey
   * @return a tuple with the aimedArrivalTime as first and the aimedDepartureTime as second.
   */
  public static T2<Integer, Integer> getTimeForStop(
    int aimedArrivalTime,
    int aimedDepartureTime,
    int stopIndex,
    int numStops
  ) {
    boolean isFirstStop = stopIndex == 0;
    boolean isLastStop = stopIndex == (numStops - 1);

    if (isFirstStop) {
      return new T2<>(aimedDepartureTime, aimedDepartureTime);
    } else if (isLastStop) {
      return new T2<>(aimedArrivalTime, aimedArrivalTime);
    } else {
      return new T2<>(aimedArrivalTime, aimedDepartureTime);
    }
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
