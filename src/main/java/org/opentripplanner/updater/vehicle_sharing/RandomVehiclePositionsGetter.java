package org.opentripplanner.updater.vehicle_sharing;

import org.opentripplanner.routing.core.vehicle_sharing.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class RandomVehiclePositionsGetter extends VehiclePositionsGetter {
    int numberOfVehicles;
    Double minLon, maxLon, minLat, maxLat;

    public RandomVehiclePositionsGetter(int numberOfVehicles, Double minLon, Double maxLon, Double minLat, Double maxLat) {
        this.numberOfVehicles = numberOfVehicles;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.minLat = minLat;
        this.maxLat = maxLat;
    }

    @Override
    VehiclePositionsDiff getVehiclePositionsDiff() {
        Random random = new Random(1L);

        List<VehicleDescription> vehicleDescriptions = new LinkedList<>();

        for (int i = 0; i < numberOfVehicles; i++) {
            Double lon = minLon + (double) random.nextFloat() * (maxLon - minLon);
            Double lat = minLat + (double) random.nextFloat() * (maxLat - minLat);
            vehicleDescriptions.add(new CarDescription(lon, lat, FuelType.ELECTRIC, Gearbox.AUTOMAT, Provider.INNOGY));
        }
        return new VehiclePositionsDiff(vehicleDescriptions, 0L, 0L);
    }
}
