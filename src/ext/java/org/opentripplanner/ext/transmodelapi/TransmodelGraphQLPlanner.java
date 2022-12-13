package org.opentripplanner.ext.transmodelapi;

import static org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper.mapIDsToDomain;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.SubMode;
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

    //        callWith.argument("transitDistanceReluctance", (Double v) -> request.transitDistanceReluctance = v);

    callWith.argument("arriveBy", request::setArriveBy);

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
      "preferred.lines",
      (List<String> lines) -> request.journey().transit().setPreferredRoutes(mapIDsToDomain(lines))
    );
    callWith.argument(
      "unpreferred.lines",
      (List<String> lines) ->
        request.journey().transit().setUnpreferredRoutes(mapIDsToDomain(lines))
    );

    // callWith.argument("banned.quays", quays -> request.setBannedStops(mappingUtil.prepareListOfFeedScopedId((List<String>) quays)));
    // callWith.argument("banned.quaysHard", quaysHard -> request.setBannedStopsHard(mappingUtil.prepareListOfFeedScopedId((List<String>) quaysHard)));

    callWith.argument(
      "whiteListed.rentalNetworks",
      (List<String> networks) -> request.journey().rental().setAllowedNetworks(Set.copyOf(networks))
    );

    callWith.argument(
      "banned.rentalNetworks",
      (List<String> networks) -> request.journey().rental().setBannedNetworks(Set.copyOf(networks))
    );

    // callWith.argument("heuristicStepsPerMainStep", (Integer v) -> request.heuristicStepsPerMainStep = v);
    // callWith.argument("compactLegsByReversedSearch", (Boolean v) -> { /* not used any more */ });
    // callWith.argument("banFirstServiceJourneysFromReuseNo", (Integer v) -> request.banFirstTripsFromReuseNo = v);

    // callWith.argument("useFlex", (Boolean v) -> request.useFlexService = v);
    // callWith.argument("ignoreMinimumBookingPeriod", (Boolean v) -> request.ignoreDrtAdvanceBookMin = v);

    // This only maps access, egress, direct & transfer.
    // Transport modes are now part of filters.
    // Only in case filters are not present we will use this mapping
    if (GqlUtil.hasArgument(environment, "modes")) {
      ElementWrapper<StreetMode> accessMode = new ElementWrapper<>();
      ElementWrapper<StreetMode> egressMode = new ElementWrapper<>();
      ElementWrapper<StreetMode> directMode = new ElementWrapper<>();
      callWith.argument("modes.accessMode", accessMode::set);
      callWith.argument("modes.egressMode", egressMode::set);
      callWith.argument("modes.directMode", directMode::set);

      var requestModes = RequestModes
        .of()
        .withAccessMode(accessMode.get())
        .withEgressMode(egressMode.get())
        .withDirectMode(directMode.get())
        .withTransferMode(accessMode.get() == StreetMode.BIKE ? StreetMode.BIKE : StreetMode.WALK)
        .build();

      request.journey().setModes(requestModes);
    }

    var bannedTrips = new ArrayList<FeedScopedId>();
    callWith.argument(
      "banned.serviceJourneys",
      (Collection<String> serviceJourneys) -> bannedTrips.addAll(mapIDsToDomain(serviceJourneys))
    );
    if (!bannedTrips.isEmpty()) {
      request.journey().transit().setBannedTrips(bannedTrips);
    }

    if (GqlUtil.hasArgument(environment, "filters")) {
      mapFilterNewWay(environment, callWith, request);
    } else {
      mapFilterOldWay(environment, callWith, request);
    }

    request.withPreferences(preferences -> mapPreferences(environment, callWith, preferences));

    return request;
  }

  @SuppressWarnings("unchecked")
  private void mapFilterOldWay(
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith,
    RouteRequest request
  ) {
    var filter = new TransitFilterRequest();
    var select = filter.select();
    var not = filter.not();

    if (
      !GqlUtil.hasArgument(environment, "modes.transportModes") &&
      GqlUtil.hasArgument(environment, "whiteListed") &&
      GqlUtil.hasArgument(environment, "banned")
    ) {
      request.journey().transit().setFilters(List.of(AllowAllTransitFilter.of()));
    }

    var whiteListedAgencies = new ArrayList<FeedScopedId>();
    callWith.argument(
      "whiteListed.authorities",
      (Collection<String> authorities) -> whiteListedAgencies.addAll(mapIDsToDomain(authorities))
    );
    if (!whiteListedAgencies.isEmpty()) {
      var selectRequest = new SelectRequest();
      selectRequest.setAgencies(whiteListedAgencies);
      select.add(selectRequest);
    }

    var bannedAgencies = new ArrayList<FeedScopedId>();
    callWith.argument(
      "banned.authorities",
      (Collection<String> authorities) -> bannedAgencies.addAll(mapIDsToDomain(authorities))
    );
    if (!bannedAgencies.isEmpty()) {
      var selectRequest = new SelectRequest();
      selectRequest.setAgencies(bannedAgencies);
      not.add(selectRequest);
    }

    var whiteListedLines = new ArrayList<FeedScopedId>();
    callWith.argument(
      "whiteListed.lines",
      (List<String> lines) -> whiteListedLines.addAll(mapIDsToDomain(lines))
    );
    if (!whiteListedLines.isEmpty()) {
      var selectRequest = new SelectRequest();
      selectRequest.setRoutes(RouteMatcher.idMatcher(whiteListedLines));
      select.add(selectRequest);
    }

    var bannedLines = new ArrayList<FeedScopedId>();
    callWith.argument(
      "banned.lines",
      (List<String> lines) -> bannedLines.addAll(mapIDsToDomain(lines))
    );
    if (!bannedLines.isEmpty()) {
      var selectRequest = new SelectRequest();
      selectRequest.setRoutes(RouteMatcher.idMatcher(bannedLines));
      not.add(selectRequest);
    }

    if (GqlUtil.hasArgument(environment, "modes")) {
      ElementWrapper<List<LinkedHashMap<String, ?>>> transportModes = new ElementWrapper<>();
      callWith.argument("modes.transportModes", transportModes::set);

      if (transportModes.get() != null) {
        var tModes = new ArrayList<MainAndSubMode>();

        for (LinkedHashMap<String, ?> modeWithSubmodes : transportModes.get()) {
          if (modeWithSubmodes.containsKey("transportMode")) {
            var mainMode = (TransitMode) modeWithSubmodes.get("transportMode");

            if (modeWithSubmodes.containsKey("transportSubModes")) {
              var transportSubModes = (List<TransmodelTransportSubmode>) modeWithSubmodes.get(
                "transportSubModes"
              );
              for (TransmodelTransportSubmode submode : transportSubModes) {
                tModes.add(new MainAndSubMode(mainMode, SubMode.of(submode.getValue())));
              }
            } else {
              tModes.add(new MainAndSubMode(mainMode));
            }
          }
        }

        if (select.isEmpty()) {
          var selectRequest = new SelectRequest();
          if (!tModes.isEmpty()) {
            selectRequest.setTransportModes(tModes);
          } else {
            selectRequest.setTransportModes(MainAndSubMode.all());
          }
          select.add(selectRequest);
        } else {
          if (!tModes.isEmpty()) {
            select.forEach(s -> s.setTransportModes(tModes));
          } else {
            select.forEach(s -> s.setTransportModes(MainAndSubMode.all()));
          }
        }
      }
    }

    request.journey().transit().setFilters(List.of(filter));
  }

  @SuppressWarnings("unchecked")
  private void mapFilterNewWay(
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith,
    RouteRequest request
  ) {
    if (GqlUtil.hasArgument(environment, "filters")) {
      ElementWrapper<List<LinkedHashMap<String, ?>>> filtersInput = new ElementWrapper<>();
      callWith.argument("filters", filtersInput::set);

      var filterRequests = new ArrayList<TransitFilter>();

      for (var filterInput : filtersInput.get()) {
        var filterRequest = new TransitFilterRequest();

        if (filterInput.containsKey("select")) {
          for (var selectInput : (List<LinkedHashMap<String, List<?>>>) filterInput.get("select")) {
            filterRequest.select().add(mapSelectRequest(selectInput));
          }
        }

        if (filterInput.containsKey("not")) {
          for (var selectInput : (List<LinkedHashMap<String, List<?>>>) filterInput.get("not")) {
            filterRequest.not().add(mapSelectRequest(selectInput));
          }
        }

        filterRequests.add(filterRequest);
      }

      request.journey().transit().setFilters(filterRequests);
    } else {
      request.journey().transit().setFilters(List.of(AllowAllTransitFilter.of()));
    }
  }

  @SuppressWarnings("unchecked")
  private SelectRequest mapSelectRequest(LinkedHashMap<String, List<?>> input) {
    var selectRequest = new SelectRequest();

    if (input.containsKey("lines")) {
      var lines = (List<String>) input.get("lines");
      selectRequest.setRoutes(RouteMatcher.idMatcher(mapIDsToDomain(lines)));
    }

    if (input.containsKey("authorities")) {
      var authorities = (List<String>) input.get("authorities");
      selectRequest.setAgencies(mapIDsToDomain(authorities));
    }

    if (input.containsKey("feeds")) {
      var feeds = (List<String>) input.get("feeds");
      selectRequest.setFeeds(feeds);
    }

    if (input.containsKey("transportModes")) {
      var tModes = new ArrayList<MainAndSubMode>();

      var transportModes = (List<LinkedHashMap<String, ?>>) input.get("transportModes");
      for (LinkedHashMap<String, ?> modeWithSubModes : transportModes) {
        var mainMode = (TransitMode) modeWithSubModes.get("transportMode");
        if (modeWithSubModes.containsKey("transportSubModes")) {
          var transportSubModes = (List<TransmodelTransportSubmode>) modeWithSubModes.get(
            "transportSubModes"
          );

          for (var subMode : transportSubModes) {
            tModes.add(new MainAndSubMode(mainMode, SubMode.of(subMode.getValue())));
          }
        } else {
          tModes.add(new MainAndSubMode(mainMode));
        }
      }
      selectRequest.setTransportModes(tModes);
    }

    return selectRequest;
  }

  private void mapPreferences(
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith,
    RoutingPreferences.Builder preferences
  ) {
    preferences.withWalk(b -> {
      callWith.argument("walkBoardCost", b::withBoardCost);
      callWith.argument("walkSpeed", b::withSpeed);
    });
    callWith.argument(
      "walkReluctance",
      (Double streetReluctance) -> {
        setStreetReluctance(preferences, streetReluctance);
      }
    );
    preferences.withBike(bike -> {
      callWith.argument("bikeSpeed", bike::withSpeed);
      callWith.argument("bikeSwitchTime", bike::withSwitchTime);
      callWith.argument("bikeSwitchCost", bike::withSwitchCost);
      callWith.argument("bicycleOptimisationMethod", bike::withOptimizeType);

      if (bike.optimizeType() == BicycleOptimizeType.TRIANGLE) {
        bike.withOptimizeTriangle(triangle -> {
          callWith.argument("triangle.timeFactor", triangle::withTime);
          callWith.argument("triangle.slopeFactor", triangle::withSlope);
          callWith.argument("triangle.safetyFactor", triangle::withSafety);
        });
      }
    });

    preferences.withTransfer(transfer -> {
      callWith.argument("transferPenalty", transfer::withCost);

      // 'minimumTransferTime' is deprecated, that's why we are mapping 'slack' twice.
      callWith.argument("minimumTransferTime", transfer::withSlack);
      callWith.argument("transferSlack", transfer::withSlack);

      callWith.argument("waitReluctance", transfer::withWaitReluctance);
      callWith.argument("maximumTransfers", transfer::withMaxTransfers);
    });
    preferences.withTransit(tr -> {
      callWith.argument(
        "preferred.otherThanPreferredLinesPenalty",
        tr::setOtherThanPreferredRoutesPenalty
      );
      tr.withBoardSlack(builder -> {
        callWith.argument("boardSlackDefault", builder::withDefaultSec);
        callWith.argument(
          "boardSlackList",
          (Integer v) -> TransportModeSlack.mapIntoDomain(builder, v)
        );
      });
      tr.withAlightSlack(builder -> {
        callWith.argument("alightSlackDefault", builder::withDefaultSec);
        callWith.argument(
          "alightSlackList",
          (Object v) -> TransportModeSlack.mapIntoDomain(builder, v)
        );
      });
      callWith.argument("ignoreRealtimeUpdates", tr::setIgnoreRealtimeUpdates);
      callWith.argument("includePlannedCancellations", tr::setIncludePlannedCancellations);
    });
    preferences.withItineraryFilter(itineraryFilter -> {
      callWith.argument("debugItineraryFilter", itineraryFilter::withDebug);
      ItineraryFiltersInputType.mapToRequest(environment, callWith, itineraryFilter);
    });
    preferences.withRental(rental ->
      callWith.argument(
        "useBikeRentalAvailabilityInformation",
        rental::withUseAvailabilityInformation
      )
    );
  }

  /**
   * This set the reluctance for bike, walk, car and bikeWalking (x2.7) - all in one go. These
   * parameters can be set individually.
   */
  private void setStreetReluctance(
    RoutingPreferences.Builder preferences,
    Double streetReluctance
  ) {
    if (streetReluctance > 0) {
      preferences.withWalk(walk -> walk.withReluctance(streetReluctance));
      preferences.withBike(bike ->
        bike.withReluctance(streetReluctance).withWalkingReluctance(streetReluctance * 2.7)
      );
      preferences.withCar(car -> car.withReluctance(streetReluctance));
    }
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
