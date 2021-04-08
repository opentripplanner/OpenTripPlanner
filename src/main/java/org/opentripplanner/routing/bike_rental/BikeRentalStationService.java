package org.opentripplanner.routing.bike_rental;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.routing.bike_park.BikePark;

public class BikeRentalStationService implements Serializable {
    private static final long serialVersionUID = -1288992939159246764L;

    private Set<BikeRentalStation> bikeRentalStations = new HashSet<>();

    private Set<BikePark> bikeParks = new HashSet<>();

    public Collection<BikeRentalStation> getBikeRentalStations() {
        return bikeRentalStations;
    }

    public void addBikeRentalStation(BikeRentalStation bikeRentalStation) {
        // Remove old reference first, as adding will be a no-op if already present
        bikeRentalStations.remove(bikeRentalStation);
        bikeRentalStations.add(bikeRentalStation);
    }

    public void removeBikeRentalStation(BikeRentalStation bikeRentalStation) {
        bikeRentalStations.remove(bikeRentalStation);
    }

    public Collection<BikePark> getBikeParks() {
        return bikeParks;
    }

    public void addBikePark(BikePark bikePark) {
        // Remove old reference first, as adding will be a no-op if already present
        bikeParks.remove(bikePark);
        bikeParks.add(bikePark);
    }

    public void removeBikePark(BikePark bikePark) {
        bikeParks.remove(bikePark);
    }

    /**
     * Gets all the bike rental stations inside the envelope. This is currently done by iterating
     * over a set, but we could use a spatial index if the number of bike rental stations is high
     * enough for performance to be a concern.
     */
    public List<BikeRentalStation> getBikeRentalStationForEnvelope(
        double minLon, double minLat, double maxLon, double maxLat
    ) {
        Envelope envelope = new Envelope(
            new Coordinate(minLon, minLat),
            new Coordinate(maxLon, maxLat)
        );

        return bikeRentalStations
            .stream()
            .filter(b -> envelope.contains(new Coordinate(b.x, b.y)))
            .collect(Collectors.toList());
    }
}
