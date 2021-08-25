package org.opentripplanner.updater.vehicle_positions;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import org.opentripplanner.index.model.RealtimeVehiclePosition;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Responsible for converting vehicle positions in memory to exportable ones, and associating each position
 * with a pattern.
 */
public class VehiclePositionSnapshotSource {
    private static final Logger LOG =
            LoggerFactory.getLogger(VehiclePositionSnapshotSource.class);

    private final GraphIndex graphIndex;

    public VehiclePositionSnapshotSource(Graph graph) {
        graphIndex = graph.index;
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
            }

            String tripId = vehiclePosition.getTrip().getTripId();
            Trip trip = graphIndex.tripForId.get(new FeedScopedId(feedId, tripId));
            if (trip == null) {
                LOG.warn("Unable to find OTP trip ID for vehicle position " + vehiclePosition);
                continue;
            }

            TripPattern pattern = graphIndex.patternForTrip.get(trip);
            if (pattern == null) {
                LOG.warn("Unable to match OTP pattern ID for vehicle position " + vehiclePosition);
                continue;
            }

            RealtimeVehiclePosition newPosition = parseVehiclePosition(vehiclePosition);
            if (pattern.vehiclePositions == null) {
                pattern.vehiclePositions = new HashMap<>();
            }
            pattern.vehiclePositions.put(tripId, newPosition);
        }
    }

    /**
     * Converts GtfsRealtime vehicle position to the OTP RealtimeVehiclePosition which can be
     * used by the API.
     * @param vehiclePosition   GtfsRealtime vehicle position
     * @return                  OTP RealtimeVehiclePosition
     */
    private RealtimeVehiclePosition parseVehiclePosition(VehiclePosition vehiclePosition) {
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
            newPosition.id = vehiclePosition.getVehicle().getId();
        }

        if (vehiclePosition.hasCurrentStatus()) {
            newPosition.stopStatus = vehiclePosition.getCurrentStatus();
        }

        if (vehiclePosition.hasCongestionLevel()) {
            newPosition.congestionLevel = vehiclePosition.getCongestionLevel();
        }
        return newPosition;
    }
}
