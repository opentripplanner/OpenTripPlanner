package org.opentripplanner.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import graphql.schema.DataFetchingEnvironment;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.ZoneIdSet;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.util.ResourceBundleSingleton;
import org.opentripplanner.util.SentryUtilities;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GraphQlPlanner {

    private static final Logger LOG = LoggerFactory.getLogger(GraphQlPlanner.class);

    private GraphIndex index;

    public GraphQlPlanner(GraphIndex index) {
        this.index = index;
    }

    public Map<String, Object> plan(DataFetchingEnvironment environment) {

        Router router = (Router) environment.getContext();


        RoutingRequest request = createRequest(environment);

        SentryUtilities.setupSentryBeforePlan(request);

        GraphPathFinder gpFinder = new GraphPathFinder(router);

        TripPlan plan = new TripPlan(
                new Place(request.from.lng, request.from.lat, request.getFromPlace().name),
                new Place(request.to.lng, request.to.lat, request.getToPlace().name),
                request.getDateTime());
        List<Message> messages = new ArrayList<>();
        DebugOutput debugOutput = new DebugOutput();

        try {
            List<GraphPath> paths = gpFinder.graphPathFinderEntryPoint(request);
            plan = GraphPathToTripPlanConverter.generatePlan(paths, request);
            // Add timeout message even if paths are found and no exception thrown
            if (request.rctx.debugOutput.timedOut) {
                messages.add(Message.REQUEST_TIMEOUT);
            }
        } catch (Exception e) {
            PlannerError error = new PlannerError(e);
            if (!PlannerError.isPlanningError(e.getClass())) {
                messages.add(error.message);
            }
        } catch (Throwable t) {
            LOG.warn("Unchecked error while planning path: ", t);
        } finally {
            if (request != null) {
                if (request.rctx != null) {
                    debugOutput = request.rctx.debugOutput;

                }
                request.cleanup(); // TODO verify that this cleanup step is being done on Analyst web services
            }
        }

        SentryUtilities.setupSentryAfterPlan(request, plan, messages);

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
        double lat = (double) m.get("lat");
        double lng = (double) m.get("lon");
        String address = (String) m.get("address");
        Integer locationSlack = (Integer) m.get("locationSlack");

        if (address != null) {
            return locationSlack != null
                ? new GenericLocation(address, Double.toString(lat) + "," + Double.toString(lng), locationSlack)
                : new GenericLocation(address, Double.toString(lat) + "," + Double.toString(lng));
        }

        return locationSlack != null
            ? new GenericLocation(lat, lng, locationSlack)
            : new GenericLocation(lat, lng);
    }

    private RoutingRequest createRequest(DataFetchingEnvironment environment) {
        Router router = (Router) environment.getContext();
        RoutingRequest request = router.defaultRoutingRequest.clone();
        request.routerId = router.id;

        CallerWithEnvironment callWith = new CallerWithEnvironment(environment);

        callWith.argument("fromPlace", request::setFromString);
        callWith.argument("toPlace", request::setToString);

        callWith.argument("from", (Map<String, Object> v) -> request.from = toGenericLocation(v));
        callWith.argument("to", (Map<String, Object> v) -> request.to = toGenericLocation(v));

        request.parseTime(router.graph.getTimeZone(), environment.getArgument("date"), environment.getArgument("time"));

        callWith.argument("wheelchair", request::setWheelchairAccessible);
        callWith.argument("numItineraries", request::setNumItineraries);
        callWith.argument("maxWalkDistance", request::setMaxWalkDistance);
        callWith.argument("maxPreTransitTime", request::setMaxPreTransitTime);
        callWith.argument("carParkCarLegWeight", request::setCarParkCarLegWeight);
        callWith.argument("itineraryFiltering", request::setItineraryFiltering);
        callWith.argument("walkReluctance", request::setWalkReluctance);
        callWith.argument("walkOnStreetReluctance", request::setWalkOnStreetReluctance);
        callWith.argument("waitReluctance", request::setWaitReluctance);
        callWith.argument("waitAtBeginningFactor", request::setWaitAtBeginningFactor);
        callWith.argument("walkSpeed", (Double v) -> request.walkSpeed = v);
        callWith.argument("bikeSpeed", (Double v) -> request.bikeSpeed = v);
        callWith.argument("bikeSwitchTime", (Integer v) -> request.bikeSwitchTime = v);
        callWith.argument("bikeSwitchCost", (Integer v) -> request.bikeSwitchCost = v);

        callWith.argument("modeWeight.TRAM", (Double v) -> request.setModeWeight(TraverseMode.TRAM, v));
        callWith.argument("modeWeight.SUBWAY", (Double v) -> request.setModeWeight(TraverseMode.SUBWAY, v));
        callWith.argument("modeWeight.RAIL", (Double v) -> request.setModeWeight(TraverseMode.RAIL, v));
        callWith.argument("modeWeight.BUS", (Double v) -> request.setModeWeight(TraverseMode.BUS, v));
        callWith.argument("modeWeight.FERRY", (Double v) -> request.setModeWeight(TraverseMode.FERRY, v));
        callWith.argument("modeWeight.CABLE_CAR", (Double v) -> request.setModeWeight(TraverseMode.CABLE_CAR, v));
        callWith.argument("modeWeight.GONDOLA", (Double v) -> request.setModeWeight(TraverseMode.GONDOLA, v));
        callWith.argument("modeWeight.FUNICULAR", (Double v) -> request.setModeWeight(TraverseMode.FUNICULAR, v));
        callWith.argument("modeWeight.AIRPLANE", (Double v) -> request.setModeWeight(TraverseMode.AIRPLANE, v));

        OptimizeType optimize = environment.getArgument("optimize");

        if (optimize == OptimizeType.TRIANGLE) {
            callWith.argument("triangle.safetyFactor", request::setTriangleSafetyFactor);
            callWith.argument("triangle.slopeFactor", request::setTriangleSlopeFactor);
            callWith.argument("triangle.timeFactor", request::setTriangleTimeFactor);
            try {
                RoutingRequest.assertTriangleParameters(request.triangleSafetyFactor, request.triangleTimeFactor, request.triangleSlopeFactor);
            } catch (ParameterException e) {
                throw new RuntimeException(e);
            }
        }

        callWith.argument("arriveBy", request::setArriveBy);
        request.showIntermediateStops = true;
        callWith.argument("intermediatePlaces", (List<Map<String, Object>> v) -> request.intermediatePlaces = v.stream().map(this::toGenericLocation).collect(Collectors.toList()));
        callWith.argument("preferred.routes", request::setPreferredRoutes);
        callWith.argument("preferred.otherThanPreferredRoutesPenalty", request::setOtherThanPreferredRoutesPenalty);
        callWith.argument("preferred.agencies", request::setPreferredAgencies);
        callWith.argument("unpreferred.routes", request::setUnpreferredRoutes);
        callWith.argument("unpreferred.agencies", request::setUnpreferredAgencies);
        callWith.argument("walkBoardCost", request::setWalkBoardCost);
        callWith.argument("bikeBoardCost", request::setBikeBoardCost);
        callWith.argument("banned.routes", request::setBannedRoutes);
        callWith.argument("banned.agencies", request::setBannedAgencies);
        callWith.argument("banned.trips", (String v) -> request.bannedTrips = RoutingResource.makeBannedTripMap(v));
        callWith.argument("banned.stops", request::setBannedStops);
        callWith.argument("banned.stopsHard", request::setBannedStopsHard);
        callWith.argument("transferPenalty", (Integer v) -> request.transferPenalty = v);
        callWith.argument("heuristicStepsPerMainStep", (Integer v) -> request.heuristicStepsPerMainStep = v);
        callWith.argument("compactLegsByReversedSearch", (Boolean v) -> request.compactLegsByReversedSearch = v);
        if (optimize == OptimizeType.TRANSFERS) {
            optimize = OptimizeType.QUICK;
            request.transferPenalty += 1800;
        }

	//Set argument 'batch' to false, as it causes timeouts and is not useful for point-to-point itinerary planning
        callWith.argument("batch", (Boolean v) -> /*request.batch = v*/ request.batch = false);

        if (optimize != null) {
            request.optimize = optimize;
        }


        if (hasArgument(environment, "transportModes")) {
            new QualifiedModeSet(((List<Map<String, Object>>)environment.getArgument("transportModes"))
                                                                .stream()
                                                                .map(transportMode -> new QualifiedMode((TraverseMode)transportMode.get("mode"), (QualifiedMode.Qualifier)transportMode.get("qualifier")))
                                                                .collect(Collectors.toList())).applyToRoutingRequest(request);
            request.setModes(request.modes);
        }

        if (hasArgument(environment, "modes") && !hasArgument(environment, "transportModes")) {
            new QualifiedModeSet((String)environment.getArgument("modes")).applyToRoutingRequest(request);
            request.setModes(request.modes);
        }

        if (hasArgument(environment, "ticketTypes")) {
            String ticketTypes = environment.getArgument("ticketTypes");
            request.setZoneIdSet(ZoneIdSet.create(index, ticketTypes));
            //TODO should we increase max walk distance?
            //request.setMaxWalkDistance(request.getMaxWalkDistance()*2);
        } else {
            request.setZoneIdSet(new ZoneIdSet());
        }

        if (request.allowBikeRental && !hasArgument(environment, "bikeSpeed")) {
            //slower bike speed for bike sharing, based on empirical evidence from DC.
            request.bikeSpeed = 4.3;
        }

        callWith.argument("boardSlack", (Integer v) -> request.boardSlack = v);
        callWith.argument("alightSlack", (Integer v) -> request.alightSlack = v);
        callWith.argument("minTransferTime", (Integer v) -> request.transferSlack = v); // TODO RoutingRequest field should be renamed
        callWith.argument("nonpreferredTransferPenalty", (Integer v) -> request.nonpreferredTransferPenalty = v);

        request.assertSlack();

        callWith.argument("maxTransfers", (Integer v) -> request.maxTransfers = v);

        final long NOW_THRESHOLD_MILLIS = 15 * 60 * 60 * 1000;
        boolean tripPlannedForNow = Math.abs(request.getDateTime().getTime() - new Date().getTime()) < NOW_THRESHOLD_MILLIS;
        request.useBikeRentalAvailabilityInformation = (tripPlannedForNow); // TODO the same thing for GTFS-RT

        callWith.argument("startTransitStopId", (String v) -> request.startingTransitStopId = GtfsLibrary.convertIdFromString(v));
        callWith.argument("startTransitTripId", (String v) -> request.startingTransitTripId = GtfsLibrary.convertIdFromString(v));
        callWith.argument("clamInitialWait", (Long v) -> request.clampInitialWait = v);
        callWith.argument("reverseOptimizeOnTheFly", (Boolean v) -> request.reverseOptimizeOnTheFly = v);
        callWith.argument("ignoreRealtimeUpdates", (Boolean v) -> request.ignoreRealtimeUpdates = v);
        callWith.argument("disableRemainingWeightHeuristic", (Boolean v) -> request.disableRemainingWeightHeuristic = v);

        callWith.argument("locale", (String v) -> request.locale = ResourceBundleSingleton.INSTANCE.getLocale(v));

//        request.setMaxWalkDistance(2500.0);
//        request.setWalkReluctance(2.0);
//        request.walkSpeed = 1.2;
//        request.setArriveBy(false);
//        request.showIntermediateStops = true;
//        request.setIntermediatePlacesFromStrings(Collections.emptyList());
//        request.setWalkBoardCost(600);
//        request.transferSlack = 180;
//        request.disableRemainingWeightHeuristic = false;
        return request;
    }

    public static boolean hasArgument(DataFetchingEnvironment environment, String name) {
        return environment.containsArgument(name) && environment.getArgument(name) != null;
    }

    public static <T> boolean hasArgument(Map<String, T> m, String name) {
        return m.containsKey(name) && m.get(name) != null;
    }
}
