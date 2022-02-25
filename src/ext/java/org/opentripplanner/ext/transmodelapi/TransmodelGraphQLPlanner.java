package org.opentripplanner.ext.transmodelapi;

import static org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper.mapIDsToDomain;

import graphql.GraphQLException;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper;
import org.opentripplanner.ext.transmodelapi.model.PlanResponse;
import org.opentripplanner.ext.transmodelapi.model.TransmodelTransportSubmode;
import org.opentripplanner.ext.transmodelapi.model.TransportModeSlack;
import org.opentripplanner.ext.transmodelapi.model.plan.ItineraryFiltersInputType;
import org.opentripplanner.ext.transmodelapi.support.DataFetcherDecorator;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.modes.AllowedTransitMode;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.algorithm.mapping.TripPlanMapper;
import org.opentripplanner.routing.api.request.BannedStopSet;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransmodelGraphQLPlanner {

    private static final Logger LOG = LoggerFactory.getLogger(TransmodelGraphQLPlanner.class);

    public DataFetcherResult<PlanResponse> plan(DataFetchingEnvironment environment) {
        PlanResponse response = new PlanResponse();
        TransmodelRequestContext ctx = environment.getContext();
        Router router = ctx.getRouter();
        Locale locale = router.defaultRoutingRequest.locale;
        RoutingRequest request = null;
        try {
            request = createRequest(environment);
            locale = request.locale;

            RoutingResponse res = ctx.getRoutingService().route(request, router);

            response.plan = res.getTripPlan();
            response.metadata = res.getMetadata();
            response.messages = res.getRoutingErrors();
            response.debugOutput = res.getDebugTimingAggregator().finishedRendering();
            response.previousPageCursor = res.getPreviousPageCursor();
            response.nextPageCursor = res.getNextPageCursor();
        }
        catch (ParameterException e) {
            var msg = e.message.get();
            throw new GraphQLException(msg, e);
        }
        catch (Exception e) {
            LOG.error("System error: " + e.getMessage(), e);
            response.plan = TripPlanMapper.mapTripPlan(request, List.of());
            response.messages.add(new RoutingError(RoutingErrorCode.SYSTEM_ERROR, null));
        }
        return DataFetcherResult.<PlanResponse>newResult()
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

    private RoutingRequest createRequest(DataFetchingEnvironment environment)
    throws ParameterException {
        TransmodelRequestContext context = environment.getContext();
        Router router = context.getRouter();
        RoutingRequest request = router.defaultRoutingRequest.clone();

        DataFetcherDecorator callWith = new DataFetcherDecorator(environment);

        callWith.argument("locale", (String v) -> request.locale = Locale.forLanguageTag(v));

        callWith.argument("from", (Map<String, Object> v) -> request.from = toGenericLocation(v));
        callWith.argument("to", (Map<String, Object> v) -> request.to = toGenericLocation(v));

        callWith.argument("dateTime", millisSinceEpoch -> request.setDateTime(Instant.ofEpochMilli((long)millisSinceEpoch)), Date::new);
        callWith.argument("searchWindow", (Integer m) -> request.searchWindow = Duration.ofMinutes(m));
        callWith.argument("pageCursor", request::setPageCursor);
        callWith.argument("timetableView", (Boolean v) -> request.timetableView = v);
        callWith.argument("wheelchair", request::setWheelchairAccessible);
        callWith.argument("numTripPatterns", request::setNumItineraries);
        callWith.argument("transitGeneralizedCostLimit", (DoubleFunction<Double> it) -> request.itineraryFilters.transitGeneralizedCostLimit = it);
//        callWith.argument("maxTransferWalkDistance", request::setMaxTransferWalkDistance);
//        callWith.argument("preTransitReluctance", (Double v) ->  request.setPreTransitReluctance(v));
//        callWith.argument("maxPreTransitWalkDistance", (Double v) ->  request.setMaxPreTransitWalkDistance(v));
        callWith.argument("walkBoardCost", request::setWalkBoardCost);
        callWith.argument("walkReluctance", request::setNonTransitReluctance);
        callWith.argument("waitReluctance", request::setWaitReluctance);
        callWith.argument("walkBoardCost", request::setWalkBoardCost);
//        callWith.argument("walkOnStreetReluctance", request::setWalkOnStreetReluctance);
        callWith.argument("waitReluctance", request::setWaitReluctance);
        callWith.argument("waitAtBeginningFactor", request::setWaitAtBeginningFactor);
        callWith.argument("walkSpeed", (Double v) -> request.walkSpeed = v);
        callWith.argument("bikeSpeed", (Double v) -> request.bikeSpeed = v);
        callWith.argument("bikeSwitchTime", (Integer v) -> request.bikeSwitchTime = v);
        callWith.argument("bikeSwitchCost", (Integer v) -> request.bikeSwitchCost = v);
//        callWith.argument("transitDistanceReluctance", (Double v) -> request.transitDistanceReluctance = v);

        BicycleOptimizeType bicycleOptimizeType = environment.getArgument("bicycleOptimisationMethod");

        if (bicycleOptimizeType == BicycleOptimizeType.TRIANGLE) {

            // Arguments: [ safety, slope, time ]
            final double[] args = new double[3];

            callWith.argument("triangleFactors.safety", (Double v) -> args[0] = v);
            callWith.argument("triangleFactors.slope", (Double v) -> args[1] = v);
            callWith.argument("triangleFactors.time", (Double v) -> args[2] = v);

            request.setTriangleNormalized(args[0], args[1], args[2]);
        }

        if (bicycleOptimizeType == BicycleOptimizeType.TRANSFERS) {
            bicycleOptimizeType = BicycleOptimizeType.QUICK;
            request.transferCost += 1800;
        }

        if (bicycleOptimizeType != null) {
            request.bicycleOptimizeType = bicycleOptimizeType;
        }

        callWith.argument("arriveBy", request::setArriveBy);
        request.showIntermediateStops = true;
        callWith.argument("vias", (List<Map<String, Object>> v) -> request.intermediatePlaces = v.stream().map(this::toGenericLocation).collect(Collectors.toList()));

        callWith.argument("preferred.authorities", (Collection<String> authorities) -> request.setPreferredAgencies(mapIDsToDomain(authorities)));
        callWith.argument("unpreferred.authorities", (Collection<String> authorities) -> request.setUnpreferredAgencies(mapIDsToDomain(authorities)));
        callWith.argument("whiteListed.authorities", (Collection<String> authorities) -> request.setWhiteListedAgencies(mapIDsToDomain(authorities)));
        callWith.argument("banned.authorities", (Collection<String> authorities) -> request.setBannedAgencies(mapIDsToDomain(authorities)));

        callWith.argument("preferred.otherThanPreferredLinesPenalty", request::setOtherThanPreferredRoutesPenalty);
        callWith.argument("preferred.lines", (List<String> lines) -> request.setPreferredRoutes(mapIDsToDomain(lines)));
        callWith.argument("unpreferred.lines", (List<String> lines) -> request.setUnpreferredRoutes(mapIDsToDomain(lines)));
        callWith.argument("whiteListed.lines", (List<String> lines) -> request.setWhiteListedRoutes(mapIDsToDomain(lines)));
        callWith.argument("banned.lines", (List<String> lines) -> request.setBannedRoutes(mapIDsToDomain(lines)));

        callWith.argument("banned.serviceJourneys", (Collection<String> serviceJourneys) -> request.bannedTrips = toBannedTrips(serviceJourneys));

//        callWith.argument("banned.quays", quays -> request.setBannedStops(mappingUtil.prepareListOfFeedScopedId((List<String>) quays)));
//        callWith.argument("banned.quaysHard", quaysHard -> request.setBannedStopsHard(mappingUtil.prepareListOfFeedScopedId((List<String>) quaysHard)));


        //callWith.argument("heuristicStepsPerMainStep", (Integer v) -> request.heuristicStepsPerMainStep = v);
        // callWith.argument("compactLegsByReversedSearch", (Boolean v) -> { /* not used any more */ });
        //callWith.argument("banFirstServiceJourneysFromReuseNo", (Integer v) -> request.banFirstTripsFromReuseNo = v);
        callWith.argument("debugItineraryFilter", (Boolean v) -> request.itineraryFilters.debug = v);

        callWith.argument("transferPenalty", (Integer v) -> request.transferCost = v);

        //callWith.argument("useFlex", (Boolean v) -> request.useFlexService = v);
        //callWith.argument("ignoreMinimumBookingPeriod", (Boolean v) -> request.ignoreDrtAdvanceBookMin = v);

        RequestModes modes = getModes(environment, callWith);
        if (modes != null) {
            request.modes = modes;
        }

        ItineraryFiltersInputType.mapToRequest(environment, callWith, request.itineraryFilters);

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
            request.bikeSpeed = 4.3;
        }

        callWith.argument("minimumTransferTime", (Integer v) -> request.transferSlack = v);
        callWith.argument("transferSlack", (Integer v) -> request.transferSlack = v);
        callWith.argument("boardSlackDefault", (Integer v) -> request.boardSlack = v);
        callWith.argument("boardSlackList", (Object v) -> request.boardSlackForMode = TransportModeSlack.mapToDomain(v));
        callWith.argument("alightSlackDefault", (Integer v) -> request.alightSlack = v);
        callWith.argument("alightSlackList", (Object v) -> request.alightSlackForMode = TransportModeSlack.mapToDomain(v));
        callWith.argument("maximumTransfers", (Integer v) -> request.maxTransfers = v);
        callWith.argument("useBikeRentalAvailabilityInformation", (Boolean v) -> request.useVehicleRentalAvailabilityInformation = v);
        callWith.argument("ignoreRealtimeUpdates", (Boolean v) -> request.ignoreRealtimeUpdates = v);
        callWith.argument("includePlannedCancellations", (Boolean v) -> request.includePlannedCancellations = v);
        //callWith.argument("ignoreInterchanges", (Boolean v) -> request.ignoreInterchanges = v);

        return request;
    }

    @SuppressWarnings("unchecked")
    private RequestModes getModes(
        DataFetchingEnvironment environment, DataFetcherDecorator callWith
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

            List<AllowedTransitMode> transitModes = new ArrayList<>();
            if (transportModes.get() == null) {
                transitModes.addAll(Collections.emptyList());
            }
            else {
                for (LinkedHashMap<String, ?> modeWithSubmodes : transportModes.get()) {
                    if (modeWithSubmodes.containsKey("transportMode")) {
                        TransitMode mainMode = (TransitMode) modeWithSubmodes.get("transportMode");
                        if (modeWithSubmodes.containsKey("transportSubModes")) {
                            List<TransmodelTransportSubmode> transportSubModes =
                                    (List<TransmodelTransportSubmode>) modeWithSubmodes.get("transportSubModes");
                            for (TransmodelTransportSubmode transitMode : transportSubModes) {
                                transitModes.add(new AllowedTransitMode(
                                        mainMode,
                                        transitMode.getValue()
                                ));
                            }
                        }
                        else {
                            transitModes.add(AllowedTransitMode.fromMainModeEnum(mainMode));
                        }
                    }
                }
            }

            return new RequestModes(
                accessMode.get(),
                accessMode.get() == StreetMode.BIKE ? StreetMode.BIKE : StreetMode.WALK,
                egressMode.get(),
                directMode.get(),
                transitModes
            );
        }
        return null;
    }

    private HashMap<FeedScopedId, BannedStopSet> toBannedTrips(Collection<String> serviceJourneyIds) {
        Map<FeedScopedId, BannedStopSet> bannedTrips = serviceJourneyIds
            .stream()
            .map(TransitIdMapper::mapIDToDomain)
            .collect(Collectors.toMap(Function.identity(), id -> BannedStopSet.ALL));
        return new HashMap<>(bannedTrips);
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
