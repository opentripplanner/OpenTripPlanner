package org.opentripplanner.apis.transmodel.mapping;

import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.transmodel.TransmodelRequestContext;
import org.opentripplanner.apis.transmodel.model.plan.TripQuery;
import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.routing.api.request.RouteRequest;

public class TripRequestMapper {

  private final TripViaLocationMapper tripViaLocationMapper;
  private final GenericLocationMapper genericLocationMapper;
  private final TransitFilterNewWayMapper transitFilterNewWayMapper;
  private final TransitFilterOldWayMapper transitFilterOldWayMapper;
  private final FeedScopedIdMapper idMapper;

  public TripRequestMapper(FeedScopedIdMapper idMapper) {
    this.tripViaLocationMapper = new TripViaLocationMapper(idMapper);
    this.genericLocationMapper = new GenericLocationMapper(idMapper);
    this.transitFilterNewWayMapper = new TransitFilterNewWayMapper(idMapper);
    this.transitFilterOldWayMapper = new TransitFilterOldWayMapper(idMapper);
    this.idMapper = idMapper;
  }

  /**
   * Create a RouteRequest from the input fields of the trip query arguments.
   */
  public RouteRequest createRequest(DataFetchingEnvironment environment) {
    TransmodelRequestContext context = environment.getContext();
    var serverContext = context.getServerContext();
    var requestBuilder = serverContext.defaultRouteRequest().copyOf();

    DataFetcherDecorator callWith = new DataFetcherDecorator(environment);

    callWith.argument("from", (Map<String, Object> v) ->
      requestBuilder.withFrom(genericLocationMapper.toGenericLocation(v))
    );
    callWith.argument("to", (Map<String, Object> v) ->
      requestBuilder.withTo(genericLocationMapper.toGenericLocation(v))
    );
    callWith.argument("passThroughPoints", (List<Map<String, Object>> v) -> {
      requestBuilder.withViaLocations(tripViaLocationMapper.toLegacyPassThroughLocations(v));
    });
    callWith.argument(TripQuery.TRIP_VIA_PARAMETER, (List<Map<String, Object>> v) -> {
      requestBuilder.withViaLocations(tripViaLocationMapper.mapToViaLocations(v));
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
        callWith.argument("unpreferred.authorities", (Collection<String> authorities) ->
          transitBuilder.withUnpreferredAgencies(idMapper.parseListNullSafe(authorities))
        );

        callWith.argument("unpreferred.lines", (List<String> lines) ->
          transitBuilder.withUnpreferredRoutes(idMapper.parseListNullSafe(lines))
        );
        callWith.argument("banned.serviceJourneys", (Collection<String> serviceJourneys) ->
          transitBuilder.withBannedTrips(idMapper.parseListNullSafe(serviceJourneys))
        );

        if (GqlUtil.hasArgument(environment, "filters")) {
          transitBuilder.setFilters(
            transitFilterNewWayMapper.mapFilter(environment.getArgument("filters"))
          );
        } else {
          transitFilterOldWayMapper.mapFilter(environment, callWith, transitBuilder);
        }
      });

      if (GqlUtil.hasArgument(environment, "modes")) {
        journeyBuilder.setModes(
          RequestStreetModesMapper.mapRequestStreetModes(environment.getArgument("modes"))
        );
      }
    });

    requestBuilder.withPreferences(preferences ->
      PreferencesMapper.mapPreferences(environment, callWith, preferences)
    );

    return requestBuilder.buildRequest();
  }
}
