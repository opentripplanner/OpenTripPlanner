package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.opentripplanner.apis.gtfs.mapping.routerequest.BicyclePreferencesMapper.setBicyclePreferences;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.CarPreferencesMapper.setCarPreferences;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.ModePreferencesMapper.setModes;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.ScooterPreferencesMapper.setScooterPreferences;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.TransitPreferencesMapper.setTransitPreferences;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.WalkPreferencesMapper.setWalkPreferences;

import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.time.DurationUtils;

public class RouteRequestMapper {

  public static RouteRequest toRouteRequest(
    DataFetchingEnvironment environment,
    GraphQLRequestContext context
  ) {
    RouteRequest request = context.defaultRouteRequest();
    var args = new GraphQLTypes.GraphQLQueryTypePlanConnectionArgs(environment.getArguments());
    var dateTime = args.getGraphQLDateTime();
    if (dateTime.getGraphQLEarliestDeparture() != null) {
      request.setDateTime(args.getGraphQLDateTime().getGraphQLEarliestDeparture().toInstant());
    } else if (dateTime.getGraphQLLatestArrival() != null) {
      request.setDateTime(args.getGraphQLDateTime().getGraphQLLatestArrival().toInstant());
      request.setArriveBy(true);
    } else {
      request.setDateTime(Instant.now());
    }
    request.setFrom(parseGenericLocation(args.getGraphQLOrigin()));
    request.setTo(parseGenericLocation(args.getGraphQLDestination()));
    request.setLocale(GraphQLUtils.getLocale(environment, args.getGraphQLLocale()));
    request.setSearchWindow(
      args.getGraphQLSearchWindow() != null
        ? DurationUtils.requireNonNegativeMax2days(args.getGraphQLSearchWindow(), "searchWindow")
        : null
    );

    if (args.getGraphQLBefore() != null) {
      request.setPageCursorFromEncoded(args.getGraphQLBefore());
      if (args.getGraphQLLast() != null) {
        request.setNumItineraries(args.getGraphQLLast());
      }
    } else if (args.getGraphQLAfter() != null) {
      request.setPageCursorFromEncoded(args.getGraphQLAfter());
      if (args.getGraphQLFirst() != null) {
        request.setNumItineraries(args.getGraphQLFirst());
      }
    } else if (args.getGraphQLFirst() != null) {
      request.setNumItineraries(args.getGraphQLFirst());
    }

    request.withPreferences(preferences -> setPreferences(preferences, request, args, environment));

    setModes(request.journey(), args.getGraphQLModes(), environment);

    // sadly we need to use the raw collection because it is cast to the wrong type
    mapViaPoints(request, environment.getArgument("via"));
    return request;
  }

  private static void setPreferences(
    RoutingPreferences.Builder prefs,
    RouteRequest request,
    GraphQLTypes.GraphQLQueryTypePlanConnectionArgs args,
    DataFetchingEnvironment environment
  ) {
    var preferenceArgs = args.getGraphQLPreferences();
    prefs.withItineraryFilter(filters ->
      setItineraryFilters(filters, args.getGraphQLItineraryFilter())
    );
    prefs.withTransit(transit -> {
      prefs.withTransfer(transfer -> setTransitPreferences(transit, transfer, args, environment));
    });
    setStreetPreferences(prefs, request, preferenceArgs.getGraphQLStreet(), environment);
    setAccessibilityPreferences(request, preferenceArgs.getGraphQLAccessibility());
  }

  private static void setItineraryFilters(
    ItineraryFilterPreferences.Builder filterPreferences,
    GraphQLTypes.GraphQLPlanItineraryFilterInput filters
  ) {
    if (filters.getGraphQLItineraryFilterDebugProfile() != null) {
      filterPreferences.withDebug(
        ItineraryFilterDebugProfileMapper.map(filters.getGraphQLItineraryFilterDebugProfile())
      );
    }
    if (filters.getGraphQLGroupSimilarityKeepOne() != null) {
      filterPreferences.withGroupSimilarityKeepOne(filters.getGraphQLGroupSimilarityKeepOne());
    }
    if (filters.getGraphQLGroupSimilarityKeepThree() != null) {
      filterPreferences.withGroupSimilarityKeepThree(filters.getGraphQLGroupSimilarityKeepThree());
    }
    if (filters.getGraphQLGroupedOtherThanSameLegsMaxCostMultiplier() != null) {
      filterPreferences.withGroupedOtherThanSameLegsMaxCostMultiplier(
        filters.getGraphQLGroupedOtherThanSameLegsMaxCostMultiplier()
      );
    }
  }

  private static void setStreetPreferences(
    RoutingPreferences.Builder preferences,
    RouteRequest request,
    GraphQLTypes.GraphQLPlanStreetPreferencesInput args,
    DataFetchingEnvironment environment
  ) {
    setRentalAvailabilityPreferences(preferences, request);

    if (args == null) {
      return;
    }

    preferences.withBike(bicycle ->
      setBicyclePreferences(bicycle, args.getGraphQLBicycle(), environment)
    );
    preferences.withCar(car -> setCarPreferences(car, args.getGraphQLCar(), environment));
    preferences.withScooter(scooter -> setScooterPreferences(scooter, args.getGraphQLScooter()));
    preferences.withWalk(walk -> setWalkPreferences(walk, args.getGraphQLWalk()));
  }

  private static void setRentalAvailabilityPreferences(
    RoutingPreferences.Builder preferences,
    RouteRequest request
  ) {
    preferences.withBike(bike ->
      bike.withRental(rental -> rental.withUseAvailabilityInformation(request.isTripPlannedForNow())
      )
    );
    preferences.withCar(car ->
      car.withRental(rental -> rental.withUseAvailabilityInformation(request.isTripPlannedForNow()))
    );
    preferences.withScooter(scooter ->
      scooter.withRental(rental ->
        rental.withUseAvailabilityInformation(request.isTripPlannedForNow())
      )
    );
  }

  private static void setAccessibilityPreferences(
    RouteRequest request,
    GraphQLTypes.GraphQLAccessibilityPreferencesInput preferenceArgs
  ) {
    if (preferenceArgs != null && preferenceArgs.getGraphQLWheelchair() != null) {
      request.setWheelchair(preferenceArgs.getGraphQLWheelchair().getGraphQLEnabled());
    }
  }

  private static GenericLocation parseGenericLocation(
    GraphQLTypes.GraphQLPlanLabeledLocationInput locationInput
  ) {
    var stopLocation = locationInput.getGraphQLLocation().getGraphQLStopLocation();
    if (stopLocation.getGraphQLStopLocationId() != null) {
      var stopId = stopLocation.getGraphQLStopLocationId();
      if (FeedScopedId.isValidString(stopId)) {
        return new GenericLocation(
          locationInput.getGraphQLLabel(),
          FeedScopedId.parse(stopId),
          null,
          null
        );
      } else {
        throw new IllegalArgumentException("Stop id %s is not of valid format.".formatted(stopId));
      }
    }

    var coordinate = locationInput.getGraphQLLocation().getGraphQLCoordinate();
    return new GenericLocation(
      locationInput.getGraphQLLabel(),
      null,
      coordinate.getGraphQLLatitude(),
      coordinate.getGraphQLLongitude()
    );
  }

  static void mapViaPoints(RouteRequest request, List<Map<String, Map<String, Object>>> via) {
    request.setViaLocations(ViaLocationMapper.mapToViaLocations(via));
  }
}
