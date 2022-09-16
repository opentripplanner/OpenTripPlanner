package org.opentripplanner.ext.transmodelapi;

import static org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper.mapIDsToDomain;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper;
import org.opentripplanner.ext.transmodelapi.model.PlanResponse;
import org.opentripplanner.ext.transmodelapi.model.TransmodelTransportSubmode;
import org.opentripplanner.ext.transmodelapi.model.TransportModeSlack;
import org.opentripplanner.ext.transmodelapi.model.plan.ItineraryFiltersInputType;
import org.opentripplanner.ext.transmodelapi.support.DataFetcherDecorator;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.algorithm.mapping.TripPlanMapper;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RequestModesBuilder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransmodelGraphQLPlanner {

  private static final Logger LOG = LoggerFactory.getLogger(TransmodelGraphQLPlanner.class);

  public DataFetcherResult<PlanResponse> plan(DataFetchingEnvironment environment) {
    PlanResponse response = new PlanResponse();
    TransmodelRequestContext ctx = environment.getContext();
    OtpServerRequestContext serverContext = ctx.getServerContext();
    RouteRequest request = null;
    try {
      request = createRequest(environment);
      RoutingResponse res = ctx.getRoutingService().route(request);

      response.plan = res.getTripPlan();
      response.metadata = res.getMetadata();
      response.messages = res.getRoutingErrors();
      response.debugOutput = res.getDebugTimingAggregator().finishedRendering();
      response.previousPageCursor = res.getPreviousPageCursor();
      response.nextPageCursor = res.getNextPageCursor();
    } catch (Exception e) {
      LOG.error("System error: " + e.getMessage(), e);
      response.plan = TripPlanMapper.mapTripPlan(request, List.of());
      response.messages.add(new RoutingError(RoutingErrorCode.SYSTEM_ERROR, null));
    }
    Locale locale = request == null ? serverContext.defaultLocale() : request.locale();
    return DataFetcherResult
      .<PlanResponse>newResult()
      .data(response)
      .localContext(Map.of("locale", locale))
      .build();
  }

  private GenericLocation toGenericLocation(Map<String, Object> m) {
    Map<String, Object> coordinates = (Map<String, Object>) m.get("coordinates");
    Double lat = null;
    Double lon = null;
    if (coordinates != null) {
      lat = (Double) coordinates.get("latitude");
      lon = (Double) coordinates.get("longitude");
    }

    String placeRef = (String) m.get("place");
    FeedScopedId stopId = placeRef == null ? null : TransitIdMapper.mapIDToDomain(placeRef);
    String name = (String) m.get("name");
    name = name == null ? "" : name;

    return new GenericLocation(name, stopId, lat, lon);
  }

  private RouteRequest createRequest(DataFetchingEnvironment environment) {
    TransmodelRequestContext context = environment.getContext();
    OtpServerRequestContext serverContext = context.getServerContext();
    RouteRequest request = serverContext.defaultRouteRequest();
    RoutingPreferences preferences = request.preferences();

    DataFetcherDecorator callWith = new DataFetcherDecorator(environment);

    callWith.argument("locale", (String v) -> request.setLocale(Locale.forLanguageTag(v)));

    callWith.argument("from", (Map<String, Object> v) -> request.setFrom(toGenericLocation(v)));
    callWith.argument("to", (Map<String, Object> v) -> request.setTo(toGenericLocation(v)));

    callWith.argument(
      "dateTime",
      millisSinceEpoch -> request.setDateTime(Instant.ofEpochMilli((long) millisSinceEpoch))
    );
    callWith.argument(
      "searchWindow",
      (Integer m) -> request.setSearchWindow(Duration.ofMinutes(m))
    );
    callWith.argument("pageCursor", request::setPageCursorFromEncoded);
    callWith.argument("timetableView", request::setTimetableView);
    callWith.argument("wheelchairAccessible", request::setWheelchair);
    callWith.argument("numTripPatterns", request::setNumItineraries);
    //        callWith.argument("maxTransferWalkDistance", request::setMaxTransferWalkDistance);
    //        callWith.argument("preTransitReluctance", (Double v) ->  request.setPreTransitReluctance(v));
    //        callWith.argument("maxPreTransitWalkDistance", (Double v) ->  request.setMaxPreTransitWalkDistance(v));
    callWith.argument("walkBoardCost", preferences.walk()::setBoardCost);
    callWith.argument("walkReluctance", preferences::setNonTransitReluctance);
    callWith.argument("waitReluctance", preferences.transfer()::setWaitReluctance);
    callWith.argument("waitAtBeginningFactor", preferences.transfer()::setWaitAtBeginningFactor);
    callWith.argument("walkSpeed", preferences.walk()::setSpeed);
    callWith.argument("bikeSpeed", preferences.bike()::setSpeed);
    callWith.argument("bikeSwitchTime", preferences.bike()::setSwitchTime);
    callWith.argument("bikeSwitchCost", preferences.bike()::setSwitchCost);
    //        callWith.argument("transitDistanceReluctance", (Double v) -> request.transitDistanceReluctance = v);

    BicycleOptimizeType bicycleOptimizeType = environment.getArgument("bicycleOptimisationMethod");

    if (bicycleOptimizeType == BicycleOptimizeType.TRIANGLE) {
      // Arguments: [ time, slope, safety ]
      final double[] args = new double[3];

      callWith.argument("triangleFactors.time", (Double v) -> args[0] = v);
      callWith.argument("triangleFactors.slope", (Double v) -> args[1] = v);
      callWith.argument("triangleFactors.safety", (Double v) -> args[2] = v);

      preferences.bike().initOptimizeTriangle(args[0], args[1], args[2]);
    }

    if (bicycleOptimizeType != null) {
      preferences.bike().setOptimizeType(bicycleOptimizeType);
    }

    callWith.argument("arriveBy", request::setArriveBy);
    // TODO VIA (Skånetrafiken): 2022-08-24 refactor
    //    callWith.argument(
    //      "vias",
    //      (List<Map<String, Object>> v) ->
    //        request.intermediatePlaces =
    //          v.stream().map(this::toGenericLocation).collect(Collectors.toList())
    //    );

    callWith.argument(
      "preferred.authorities",
      (Collection<String> authorities) ->
        request.journey().transit().setPreferredAgencies(mapIDsToDomain(authorities))
    );
    callWith.argument(
      "unpreferred.authorities",
      (Collection<String> authorities) ->
        request.journey().transit().setUnpreferredAgencies(mapIDsToDomain(authorities))
    );
    callWith.argument(
      "whiteListed.authorities",
      (Collection<String> authorities) ->
        request.journey().transit().setWhiteListedAgencies(mapIDsToDomain(authorities))
    );
    callWith.argument(
      "banned.authorities",
      (Collection<String> authorities) ->
        request.journey().transit().setBannedAgencies(mapIDsToDomain(authorities))
    );

    callWith.argument(
      "preferred.otherThanPreferredLinesPenalty",
      preferences.transit()::setOtherThanPreferredRoutesPenalty
    );
    callWith.argument(
      "preferred.lines",
      (List<String> lines) -> request.journey().transit().setPreferredRoutes(mapIDsToDomain(lines))
    );
    callWith.argument(
      "unpreferred.lines",
      (List<String> lines) ->
        request.journey().transit().setUnpreferredRoutes(mapIDsToDomain(lines))
    );
    callWith.argument(
      "whiteListed.lines",
      (List<String> lines) ->
        request
          .journey()
          .transit()
          .setWhiteListedRoutes(RouteMatcher.idMatcher(mapIDsToDomain(lines)))
    );
    callWith.argument(
      "banned.lines",
      (List<String> lines) ->
        request.journey().transit().setBannedRoutes(RouteMatcher.idMatcher(mapIDsToDomain(lines)))
    );
    callWith.argument(
      "banned.serviceJourneys",
      (Collection<String> serviceJourneys) ->
        request.journey().transit().setBannedTrips(mapIDsToDomain(serviceJourneys))
    );

    // callWith.argument("banned.quays", quays -> request.setBannedStops(mappingUtil.prepareListOfFeedScopedId((List<String>) quays)));
    // callWith.argument("banned.quaysHard", quaysHard -> request.setBannedStopsHard(mappingUtil.prepareListOfFeedScopedId((List<String>) quaysHard)));

    // callWith.argument("heuristicStepsPerMainStep", (Integer v) -> request.heuristicStepsPerMainStep = v);
    // callWith.argument("compactLegsByReversedSearch", (Boolean v) -> { /* not used any more */ });
    // callWith.argument("banFirstServiceJourneysFromReuseNo", (Integer v) -> request.banFirstTripsFromReuseNo = v);
    callWith.argument(
      "debugItineraryFilter",
      (Boolean v) -> preferences.system().itineraryFilters().debug = v
    );

    callWith.argument("transferPenalty", (Integer v) -> preferences.transfer().setCost(v));

    // callWith.argument("useFlex", (Boolean v) -> request.useFlexService = v);
    // callWith.argument("ignoreMinimumBookingPeriod", (Boolean v) -> request.ignoreDrtAdvanceBookMin = v);

    RequestModes modes = getModes(environment, callWith);
    if (modes != null) {
      request.journey().setModes(modes);
    }
    ItineraryFiltersInputType.mapToRequest(
      environment,
      callWith,
      preferences.system().itineraryFilters()
    );

    /*
        List<Map<String, ?>> transportSubmodeFilters = environment.getArgument("transportSubmodes");
        if (transportSubmodeFilters != null) {
            request.transportSubmodes = new HashMap<>();
            for (Map<String, ?> transportSubmodeFilter : transportSubmodeFilters) {
                TraverseMode transportMode = (TraverseMode) transportSubmodeFilter.get("transportMode");
                List<TransmodelTransportSubmode> transportSubmodes = (List<TransmodelTransportSubmode>) transportSubmodeFilter.get("transportSubmodes");
                if (!CollectionUtils.isEmpty(transportSubmodes)) {
                    request.transportSubmodes.put(transportMode, new HashSet<>(transportSubmodes));
                }
            }
        }*/

    if (request.vehicleRental && !GqlUtil.hasArgument(environment, "bikeSpeed")) {
      //slower bike speed for bike sharing, based on empirical evidence from DC.
      preferences.bike().setSpeed(4.3);
    }

    // One of those arguments has been deprecated. That's why we are mapping same thing twice.
    callWith.argument("minimumTransferTime", preferences.transfer()::setSlack);
    callWith.argument("transferSlack", preferences.transfer()::setSlack);

    preferences
      .transit()
      .withBoardSlack(builder -> {
        callWith.argument("boardSlackDefault", builder::withDefaultSec);
        callWith.argument(
          "boardSlackList",
          (Integer v) -> TransportModeSlack.mapIntoDomain(builder, v)
        );
      });
    preferences
      .transit()
      .withAlightSlack(builder -> {
        callWith.argument("alightSlackDefault", builder::withDefaultSec);
        callWith.argument(
          "alightSlackList",
          (Object v) -> TransportModeSlack.mapIntoDomain(builder, v)
        );
      });
    callWith.argument("maximumTransfers", preferences.transfer()::setMaxTransfers);
    callWith.argument(
      "useBikeRentalAvailabilityInformation",
      preferences.rental()::setUseAvailabilityInformation
    );
    callWith.argument("ignoreRealtimeUpdates", preferences.transit()::setIgnoreRealtimeUpdates);
    callWith.argument(
      "includePlannedCancellations",
      preferences.transit()::setIncludePlannedCancellations
    );
    //callWith.argument("ignoreInterchanges", (Boolean v) -> request.ignoreInterchanges = v);

    return request;
  }

  @SuppressWarnings("unchecked")
  private RequestModes getModes(
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith
  ) {
    if (GqlUtil.hasArgument(environment, "modes")) {
      ElementWrapper<StreetMode> accessMode = new ElementWrapper<>();
      ElementWrapper<StreetMode> egressMode = new ElementWrapper<>();
      ElementWrapper<StreetMode> directMode = new ElementWrapper<>();
      ElementWrapper<List<LinkedHashMap<String, ?>>> transportModes = new ElementWrapper<>();
      callWith.argument("modes.accessMode", accessMode::set);
      callWith.argument("modes.egressMode", egressMode::set);
      callWith.argument("modes.directMode", directMode::set);
      callWith.argument("modes.transportModes", transportModes::set);

      RequestModesBuilder mBuilder = RequestModes
        .of()
        .withAccessMode(accessMode.get())
        .withEgressMode(egressMode.get())
        .withDirectMode(directMode.get());

      mBuilder.withTransferMode(
        accessMode.get() == StreetMode.BIKE ? StreetMode.BIKE : StreetMode.WALK
      );

      if (transportModes.get() == null) {
        mBuilder.withTransitModes(MainAndSubMode.all());
      } else {
        mBuilder.clearTransitModes();
        for (LinkedHashMap<String, ?> modeWithSubmodes : transportModes.get()) {
          if (modeWithSubmodes.containsKey("transportMode")) {
            TransitMode mainMode = (TransitMode) modeWithSubmodes.get("transportMode");
            if (modeWithSubmodes.containsKey("transportSubModes")) {
              var transportSubModes = (List<TransmodelTransportSubmode>) modeWithSubmodes.get(
                "transportSubModes"
              );
              for (TransmodelTransportSubmode submode : transportSubModes) {
                mBuilder.withTransitMode(mainMode, submode.getValue());
              }
            } else {
              mBuilder.withTransitMode(mainMode);
            }
          }
        }
      }
      return mBuilder.build();
    }
    return null;
  }

  /**
   * Simple wrapper in order to pass a consumer into the CallerWithEnvironment.argument method.
   */
  private static class ElementWrapper<T> {

    private T element;

    void set(T element) {
      this.element = element;
    }

    T get() {
      return this.element;
    }
  }
}
