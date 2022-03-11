package org.opentripplanner.routing.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.vehicle_position.RealtimeVehiclePosition;

public class RealtimeVehiclePositionService {

    private final Multimap<TripPattern, RealtimeVehiclePosition> positions = HashMultimap.create();

    public void addVehiclePosition(TripPattern pattern, RealtimeVehiclePosition position) {
       positions.put(pattern, position);
    }

    public void clearVehiclePositions(TripPattern pattern) {
        positions.removeAll(pattern);
    }

    public Collection<RealtimeVehiclePosition> getVehiclePositions(TripPattern pattern) {
        return List.copyOf(positions.get(pattern));
    }
}
