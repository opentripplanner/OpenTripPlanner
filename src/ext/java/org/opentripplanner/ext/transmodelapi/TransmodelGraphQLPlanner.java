package org.opentripplanner.ext.transmodelapi;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang3.StringUtils;
import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.ext.transmodelapi.mapping.TransmodelMappingUtil;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.algorithm.raptor.router.RaptorRouter;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.request.BannedStopSet;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TransmodelGraphQLPlanner {

    private static final Logger LOG = LoggerFactory.getLogger(TransmodelGraphQLPlanner.class);

    private TransmodelMappingUtil mappingUtil;

    public TransmodelGraphQLPlanner(TransmodelMappingUtil mappingUtil) {
        this.mappingUtil = mappingUtil;
    }

    public Map<String, Object> plan(DataFetchingEnvironment environment) {
        Router router = environment.getContext();
        RoutingRequest request = createRequest(environment);

        TripPlan plan = new TripPlan(
                                            new Place(request.from.lng, request.from.lat, request.getFromPlace().name),
                                            new Place(request.to.lng, request.to.lat, request.getToPlace().name),
                                            request.getDateTime());
        List<Message> messages = new ArrayList<>();
        DebugOutput debugOutput = new DebugOutput();

        try {
            List<Itinerary> itineraries = new ArrayList<>();

            // TODO This currently only calculates the distances between the first fromVertex
            //      and the first toVertex
            if (request.modes.getNonTransitSet().isValid()) {
                double distance = SphericalDistanceLibrary.distance(
                        request.rctx.fromVertices.iterator().next().getCoordinate(),
                        request.rctx.toVertices.iterator().next().getCoordinate()
                );
                double limit = request.maxWalkDistance * 2;
                // Handle int overflow, in which case the multiplication will be less than zero
                if (limit < 0 || distance < limit) {
                    itineraries.addAll(findNonTransitItineraries(request, router));
                }
            }


            if (request.modes.isTransit()) {
                RaptorRouter raptorRouter = new RaptorRouter(request, router.graph.realtimeTransitLayer);
                itineraries.addAll(raptorRouter.route());
            }

            if (itineraries.isEmpty()) {
                throw new PathNotFoundException();
            }

            plan = createTripPlan(request, itineraries);
        } catch (Exception e) {
            PlannerError error = new PlannerError(e);
            if (!PlannerError.isPlanningError(e.getClass()))
                LOG.warn("Error while planning path: ", e);
            messages.add(error.message);
        } finally {
            if (request != null) {
                if (request.rctx != null) {
                    debugOutput = request.rctx.debugOutput;
                }
                request.cleanup(); // TODO verify that this cleanup step is being done on Analyst web services
            }
        }

        return ImmutableMap.<String, Object>builder()
                       .put("plan", plan)
                       .put("messages", messages)
                       .put("debugOutput", debugOutput)
                       .build();
    }

    private static <T> void call(Map<String, T> m, String name, Consumer<T> consumer) {
        if (!name.contains(".")) {
            if (hasArgument(m, name)) {
                T v = m.get(name);
                consumer.accept(v);
            }
        } else {
            String[] parts = name.split("\\.");
            if (hasArgument(m, parts[0])) {
                Map<String, T> nm = (Map<String, T>) m.get(parts[0]);
                call(nm, String.join(".", Arrays.copyOfRange(parts, 1, parts.length)), consumer);
            }
        }
    }

    private static <T> void call(DataFetchingEnvironment environment, String name, Consumer<T> consumer) {
        if (!name.contains(".")) {
            if (hasArgument(environment, name)) {
                consumer.accept(environment.getArgument(name));
            }
        } else {
            String[] parts = name.split("\\.");
            if (hasArgument(environment, parts[0])) {
                Map<String, T> nm = (Map<String, T>) environment.getArgument(parts[0]);
                call(nm, String.join(".", Arrays.copyOfRange(parts, 1, parts.length)), consumer);
            }
        }
    }

    private static class CallerWithEnvironment {
        private final DataFetchingEnvironment environment;

        public CallerWithEnvironment(DataFetchingEnvironment e) {
            this.environment = e;
        }

        private <T> void argument(String name, Consumer<T> consumer) {
            call(environment, name, consumer);
        }
    }

    private GenericLocation toGenericLocation(Map<String, Object> m) {
        Map<String, Object> coordinates = (Map<String, Object>) m.get("coordinates");
        Double lat = null;
        Double lon = null;
        if (coordinates != null) {
            lat = (Double) coordinates.get("latitude");
            lon = (Double) coordinates.get("longitude");
        }

        String placeRef = (String) m.get("place"); // TODO OTP2 mappingUtil.preparePlaceRef((String) m.get("place"));
        String name = (String) m.get("name");
        name = name == null ? "" : name;

        return new GenericLocation(name, mappingUtil.fromIdString(placeRef), lat, lon);
    }

    private RoutingRequest createRequest(DataFetchingEnvironment environment) {
        Router router = environment.getContext();
        RoutingRequest request = router.defaultRoutingRequest.clone();

        TransmodelGraphQLPlanner.CallerWithEnvironment callWith = new TransmodelGraphQLPlanner.CallerWithEnvironment(environment);

        callWith.argument("locale", (String v) -> request.locale = Locale.forLanguageTag(v));

        callWith.argument("from", (Map<String, Object> v) -> request.from = toGenericLocation(v));
        callWith.argument("to", (Map<String, Object> v) -> request.to = toGenericLocation(v));

        if (hasArgument(environment, "dateTime")) {
            callWith.argument("dateTime", millisSinceEpoch -> request.setDateTime(new Date((long) millisSinceEpoch)));
        } else {
            request.setDateTime(new Date());
        }
        callWith.argument("wheelchair", request::setWheelchairAccessible);
        callWith.argument("numTripPatterns", request::setNumItineraries);
        callWith.argument("maximumWalkDistance", request::setMaxWalkDistance);
//        callWith.argument("maxTransferWalkDistance", request::setMaxTransferWalkDistance);
        callWith.argument("maxPreTransitTime", request::setMaxPreTransitTime);
//        callWith.argument("preTransitReluctance", (Double v) ->  request.setPreTransitReluctance(v));
//        callWith.argument("maxPreTransitWalkDistance", (Double v) ->  request.setMaxPreTransitWalkDistance(v));
        callWith.argument("walkBoardCost", request::setWalkBoardCost);
        callWith.argument("walkReluctance", (Double v) ->  request.setWalkReluctance(v));
        callWith.argument("waitReluctance", (Double v) ->  request.setWaitReluctance(v));
        callWith.argument("walkBoardCost", request::setWalkBoardCost);
//        callWith.argument("walkOnStreetReluctance", request::setWalkOnStreetReluctance);
        callWith.argument("waitReluctance", request::setWaitReluctance);
        callWith.argument("waitAtBeginningFactor", request::setWaitAtBeginningFactor);
        callWith.argument("walkSpeed", (Double v) -> request.walkSpeed = v);
        callWith.argument("bikeSpeed", (Double v) -> request.bikeSpeed = v);
        callWith.argument("bikeSwitchTime", (Integer v) -> request.bikeSwitchTime = v);
        callWith.argument("bikeSwitchCost", (Integer v) -> request.bikeSwitchCost = v);
//        callWith.argument("transitDistanceReluctance", (Double v) -> request.transitDistanceReluctance = v);

        OptimizeType optimize = environment.getArgument("optimize");

        if (optimize == OptimizeType.TRIANGLE) {
            try {
                request.assertTriangleParameters(request.bikeTriangleSafetyFactor, request.bikeTriangleTimeFactor, request.bikeTriangleSlopeFactor);
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
//        callWith.argument("preferred.lines", lines -> request.setPreferredRoutes(mappingUtil.prepareListOfFeedScopedId((List<String>) lines, "__")));
        callWith.argument("preferred.otherThanPreferredLinesPenalty", request::setOtherThanPreferredRoutesPenalty);
        // Deprecated organisations -> authorities
        callWith.argument("preferred.organisations", organisations -> request.setPreferredAgencies(mappingUtil.mapCollectionOfValues((Collection<String>) organisations, in -> in)));
        callWith.argument("preferred.authorities", authorities -> request.setPreferredAgencies(mappingUtil.mapCollectionOfValues((Collection<String>) authorities, in -> in)));
//        callWith.argument("unpreferred.lines", lines -> request.setUnpreferredRoutes(mappingUtil.prepareListOfFeedScopedId((List<String>) lines, "__")));
        callWith.argument("unpreferred.organisations", organisations -> request.setUnpreferredAgencies(mappingUtil.mapCollectionOfValues((Collection<String>) organisations, in -> in)));
        callWith.argument("unpreferred.authorities", authorities -> request.setUnpreferredAgencies(mappingUtil.mapCollectionOfValues((Collection<String>) authorities, in -> in)));

//        callWith.argument("banned.lines", lines -> request.setBannedRoutes(mappingUtil.prepareListOfFeedScopedId((List<String>) lines, "__")));
        callWith.argument("banned.organisations", organisations -> request.setBannedAgencies(mappingUtil.mapCollectionOfValues((Collection<String>) organisations, in -> in)));
        callWith.argument("banned.authorities", authorities -> request.setBannedAgencies(mappingUtil.mapCollectionOfValues((Collection<String>) authorities, in -> in)));
        callWith.argument("banned.serviceJourneys", serviceJourneys -> request.bannedTrips = toBannedTrips((Collection<String>) serviceJourneys));

//        callWith.argument("banned.quays", quays -> request.setBannedStops(mappingUtil.prepareListOfFeedScopedId((List<String>) quays)));
//        callWith.argument("banned.quaysHard", quaysHard -> request.setBannedStopsHard(mappingUtil.prepareListOfFeedScopedId((List<String>) quaysHard)));

        callWith.argument("whiteListed.lines", lines -> request.setWhiteListedRoutes(mappingUtil.prepareListOfFeedScopedId((List<String>) lines, "__")));
        callWith.argument("whiteListed.organisations", organisations -> request.setWhiteListedAgencies(mappingUtil.mapCollectionOfValues((Collection<String>) organisations, in -> in)));
        callWith.argument("whiteListed.authorities", authorities -> request.setWhiteListedAgencies(mappingUtil.mapCollectionOfValues((Collection<String>) authorities, in -> in)));

        //callWith.argument("heuristicStepsPerMainStep", (Integer v) -> request.heuristicStepsPerMainStep = v);
        // callWith.argument("compactLegsByReversedSearch", (Boolean v) -> { /* not used any more */ });
        //callWith.argument("banFirstServiceJourneysFromReuseNo", (Integer v) -> request.banFirstTripsFromReuseNo = v);
        callWith.argument("allowBikeRental", (Boolean v) -> request.allowBikeRental = v);

        callWith.argument("transferPenalty", (Integer v) -> request.transferPenalty = v);

        //callWith.argument("useFlex", (Boolean v) -> request.useFlexService = v);
        //callWith.argument("ignoreMinimumBookingPeriod", (Boolean v) -> request.ignoreDrtAdvanceBookMin = v);

        if (optimize == OptimizeType.TRANSFERS) {
            optimize = OptimizeType.QUICK;
            request.transferPenalty += 1800;
        }

        if (optimize != null) {
            request.optimize = optimize;
        }

        if (hasArgument(environment, "modes")) {
            // Map modes to comma separated list in string first to be able to reuse logic in QualifiedModeSet
            // Remove CABLE_CAR from collection because QualifiedModeSet does not support mapping (splits on '_')
            Set<TraverseMode> modes = new HashSet<>(environment.getArgument("modes"));
            boolean cableCar = modes.remove(TraverseMode.CABLE_CAR);

            String modesAsString = modes.isEmpty() ? "" : Joiner.on(",").join(modes);
            if (!StringUtils.isEmpty(modesAsString)) {
                new QualifiedModeSet(modesAsString).applyToRoutingRequest(request);
                request.setModes(request.modes);
            } else if (cableCar) {
                // Clear default modes in case only cable car is selected
                request.clearModes();
            }

            // Apply cable car setting 
            request.modes.setCableCar(cableCar);
        }

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

        if (request.allowBikeRental && !hasArgument(environment, "bikeSpeed")) {
            //slower bike speed for bike sharing, based on empirical evidence from DC.
            request.bikeSpeed = 4.3;
        }

        callWith.argument("minimumTransferTime", (Integer v) -> request.transferSlack = v);
        assertSlack(request);

        callWith.argument("maximumTransfers", (Integer v) -> request.maxTransfers = v);

        final long NOW_THRESHOLD_MILLIS = 15 * 60 * 60 * 1000;
        boolean tripPlannedForNow = Math.abs(request.getDateTime().getTime() - new Date().getTime()) < NOW_THRESHOLD_MILLIS;
        request.useBikeRentalAvailabilityInformation = (tripPlannedForNow); // TODO the same thing for GTFS-RT


        callWith.argument("ignoreRealtimeUpdates", (Boolean v) -> request.ignoreRealtimeUpdates = v);
        //callWith.argument("includePlannedCancellations", (Boolean v) -> request.includePlannedCancellations = v);
        //callWith.argument("ignoreInterchanges", (Boolean v) -> request.ignoreInterchanges = v);

        /*
        if (!request.modes.isTransit() && request.modes.getCar()) {
            request.from.vertexId = getLocationOfFirstQuay(request.from.vertexId, ((Router)environment.getContext()).graph.index);
            request.to.vertexId = getLocationOfFirstQuay(request.to.vertexId, ((Router)environment.getContext()).graph.index);
        } else if (request.kissAndRide) {
            request.from.vertexId = getLocationOfFirstQuay(request.from.vertexId, ((Router)environment.getContext()).graph.index);
        } else if (request.rideAndKiss) {
            request.to.vertexId = getLocationOfFirstQuay(request.to.vertexId, ((Router)environment.getContext()).graph.index);
        } else if (request.parkAndRide) {
            request.from.vertexId = getLocationOfFirstQuay(request.from.vertexId, ((Router)environment.getContext()).graph.index);
        } else if (request.useFlexService) {
            request.from.vertexId = getLocationOfFirstQuay(request.from.vertexId, ((Router)environment.getContext()).graph.index);
            request.to.vertexId = getLocationOfFirstQuay(request.to.vertexId, ((Router)environment.getContext()).graph.index);
        }
         */

        request.setRoutingContext(router.graph);

        return request;
    }

    public void assertSlack(RoutingRequest r) {
        if (r.boardSlack + r.alightSlack > r.transferSlack) {
            // TODO thrown exception type is not consistent with assertTriangleParameters
            throw new RuntimeException("Invalid parameters: " +
                    "transfer slack must be greater than or equal to board slack plus alight slack");
        }
    }


    private HashMap<FeedScopedId, BannedStopSet> toBannedTrips(Collection<String> serviceJourneyIds) {
        Map<FeedScopedId, BannedStopSet> bannedTrips = serviceJourneyIds.stream().map(mappingUtil::fromIdString).collect(Collectors.toMap(Function.identity(), id -> BannedStopSet.ALL));
        return new HashMap<>(bannedTrips);
    }

    private String getLocationOfFirstQuay(String vertexId, GraphIndex graphIndex) {
        Vertex vertex = graphIndex.stopVertexForStop.get(vertexId);
        if (vertex instanceof TransitStopVertex) {
            TransitStopVertex stopVertex = (TransitStopVertex) vertex;

            FeedScopedId stopId = stopVertex.getStop().getId();

            return stopId.getFeedId().concat(":").concat(stopId.getId());
        } else {
            return vertexId;
        }
    }

    public static boolean hasArgument(DataFetchingEnvironment environment, String name) {
        return environment.containsArgument(name) && environment.getArgument(name) != null;
    }

    public static <T> boolean hasArgument(Map<String, T> m, String name) {
        return m.containsKey(name) && m.get(name) != null;
    }

    private List<Itinerary> findNonTransitItineraries(RoutingRequest request, Router router) {
        RoutingRequest nonTransitRequest = request.clone();
        nonTransitRequest.modes.setTransit(false);

        try {
            // we could also get a persistent router-scoped GraphPathFinder but there's no setup cost here
            GraphPathFinder gpFinder = new GraphPathFinder(router);
            List<GraphPath> paths = gpFinder.graphPathFinderEntryPoint(nonTransitRequest);

            /* Convert the internal GraphPaths to a TripPlan object that is included in an OTP web service Response. */
            TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, request);
            return plan.itinerary;
        } catch (PathNotFoundException e) {
            return Collections.emptyList();
        }
    }

    private TripPlan createTripPlan(RoutingRequest request, List<Itinerary> itineraries) {
        Place from = new Place();
        Place to = new Place();
        if (!itineraries.isEmpty()) {
            from = itineraries.get(0).legs.get(0).from;
            to = itineraries.get(0).legs.get(itineraries.get(0).legs.size() - 1).to;
        }
        TripPlan tripPlan = new TripPlan(from, to, request.getDateTime());
        itineraries = itineraries.stream().sorted(Comparator.comparing(i -> i.endTime))
                .limit(request.numItineraries).collect(Collectors.toList());
        tripPlan.itinerary = itineraries;
        LOG.info("Returning {} itineraries", itineraries.size());
        return tripPlan;
    }

}