package org.opentripplanner.updater.vehicle_positions;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import org.opentripplanner.index.model.RealtimeVehiclePosition;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Responsible for converting vehicle positions in memory to exportable ones, and associating each position
 * with a pattern.
 */
public class VehiclePositionPatternMatcher {
    private static final Logger LOG =
            LoggerFactory.getLogger(VehiclePositionPatternMatcher.class);

    private final GraphIndex graphIndex;

    /**
     * Set of trip IDs we've seen, so if we stop seeing them we know to remove them from the pattern
     */
    private final Set<String> seenTripIds = new HashSet<>();

    public VehiclePositionPatternMatcher(Graph graph) {
        graphIndex = graph.index;
    }

    /**
     * Clear seen trip IDs
     */
    public void wipeSeenTripIds() {
        seenTripIds.clear();
    }

    /**
     * Iterates over a pattern's vehicle positions and removes all not seen in the seenTripIds set
     * @param feedId           FeedId whose pattern's should be "cleaned"
     */
    public void cleanPatternVehiclePositions(String feedId) {
        for (TripPattern pattern : graphIndex.patternsForFeedId.get(feedId)) {
            for (String key : pattern.vehiclePositions.keySet()) {
                if (!seenTripIds.contains(key)) {
                    pattern.vehiclePositions.remove(key);
                }
            }
        }
    }

    /**
     * Attempts to match each vehicle position to a pattern, then adds each to a pattern
     * @param vehiclePositions  List of vehicle positions to match to patterns
     * @param feedId            Feed id of vehicle positions to assist in pattern-matching
     */
    public void applyVehiclePositionUpdates(List<VehiclePosition> vehiclePositions, String feedId) {
        for (VehiclePosition vehiclePosition : vehiclePositions) {
            if (!vehiclePosition.hasTrip()) {
                LOG.warn("Realtime vehicle positions without trip IDs are not yet supported.");
                continue;
            }

            String tripId = vehiclePosition.getTrip().getTripId();
            Trip trip = graphIndex.tripForId.get(new FeedScopedId(feedId, tripId));
            if (trip == null) {
                LOG.warn(String.format("Unable to find OTP trip ID for vehicle position with trip ID %s", tripId));
                continue;
            }

            TripPattern pattern = graphIndex.patternForTrip.get(trip);
            if (pattern == null) {
                LOG.warn(String.format("Unable to match OTP pattern ID for vehicle position with trip ID %s", tripId));
                continue;
            }

            // Add trip to seen trip set
            seenTripIds.add(tripId);

            // Add position to pattern
            RealtimeVehiclePosition newPosition = parseVehiclePosition(
                    vehiclePosition,
                    Arrays.asList(pattern.stopVertices)
            );
            newPosition.patternId = pattern.code;

            if (pattern.vehiclePositions == null) {
                pattern.vehiclePositions = new HashMap<>();
            }
            pattern.vehiclePositions.put(tripId, newPosition);
        }
    }

    /**
     * Converts GtfsRealtime vehicle position to the OTP RealtimeVehiclePosition which can be
     * used by the API.
     * @param vehiclePosition       GtfsRealtime vehicle position
     * @param stopsOnVehicleTrip    Collection of stops method will try to match next arriving stop ID to
     * @return                      OTP RealtimeVehiclePosition
     */
    private RealtimeVehiclePosition parseVehiclePosition(VehiclePosition vehiclePosition, List<TransitStop> stopsOnVehicleTrip) {
        RealtimeVehiclePosition newPosition = new RealtimeVehiclePosition();
        if (vehiclePosition.hasPosition()) {
            newPosition.lat = vehiclePosition.getPosition().getLatitude();
            newPosition.lon = vehiclePosition.getPosition().getLongitude();
            newPosition.speed = vehiclePosition.getPosition().getSpeed();
            newPosition.heading = vehiclePosition.getPosition().getBearing();
        }

        if (vehiclePosition.hasVehicle()) {
            newPosition.label = vehiclePosition.getVehicle().getLabel();
            if (newPosition.label == null) {
                newPosition.label = vehiclePosition.getVehicle().getLicensePlate();
            }
            newPosition.vehicleId = vehiclePosition.getVehicle().getId();
        }

        if (vehiclePosition.hasCurrentStatus()) {
            newPosition.stopStatus = vehiclePosition.getCurrentStatus();
        }

        if (vehiclePosition.hasCongestionLevel()) {
            newPosition.congestionLevel = vehiclePosition.getCongestionLevel();
        }

        if (vehiclePosition.hasTimestamp()) {
            newPosition.seconds = vehiclePosition.getTimestamp();
        }

        if (vehiclePosition.hasStopId()) {
            List<TransitStop> matchedStops = stopsOnVehicleTrip
                    .stream()
                    .filter(stop -> stop.getStopId().getId().equals(vehiclePosition.getStopId()))
                    .collect(Collectors.toList());
            if (matchedStops.size() == 1) {
                newPosition.nextStopName = matchedStops.get(0).getName();
                newPosition.nextStopId = matchedStops.get(0).getStopId();
            }
        }

        if (vehiclePosition.hasCurrentStopSequence()) {
            newPosition.nextStopSequenceId = vehiclePosition.getCurrentStopSequence();
        }

        return newPosition;
    }
}
