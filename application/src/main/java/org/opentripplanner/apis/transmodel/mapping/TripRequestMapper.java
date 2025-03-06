package org.opentripplanner.apis.transmodel.mapping;

import static org.opentripplanner.apis.transmodel.mapping.TransitIdMapper.mapIDsToDomainNullSafe;

import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.opentripplanner.apis.transmodel.TransmodelRequestContext;
import org.opentripplanner.apis.transmodel.model.plan.TripQuery;
import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TripRequestMapper {

  /**
   * Create a RouteRequest from the input fields of the trip query arguments.
   */
  public static RouteRequest createRequest(DataFetchingEnvironment environment) {
    TransmodelRequestContext context = environment.getContext();
    OtpServerRequestContext serverContext = context.getServerContext();
    RouteRequest request = serverContext.defaultRouteRequest();

    DataFetcherDecorator callWith = new DataFetcherDecorator(environment);

    callWith.argument("locale", (String v) -> request.setLocale(Locale.forLanguageTag(v)));

    callWith.argument("from", (Map<String, Object> v) ->
      request.setFrom(GenericLocationMapper.toGenericLocation(v))
    );
    callWith.argument("to", (Map<String, Object> v) ->
      request.setTo(GenericLocationMapper.toGenericLocation(v))
    );
    callWith.argument("passThroughPoints", (List<Map<String, Object>> v) -> {
      request.setViaLocations(TripViaLocationMapper.toLegacyPassThroughLocations(v));
    });
    callWith.argument(TripQuery.TRIP_VIA_PARAMETER, (List<Map<String, Object>> v) -> {
      request.setViaLocations(TripViaLocationMapper.mapToViaLocations(v));
    });

    callWith.argument("dateTime", millisSinceEpoch ->
      request.setDateTime(Instant.ofEpochMilli((long) millisSinceEpoch))
    );

    callWith.argument("bookingTime", millisSinceEpoch ->
      request.setBookingTime(Instant.ofEpochMilli((long) millisSinceEpoch))
    );

    callWith.argument("searchWindow", (Integer m) -> request.setSearchWindow(Duration.ofMinutes(m))
    );
    callWith.argument("pageCursor", request::setPageCursorFromEncoded);
    callWith.argument("timetableView", request::setTimetableView);
    callWith.argument("wheelchairAccessible", request::setWheelchair);
    callWith.argument("numTripPatterns", request::setNumItineraries);
    callWith.argument("arriveBy", request::setArriveBy);

    callWith.argument("preferred.authorities", (Collection<String> authorities) ->
      request.journey().transit().setPreferredAgencies(mapIDsToDomainNullSafe(authorities))
    );
    callWith.argument("unpreferred.authorities", (Collection<String> authorities) ->
      request.journey().transit().setUnpreferredAgencies(mapIDsToDomainNullSafe(authorities))
    );

    callWith.argument("preferred.lines", (List<String> lines) ->
      request.journey().transit().setPreferredRoutes(mapIDsToDomainNullSafe(lines))
    );
    callWith.argument("unpreferred.lines", (List<String> lines) ->
      request.journey().transit().setUnpreferredRoutes(mapIDsToDomainNullSafe(lines))
    );

    if (GqlUtil.hasArgument(environment, "modes")) {
      request
        .journey()
        .setModes(RequestModesMapper.mapRequestModes(environment.getArgument("modes")));
    }

    var bannedTrips = new ArrayList<FeedScopedId>();
    callWith.argument("banned.serviceJourneys", (Collection<String> serviceJourneys) ->
      bannedTrips.addAll(mapIDsToDomainNullSafe(serviceJourneys))
    );
    if (!bannedTrips.isEmpty()) {
      request.journey().transit().setBannedTrips(bannedTrips);
    }

    if (GqlUtil.hasArgument(environment, "filters")) {
      request
        .journey()
        .transit()
        .setFilters(FilterMapper.mapFilterNewWay(environment.getArgument("filters")));
    } else {
      FilterMapper.mapFilterOldWay(environment, callWith, request);
    }

    request.withPreferences(preferences ->
      PreferencesMapper.mapPreferences(environment, callWith, preferences)
    );

    return request;
  }
}
