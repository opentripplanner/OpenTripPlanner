package org.opentripplanner.standalone.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;

/**
 * This class is an object representation of the 'router-config.json'.
 */
public class RouterConfig implements Serializable {

    private static final double DEFAULT_STREET_ROUTING_TIMEOUT = 5.0;
    private static final Logger LOG = LoggerFactory.getLogger(RouterConfig.class);

    public static final RouterConfig DEFAULT = new RouterConfig(
            MissingNode.getInstance(), "DEFAULT"
    );

    /**
     * The raw JsonNode three kept for reference and (de)serialization.
     */
    public final JsonNode rawJson;
    public final String requestLogFile;

    /**
     * The preferred way to limit the search is to limit the distance for
     * each street mode(WALK, BIKE, CAR). So the default timeout for a
     * street search is set quite high. This is used to abort the search
     * if the max distance is not reached within the timeout.
     */
    public final double streetRoutingTimeoutSeconds;

    /**
     * TODO OTP2 - Regression; this need to be integrated with Raptor. This parameter was
     *           - previously on the graph, but should be injected into Raptor instead.
     *
     * Has information how much time boarding a vehicle takes. Can be significant
     * eg in airplanes or ferries. Unit is ?. Default value is ? <-- TODO OTP2
     */
    public final Map<TraverseMode, Integer> boardSlackByMode;

    /**
     * TODO OTP2 - Regression; this need to be integrated with Raptor. This parameter was
     *           - previously on the graph, but should be injected into Raptor instead.
     *
     * Has information how much time alighting a vehicle takes. Can be significant
     * eg in airplanes or ferries.Unit is ?. Default value is ? <-- TODO OTP2
     */
    public final Map<TraverseMode, Integer> alightSlackByMode;

    public final RoutingRequest routingRequestDefaults;
    public final RaptorTuningParameters raptorTuningParameters;

    public RouterConfig(JsonNode node, String source) {
        NodeAdapter adapter = new NodeAdapter(node, source);
        this.rawJson = node;
        this.requestLogFile = adapter.asText("requestLogFile", null);
        this.streetRoutingTimeoutSeconds = adapter.asDouble(
                "streetRoutingTimeout", DEFAULT_STREET_ROUTING_TIMEOUT
        );
        this.boardSlackByMode = adapter.asEnumMap("boardTimes", TraverseMode.class, NodeAdapter::asInt);
        this.alightSlackByMode = adapter.asEnumMap("alightTimes", TraverseMode.class, NodeAdapter::asInt);

        this.raptorTuningParameters = new TransitRoutingConfig(adapter.path("transit"));
        this.routingRequestDefaults = routingRequestDefaults(adapter.path("routingDefaults"));

        adapter.logUnusedParameters(LOG);
    }

