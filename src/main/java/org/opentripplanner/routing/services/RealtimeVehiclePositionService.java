package org.opentripplanner.routing.services;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.vehicle_position.RealtimeVehiclePosition;

public class RealtimeVehiclePositionService {

    private final Map<TripPattern, List<RealtimeVehiclePosition>> positions =
            new ConcurrentHashMap<>();

    /**
     * For the given pattern set all realtime vehicle positions.
     * <p>
     * The list is expected to be exhaustive: all existing positions will be overridden.
     */
    public void setVehiclePositions(TripPattern pattern, List<RealtimeVehiclePosition> updates) {
        positions.put(pattern, List.copyOf(updates));
    }

    /**
     * Remove all vehicle positions for a given pattern.
     * <p>
     * This is useful to clear old vehicles for which there are no more updates and we assume that
     * they have stopped their trip.
     */
    public void clearVehiclePositions(TripPattern pattern) {
        positions.remove(pattern);
    }

    /**
     * Get the vehicle positions for a certain trip.
     */
    public Collection<RealtimeVehiclePosition> getVehiclePositions(TripPattern pattern) {
        return positions.getOrDefault(pattern, List.of());
    }
}
