package org.opentripplanner.apis.transmodel.mapping;

import static org.opentripplanner.apis.transmodel.mapping.TransitIdMapper.mapIDsToDomainNullSafe;

import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.opentripplanner.apis.transmodel.TransmodelRequestContext;
import org.opentripplanner.apis.transmodel.model.plan.TripQuery;
import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.routing.api.request.RouteRequest;

public class TripRequestMapper {

  /**
   * Create a RouteRequest from the input fields of the trip query arguments.
   */
  public static RouteRequest createRequest(DataFetchingEnvironment environment) {
    TransmodelRequestContext context = environment.getContext();
    var serverContext = context.getServerContext();
    var requestBuilder = serverContext.defaultRouteRequest().copyOf();

    DataFetcherDecorator callWith = new DataFetcherDecorator(environment);

    callWith.argument("from", (Map<String, Object> v) ->
      requestBuilder.withFrom(GenericLocationMapper.toGenericLocation(v))
    );
    callWith.argument("to", (Map<String, Object> v) ->
      requestBuilder.withTo(GenericLocationMapper.toGenericLocation(v))
    );
    callWith.argument("passThroughPoints", (List<Map<String, Object>> v) -> {
      requestBuilder.withViaLocations(TripViaLocationMapper.toLegacyPassThroughLocations(v));
    });
    callWith.argument(TripQuery.TRIP_VIA_PARAMETER, (List<Map<String, Object>> v) -> {
      requestBuilder.withViaLocations(TripViaLocationMapper.mapToViaLocations(v));
    });

    callWith.argument("dateTime", millisSinceEpoch ->
      requestBuilder.withDateTime(Instant.ofEpochMilli((long) millisSinceEpoch))
    );

    callWith.argument("bookingTime", millisSinceEpoch ->
      requestBuilder.withBookingTime(Instant.ofEpochMilli((long) millisSinceEpoch))
    );

    callWith.argument("searchWindow", (Integer m) ->
      requestBuilder.withSearchWindow(Duration.ofMinutes(m))
    );
    callWith.argument("pageCursor", requestBuilder::withPageCursorFromEncoded);
    callWith.argument("timetableView", requestBuilder::withTimetableView);
    callWith.argument("numTripPatterns", requestBuilder::withNumItineraries);
    callWith.argument("arriveBy", requestBuilder::withArriveBy);

    requestBuilder.withJourney(journeyBuilder -> {
      callWith.argument("wheelchairAccessible", journeyBuilder::withWheelchair);

      journeyBuilder.withTransit(transitBuilder -> {
        callWith.argument("preferred.authorities", (Collection<String> authorities) ->
          transitBuilder.withPreferredAgencies(mapIDsToDomainNullSafe(authorities))
        );
        callWith.argument("unpreferred.authorities", (Collection<String> authorities) ->
          transitBuilder.withUnpreferredAgencies(mapIDsToDomainNullSafe(authorities))
        );

        callWith.argument("preferred.lines", (List<String> lines) ->
          transitBuilder.withPreferredRoutes(mapIDsToDomainNullSafe(lines))
        );
        callWith.argument("unpreferred.lines", (List<String> lines) ->
          transitBuilder.withUnpreferredRoutes(mapIDsToDomainNullSafe(lines))
        );
        callWith.argument("banned.serviceJourneys", (Collection<String> serviceJourneys) ->
          transitBuilder.withBannedTrips(mapIDsToDomainNullSafe(serviceJourneys))
        );

        if (GqlUtil.hasArgument(environment, "filters")) {
          transitBuilder.setFilters(
            FilterMapper.mapFilterNewWay(environment.getArgument("filters"))
          );
        } else {
          FilterMapper.mapFilterOldWay(environment, callWith, requestBuilder);
        }
      });

      if (GqlUtil.hasArgument(environment, "modes")) {
        journeyBuilder.setModes(
          RequestModesMapper.mapRequestModes(environment.getArgument("modes"))
        );
      }
    });

    requestBuilder.withPreferences(preferences ->
      PreferencesMapper.mapPreferences(environment, callWith, preferences)
    );

    return requestBuilder.buildRequest();
  }
}