    public RoutingRequest routingRequestDefaults(NodeAdapter c) {
        RoutingRequest dft = new RoutingRequest();

        if (c.isEmpty()) {
            LOG.info("No default routing parameters were found in the router config JSON. Using built-in OTP defaults.");
            return dft;
        }

        LOG.debug("Loading default routing parameters from JSON.");
        RoutingRequest request = new RoutingRequest();

        // Keep this alphabetically sorted so it is easy to check if a parameter is missing from the
        // mapping or duplicate exist.
        request.alightSlack = c.asInt("alightSlack", dft.alightSlack);
        request.allowBikeRental = c.asBoolean("allowBikeRental", dft.allowBikeRental);
        request.arriveBy = c.asBoolean("arriveBy", dft.arriveBy);
        request.bikeBoardCost = c.asInt("bikeBoardCost", dft.bikeBoardCost);
        request.bikeParkAndRide = c.asBoolean("bikeParkAndRide", dft.bikeParkAndRide);
        request.bikeParkTime = c.asInt("bikeParkTime", dft.bikeParkTime);
        request.bikeParkCost = c.asInt("bikeParkCost", dft.bikeParkCost);
        request.bikeRentalDropoffCost = c.asInt("bikeRentalDropoffCost", dft.bikeRentalDropoffCost);
        request.bikeRentalDropoffTime = c.asInt("bikeRentalDropoffTime", dft.bikeRentalDropoffTime);
        request.bikeRentalPickupCost = c.asInt("bikeRentalPickupCost", dft.bikeRentalPickupCost);
        request.bikeRentalPickupTime = c.asInt("bikeRentalPickupTime", dft.bikeRentalPickupTime);
        request.bikeSpeed = c.asDouble("bikeSpeed", dft.bikeSpeed);
        request.bikeTriangleSafetyFactor = c.asDouble("bikeTriangleSafetyFactor", dft.bikeTriangleSafetyFactor);
        request.bikeTriangleSlopeFactor = c.asDouble("bikeTriangleSlopeFactor", dft.bikeTriangleSlopeFactor);
        request.bikeTriangleTimeFactor = c.asDouble("bikeTriangleTimeFactor", dft.bikeTriangleTimeFactor);
        request.bikeSwitchTime = c.asInt("bikeSwitchTime", dft.bikeSwitchTime);
        request.bikeSwitchCost = c.asInt("bikeSwitchCost", dft.bikeSwitchCost);
        request.debugItineraryFilter = c.asBoolean("debugItineraryFilter", dft.debugItineraryFilter);
        request.carAccelerationSpeed = c.asDouble("carAccelerationSpeed", dft.carAccelerationSpeed);
        request.carDecelerationSpeed = c.asDouble("carDecelerationSpeed", dft.carDecelerationSpeed);
        request.carDropoffTime = c.asInt("carDropoffTime", dft.carDropoffTime);
        request.carSpeed = c.asDouble("carSpeed", dft.carSpeed);
        request.debugItineraryFilter = c.asBoolean("debugItineraryFilter", dft.debugItineraryFilter);
        request.disableAlertFiltering = c.asBoolean("disableAlertFiltering", dft.disableAlertFiltering);
        request.disableRemainingWeightHeuristic = c.asBoolean("disableRemainingWeightHeuristic", dft.disableRemainingWeightHeuristic);
        request.driveOnRight = c.asBoolean("driveOnRight", dft.driveOnRight);
        request.elevatorBoardCost = c.asInt("elevatorBoardCost", dft.elevatorBoardCost);
        request.elevatorBoardTime = c.asInt("elevatorBoardTime", dft.elevatorBoardTime);
        request.elevatorHopCost = c.asInt("elevatorHopCost", dft.elevatorHopCost);
        request.elevatorHopTime = c.asInt("elevatorHopTime", dft.elevatorHopTime);
        request.geoidElevation = c.asBoolean("geoidElevation", dft.geoidElevation);
        request.ignoreRealtimeUpdates = c.asBoolean("ignoreRealtimeUpdates", dft.ignoreRealtimeUpdates);
        request.kissAndRide = c.asBoolean("kissAndRide", dft.kissAndRide);
        request.locale = c.asLocale("locale", dft.locale);
        request.maxHours = c.asDouble("maxHours", dft.maxHours);
        request.maxPreTransitTime = c.asInt("maxPreTransitTime", dft.maxPreTransitTime);
        request.maxTransferWalkDistance = c.asDouble("maxTransferWalkDistance", dft.maxTransferWalkDistance);
        // 'maxTransfers' is configured in the Raptor tuning parameters, not here
        request.maxWalkDistance = c.asDouble("maxWalkDistance", dft.maxWalkDistance);
        request.maxWeight = c.asDouble("maxWeight", dft.maxWeight);
        request.maxWheelchairSlope = c.asDouble("maxWheelchairSlope", dft.maxWheelchairSlope); // ADA max wheelchair ramp slope is a good default.
        request.modes = c.exist("modes") ? new TraverseModeSet(c.asEnumSet("modes", TraverseMode.class)) : dft.modes;
        request.nonpreferredTransferCost = c.asInt("nonpreferredTransferPenalty", dft.nonpreferredTransferCost);
        request.numItineraries = c.asInt("numItineraries", dft.numItineraries);
        request.onlyTransitTrips = c.asBoolean("onlyTransitTrips", dft.onlyTransitTrips);
        request.optimize = c.asEnum("optimize", dft.optimize);
        request.otherThanPreferredRoutesPenalty = c.asInt("otherThanPreferredRoutesPenalty", dft.otherThanPreferredRoutesPenalty);
        request.parkAndRide = c.asBoolean("parkAndRide", dft.parkAndRide);
        request.pathComparator = c.asText("pathComparator", dft.pathComparator);
        request.showIntermediateStops = c.asBoolean("showIntermediateStops", dft.showIntermediateStops);
        request.stairsReluctance = c.asDouble("stairsReluctance", dft.stairsReluctance);
        request.startingTransitTripId = c.asFeedScopedId("startingTransitTripId", dft.startingTransitTripId);
        request.transferCost = c.asInt("transferPenalty", dft.transferCost);
        request.transferSlack = c.asInt("transferSlack", dft.transferSlack);
        request.turnReluctance = c.asDouble("turnReluctance", dft.turnReluctance);
        request.useBikeRentalAvailabilityInformation = c.asBoolean("useBikeRentalAvailabilityInformation", dft.useBikeRentalAvailabilityInformation);
        request.useRequestedDateTimeInMaxHours = c.asBoolean("useRequestedDateTimeInMaxHours", dft.useRequestedDateTimeInMaxHours);
        request.useUnpreferredRoutesPenalty = c.asInt("useUnpreferredRoutesPenalty", dft.useUnpreferredRoutesPenalty);
        request.waitAtBeginningFactor = c.asDouble("waitAtBeginningFactor", dft.waitAtBeginningFactor);
        request.waitReluctance = c.asDouble("waitReluctance", dft.waitReluctance);
        request.walkBoardCost = c.asInt("walkBoardCost", dft.walkBoardCost);
        request.walkReluctance = c.asDouble("walkReluctance", dft.walkReluctance);
        request.walkSpeed = c.asDouble("walkSpeed", dft.walkSpeed);
        request.walkingBike = c.asBoolean("walkingBike", dft.walkingBike);
        request.wheelchairAccessible = c.asBoolean("wheelchairAccessible", dft.wheelchairAccessible);
        request.worstTime = c.asLong("worstTime", dft.worstTime);

        return request;
    }

    /**
     * If {@code true} the config is loaded from file, in not the DEFAULT config is used.
     */
    public boolean isDefault() {
        return this.rawJson.isMissingNode();
    }

    public String toJson() {
        return rawJson.isMissingNode() ? "" : rawJson.toString();
    }

    public String toString() {
        // Print ONLY the values set, not deafult values
        return rawJson.toPrettyString();
    }

}
