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
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteRequestBuilder;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferencesBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.time.DurationUtils;

public class RouteRequestMapper {

  public static RouteRequest toRouteRequest(
    DataFetchingEnvironment environment,
    GraphQLRequestContext context
  ) {
    var request = context.defaultRouteRequest().copyOf();
    var args = new GraphQLTypes.GraphQLQueryTypePlanConnectionArgs(environment.getArguments());
    var dateTime = args.getGraphQLDateTime();

    if (dateTime.getGraphQLEarliestDeparture() != null) {
      request.withDateTime(args.getGraphQLDateTime().getGraphQLEarliestDeparture().toInstant());
    } else if (dateTime.getGraphQLLatestArrival() != null) {
      request.withDateTime(args.getGraphQLDateTime().getGraphQLLatestArrival().toInstant());
      request.withArriveBy(true);
    } else {
      request.withDateTime(Instant.now());
    }

    boolean isTripPlannedForNow = RouteRequest.isAPIGtfsTripPlannedForNow(request.dateTime());

    request.withFrom(parseGenericLocation(args.getGraphQLOrigin()));
    request.withTo(parseGenericLocation(args.getGraphQLDestination()));
    request.withSearchWindow(
      args.getGraphQLSearchWindow() != null
        ? DurationUtils.requireNonNegativeMax2days(args.getGraphQLSearchWindow(), "searchWindow")
        : null
    );

    if (args.getGraphQLBefore() != null) {
      request.withPageCursorFromEncoded(args.getGraphQLBefore());
      if (args.getGraphQLLast() != null) {
        request.withNumItineraries(args.getGraphQLLast());
      }
    } else if (args.getGraphQLAfter() != null) {
      request.withPageCursorFromEncoded(args.getGraphQLAfter());
      if (args.getGraphQLFirst() != null) {
        request.withNumItineraries(args.getGraphQLFirst());
      }
    } else if (args.getGraphQLFirst() != null) {
      request.withNumItineraries(args.getGraphQLFirst());
    }

    request.withPreferences(preferences ->
      setPreferences(preferences, request, isTripPlannedForNow, args, environment)
    );

    request.withJourney(journeyRequestBuilder ->
      setModes(journeyRequestBuilder, args, environment)
    );

    // sadly we need to use the raw collection because it is cast to the wrong type
    mapViaPoints(request, environment.getArgument("via"));

    return request.buildRequest();
  }

  private static void setPreferences(
    RoutingPreferencesBuilder prefs,
    RouteRequestBuilder requestBuilder,
    boolean isTripPlannedForNow,
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
    setStreetPreferences(
      prefs,
      isTripPlannedForNow,
      preferenceArgs.getGraphQLStreet(),
      environment
    );
    setAccessibilityPreferences(requestBuilder, preferenceArgs.getGraphQLAccessibility());
    prefs.withLocale(GraphQLUtils.getLocale(environment, args.getGraphQLLocale()));
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
    RoutingPreferencesBuilder preferences,
    boolean isTripPlannedForNow,
    @Nullable GraphQLTypes.GraphQLPlanStreetPreferencesInput args,
    DataFetchingEnvironment environment
  ) {
    setRentalAvailabilityPreferences(preferences, isTripPlannedForNow);

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
    RoutingPreferencesBuilder preferences,
    boolean isTripPlannedForNow
  ) {
    preferences.withBike(bike ->
      bike.withRental(rental -> rental.withUseAvailabilityInformation(isTripPlannedForNow))
    );
    preferences.withCar(car ->
      car.withRental(rental -> rental.withUseAvailabilityInformation(isTripPlannedForNow))
    );
    preferences.withScooter(scooter ->
      scooter.withRental(rental -> rental.withUseAvailabilityInformation(isTripPlannedForNow))
    );
  }

  private static void setAccessibilityPreferences(
    RouteRequestBuilder requestBuilder,
    @Nullable GraphQLTypes.GraphQLAccessibilityPreferencesInput preferenceArgs
  ) {
    if (preferenceArgs != null && preferenceArgs.getGraphQLWheelchair() != null) {
      requestBuilder.withJourney(j ->
        j.withWheelchair(preferenceArgs.getGraphQLWheelchair().getGraphQLEnabled())
      );
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

  static void mapViaPoints(RouteRequestBuilder request, List<Map<String, Object>> via) {
    request.withViaLocations(ViaLocationMapper.mapToViaLocations(via));
  }
}
