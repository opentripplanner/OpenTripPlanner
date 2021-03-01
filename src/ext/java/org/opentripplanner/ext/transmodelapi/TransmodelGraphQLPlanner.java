package org.opentripplanner.ext.transmodelapi;

import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.api.mapping.PlannerErrorMapper;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper;
import org.opentripplanner.ext.transmodelapi.model.PlanResponse;
import org.opentripplanner.ext.transmodelapi.model.TransportModeSlack;
import org.opentripplanner.ext.transmodelapi.model.plan.ItineraryFiltersInputType;
import org.opentripplanner.ext.transmodelapi.support.DataFetcherDecorator;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.api.request.BannedStopSet;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper.mapIDsToDomain;

public class TransmodelGraphQLPlanner {

    private static final Logger LOG = LoggerFactory.getLogger(TransmodelGraphQLPlanner.class);

    public PlanResponse plan(DataFetchingEnvironment environment) {
        PlanResponse response = new PlanResponse();
        RoutingRequest request = null;
        try {
            TransmodelRequestContext ctx = environment.getContext();
            Router router = ctx.getRouter();

            request = createRequest(environment);

            RoutingResponse res = ctx.getRoutingService().route(request, router);

            response.plan = res.getTripPlan();
            response.metadata = res.getMetadata();

            for (RoutingError routingError : res.getRoutingErrors()) {
                response.messages.add(PlannerErrorMapper.mapMessage(routingError).message);
            }

            response.debugOutput = res.getDebugAggregator().finishedRendering();
        }
        catch (Exception e) {
            LOG.warn("System error");
            LOG.error("Root cause: " + e.getMessage(), e);
            PlannerError error = new PlannerError();
            error.setMsg(Message.SYSTEM_ERROR);
            response.messages.add(error.message);
        }
        return response;
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

    private RoutingRequest createRequest(DataFetchingEnvironment environment) {
        TransmodelRequestContext context = environment.getContext();
        Router router = context.getRouter();
        RoutingRequest request = router.defaultRoutingRequest.clone();

        DataFetcherDecorator callWith = new DataFetcherDecorator(environment);

        callWith.argument("locale", (String v) -> request.locale = Locale.forLanguageTag(v));

        callWith.argument("from", (Map<String, Object> v) -> request.from = toGenericLocation(v));
        callWith.argument("to", (Map<String, Object> v) -> request.to = toGenericLocation(v));

        callWith.argument("dateTime", millisSinceEpoch -> request.setDateTime(new Date((long) millisSinceEpoch)), Date::new);
        callWith.argument("searchWindow", (Integer m) -> request.searchWindow = Duration.ofMinutes(m));
        callWith.argument("timetableView", (Boolean v) -> request.timetableView = v);
        callWith.argument("wheelchair", request::setWheelchairAccessible);
        callWith.argument("numTripPatterns", request::setNumItineraries);
        callWith.argument("transitGeneralizedCostLimit", (DoubleFunction<Double> it) -> request.itineraryFilters.transitGeneralizedCostLimit = it);
        callWith.argument("maximumWalkDistance", request::setMaxWalkDistance);
//        callWith.argument("maxTransferWalkDistance", request::setMaxTransferWalkDistance);
        callWith.argument("maxPreTransitTime", request::setMaxPreTransitTime);
//        callWith.argument("preTransitReluctance", (Double v) ->  request.setPreTransitReluctance(v));
//        callWith.argument("maxPreTransitWalkDistance", (Double v) ->  request.setMaxPreTransitWalkDistance(v));
        callWith.argument("walkBoardCost", request::setWalkBoardCost);
        callWith.argument("walkReluctance", request::setWalkReluctance);
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

        BicycleOptimizeType optimize = environment.getArgument("optimize");

        if (optimize == BicycleOptimizeType.TRIANGLE) {
            try {
                RoutingRequest.assertTriangleParameters(
                    request.bikeTriangleSafetyFactor,
                    request.bikeTriangleTimeFactor,
                    request.bikeTriangleSlopeFactor
                );
                callWith.argument("triangle.safetyFactor", request::setBikeTriangleSafetyFactor);
                callWith.argument("triangle.slopeFactor", request::setBikeTriangleSlopeFactor);
                callWith.argument("triangle.timeFactor", request::setBikeTriangleTimeFactor);
            } catch (ParameterException e) {
                throw new RuntimeException(e);
            }
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
        callWith.argument("allowBikeRental", (Boolean v) -> request.bikeRental = v);
        callWith.argument("debugItineraryFilter", (Boolean v) -> request.itineraryFilters.debug = v);

        callWith.argument("transferPenalty", (Integer v) -> request.transferCost = v);

        //callWith.argument("useFlex", (Boolean v) -> request.useFlexService = v);
        //callWith.argument("ignoreMinimumBookingPeriod", (Boolean v) -> request.ignoreDrtAdvanceBookMin = v);

        if (optimize == BicycleOptimizeType.TRANSFERS) {
            optimize = BicycleOptimizeType.QUICK;
            request.transferCost += 1800;
        }

        if (optimize != null) {
            request.optimize = optimize;
        }

        if (GqlUtil.hasArgument(environment, "modes")) {
            ElementWrapper<StreetMode> accessMode = new ElementWrapper<>();
            ElementWrapper<StreetMode> egressMode = new ElementWrapper<>();
            ElementWrapper<StreetMode> directMode = new ElementWrapper<>();
            ElementWrapper<List<TransitMode>> transitModes = new ElementWrapper<>();
            callWith.argument("modes.accessMode", accessMode::set);
            callWith.argument("modes.egressMode", egressMode::set);
            callWith.argument("modes.directMode", directMode::set);
            callWith.argument("modes.transportMode", transitModes::set);

            if (transitModes.get() == null) {
                // Default to no transport modes if transport modes not specified
                transitModes.set(List.of());
            }

            request.modes = new RequestModes(
                accessMode.get(),
                egressMode.get(),
                directMode.get(),
                new HashSet<>(transitModes.get())
            );
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

        if (request.bikeRental && !GqlUtil.hasArgument(environment, "bikeSpeed")) {
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
        callWith.argument("useBikeRentalAvailabilityInformation", (Boolean v) -> request.useBikeRentalAvailabilityInformation = v);
        callWith.argument("ignoreRealtimeUpdates", (Boolean v) -> request.ignoreRealtimeUpdates = v);
        callWith.argument("includePlannedCancellations", (Boolean v) -> request.includePlannedCancellations = v);
        //callWith.argument("ignoreInterchanges", (Boolean v) -> request.ignoreInterchanges = v);

        return request;
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
