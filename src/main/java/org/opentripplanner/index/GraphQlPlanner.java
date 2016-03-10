package org.opentripplanner.index;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;

import graphql.schema.DataFetchingEnvironment;

public class GraphQlPlanner {
    private GraphIndex index;

    public GraphQlPlanner(GraphIndex index) {
        this.index = index;
    }

    public TripPlan plan(DataFetchingEnvironment environment) {
        RoutingRequest request = createRequest(environment);
        Router router = index.graph.router;
        GraphPathFinder gpFinder = new GraphPathFinder(router);
        List<GraphPath> paths = gpFinder.graphPathFinderEntryPoint(request);
        TripPlan plan = GraphPathToTripPlanConverter.generatePlan(paths, request);        
        return plan;
    }

    private <T> void withArgument(DataFetchingEnvironment environment, String name, Consumer<T> consumer) {
        if (environment.containsArgument(name)) {
            consumer.accept(environment.getArgument(name));
        }
    }

    private RoutingRequest createRequest(DataFetchingEnvironment environment) {
        RoutingRequest request = new RoutingRequest();

        withArgument(environment, "fromPlace", request::setFromString);
        withArgument(environment, "toPlace", request::setToString);
        withArgument(environment, "intermediatePlaces", request::setIntermediatePlacesFromStrings);

        // something for date, time, tz into setDateTime

        withArgument(environment, "arriveBy", request::setArriveBy);
        withArgument(environment, "wheelchair", request::setWheelchairAccessible);
        withArgument(environment, "maxWalkDistance", request::setMaxWalkDistance);
        withArgument(environment, "maxPreTransitTime", request::setMaxPreTransitTime);
        withArgument(environment, "walkReluctance", request::setWalkReluctance);
        withArgument(environment, "waitAtBeginningFactor", request::setWaitAtBeginningFactor);
        withArgument(environment, "walkSpeed", (Double v) -> request.walkSpeed = v);
        withArgument(environment, "bikeSpeed", (Double v) -> request.bikeSpeed = v);
        withArgument(environment, "bikeSwitchTime", (Integer v) -> request.bikeSwitchTime = v);
        withArgument(environment, "bikeSwitchCost", (Integer v) -> request.bikeSwitchCost = v);

        //withArgument(environment, "optimize", request::setOptimize);
        withArgument(environment, "triangleSafetyFactor", request::setTriangleSafetyFactor);
        withArgument(environment, "triangleSlopeFactor", request::setTriangleSlopeFactor);
        withArgument(environment, "triangleTimeFactor", request::setTriangleTimeFactor);

        QualifiedModeSet modes = new QualifiedModeSet("WALK,BUS");
        modes.applyToRoutingRequest(request);
        request.setModes(request.modes);

        withArgument(environment, "minTransferTime", (Integer v) -> request.transferSlack= v);




        withArgument(environment, "fromLat", (Double v) -> request.from.lat = v);
        withArgument(environment, "fromLon", (Double v) -> request.from.lng = v);
        withArgument(environment, "toLat", (Double v) -> request.to.lat = v);
        withArgument(environment, "toLon", (Double v) -> request.to.lng = v);

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
        if (environment.containsArgument("numItineraries")) {
            request.setNumItineraries(((Number)environment.getArgument("numItineraries")).intValue());
        }
        return request;
    }
}
