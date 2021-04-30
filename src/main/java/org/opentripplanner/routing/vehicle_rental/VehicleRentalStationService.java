package org.opentripplanner.routing.vehicle_rental;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.model.FeedScopedId;

public class VehicleRentalStationService implements Serializable {
    private static final long serialVersionUID = -1288992939159246764L;

    private final Map<FeedScopedId, VehicleRentalPlace> vehicleRentalStations = new HashMap<>();

    public Collection<VehicleRentalPlace> getVehicleRentalStations() {
        return vehicleRentalStations.values();
    }

    public VehicleRentalPlace getVehicleRentalStation(FeedScopedId id) {
        return vehicleRentalStations.get(id);
    }

    public void addVehicleRentalStation(VehicleRentalPlace vehicleRentalStation) {
        // Remove old reference first, as adding will be a no-op if already present
        vehicleRentalStations.remove(vehicleRentalStation.getId());
        vehicleRentalStations.put(vehicleRentalStation.getId(), vehicleRentalStation);
    }

    public void removeVehicleRentalStation(FeedScopedId vehicleRentalStationId) {
        vehicleRentalStations.remove(vehicleRentalStationId);
    }

    /**
     * Gets all the vehicle rental stations inside the envelope. This is currently done by iterating
     * over a set, but we could use a spatial index if the number of vehicle rental stations is high
     * enough for performance to be a concern.
     */
    public List<VehicleRentalPlace> getVehicleRentalStationForEnvelope(
        double minLon, double minLat, double maxLon, double maxLat
    ) {
        Envelope envelope = new Envelope(
            new Coordinate(minLon, minLat),
            new Coordinate(maxLon, maxLat)
        );

        return vehicleRentalStations
            .values()
            .stream()
            .filter(b -> envelope.contains(new Coordinate(b.getLongitude(), b.getLatitude())))
            .collect(Collectors.toList());
    }
}
