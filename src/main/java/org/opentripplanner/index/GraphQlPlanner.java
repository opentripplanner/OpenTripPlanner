package org.opentripplanner.index;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.util.ResourceBundleSingleton;

import graphql.schema.DataFetchingEnvironment;

public class GraphQlPlanner {
    private GraphIndex index;

    public GraphQlPlanner(GraphIndex index) {
        this.index = index;
    }

    public TripPlan plan(DataFetchingEnvironment environment) {
        Router router = (Router)environment.getContext();
        RoutingRequest request = createRequest(environment);
        GraphPathFinder gpFinder = new GraphPathFinder(router);
        List<GraphPath> paths = gpFinder.graphPathFinderEntryPoint(request);
        TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, request);        
        return plan;
    }

    private static <T> void call(DataFetchingEnvironment environment, String name, Consumer<T> consumer) {
        if (hasArgument(environment, name)) {
            consumer.accept(environment.getArgument(name));
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
        return new GenericLocation(lat, lng);
    }

    private RoutingRequest createRequest(DataFetchingEnvironment environment) {
        Router router = (Router)environment.getContext();
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
        callWith.argument("walkReluctance", request::setWalkReluctance);
        callWith.argument("waitReluctance", request::setWaitReluctance);
        callWith.argument("waitAtBeginningFactor", request::setWaitAtBeginningFactor);
        callWith.argument("walkSpeed", (Double v) -> request.walkSpeed = v);
        callWith.argument("bikeSpeed", (Double v) -> request.bikeSpeed = v);
        callWith.argument("bikeSwitchTime", (Integer v) -> request.bikeSwitchTime = v);
        callWith.argument("bikeSwitchCost", (Integer v) -> request.bikeSwitchCost = v);

        OptimizeType optimize = environment.getArgument("optimize");

        if (optimize == OptimizeType.TRIANGLE) {
            Double triangleSafetyFactor = environment.getArgument("triangleSafetyFactor");
            Double triangleSlopeFactor = environment.getArgument("triangleSlopeFactor");
            Double triangleTimeFactor = environment.getArgument("triangleTimeFactor");
            try {
                RoutingRequest.assertTriangleParameters(triangleSafetyFactor, triangleTimeFactor, triangleSlopeFactor);
            } catch (ParameterException e) {
                throw new RuntimeException(e);
            }
            callWith.argument("triangleSafetyFactor", request::setTriangleSafetyFactor);
            callWith.argument("triangleSlopeFactor", request::setTriangleSlopeFactor);
            callWith.argument("triangleTimeFactor", request::setTriangleTimeFactor);
        }

        callWith.argument("arriveBy", request::setArriveBy);
        callWith.argument("showIntermediateStops", (Boolean v) -> request.showIntermediateStops = v);
        callWith.argument("intermediatePlaces", request::setIntermediatePlacesFromStrings);
        callWith.argument("preferredRoutes", request::setPreferredRoutes);
        callWith.argument("otherThanPreferredRoutesPenalty", request::setOtherThanPreferredRoutesPenalty);
        callWith.argument("preferredAgencies", request::setPreferredAgencies);
        callWith.argument("unpreferredRoutes", request::setUnpreferredRoutes);
        callWith.argument("unpreferredAgencies", request::setUnpreferredAgencies);
        callWith.argument("walkBoardCost", request::setWalkBoardCost);
        callWith.argument("bikeBoardCost", request::setBikeBoardCost);
        callWith.argument("bannedRoutes", request::setBannedRoutes);
        callWith.argument("bannedAgencies", request::setBannedAgencies);
        callWith.argument("bannedTrips", (String v) -> request.bannedTrips = RoutingResource.makeBannedTripMap(v));
        callWith.argument("bannedStops", request::setBannedStops);
        callWith.argument("bannedStopsHard", request::setBannedStopsHard);
        callWith.argument("transferPenalty", (Integer v) -> request.transferPenalty = v);
        if (optimize == OptimizeType.TRANSFERS) {
            optimize = OptimizeType.QUICK;
            request.transferPenalty += 1800;
        }

        callWith.argument("batch", (Boolean v) -> request.batch = v);

        if (optimize != null) {
            request.optimize = optimize;
        }

        if (hasArgument(environment, "modes")) {
            new QualifiedModeSet(environment.getArgument("modes")).applyToRoutingRequest(request);
            request.setModes(request.modes);
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

        callWith.argument("startTransitStopId", (String v) -> request.startingTransitStopId = AgencyAndId.convertFromString(v));
        callWith.argument("startTransitTripId", (String v) -> request.startingTransitTripId = AgencyAndId.convertFromString(v));
        callWith.argument("clamInitialWait", (Long v) -> request.clampInitialWait = v);
        callWith.argument("reverseOptimizeOnTheFly", (Boolean v) -> request.reverseOptimizeOnTheFly = v);
        callWith.argument("ignoreRealtimeUpdates", (Boolean v) -> request.ignoreRealtimeUpdates = v);
        callWith.argument("disableRemainingWeightHeuristic", (Boolean v) -> request.disableRemainingWeightHeuristic = v);

        callWith.argument("locale", (String v) -> request.locale = ResourceBundleSingleton.INSTANCE.getLocale(v));

        request.setPreferredAgencies("HSL");
        request.setWheelchairAccessible(false);
        request.setMaxWalkDistance(2500.0);
        request.setWalkReluctance(2.0);
        request.walkSpeed = 1.2;
        request.setArriveBy(false);
        request.showIntermediateStops = true;
        request.setIntermediatePlacesFromStrings(Collections.emptyList());
        request.setWalkBoardCost(600);
        request.transferSlack = 180;
        request.disableRemainingWeightHeuristic = false;
        return request;
    }

    public static boolean hasArgument(DataFetchingEnvironment environment, String name) {
        return environment.containsArgument(name) && environment.getArgument(name) != null;
    }
}
