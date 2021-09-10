package org.opentripplanner.updater.vehicle_rental.datasources;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GbfsVehicleRentalDataSourceTest {
    @Test
    void makeStation() {
        var dataSource = new GbfsVehicleRentalDataSource(new GbfsVehicleRentalDataSourceParameters(
                "file:src/test/resources/gbfs/gbfs.json",
                "nb",
                false,
                false,
                new HashMap<>()
        ));

        dataSource.setup();

        assertTrue(dataSource.update());

        List<VehicleRentalStation> stations = dataSource.getStations();
        assertEquals(6, stations.size());
        assertTrue(stations.stream().anyMatch(vehicleRentalStation -> vehicleRentalStation.name.toString().equals("TORVGATA")));
        assertTrue(stations.stream().allMatch(vehicleRentalStation -> vehicleRentalStation.allowDropoff));
        assertTrue(stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.isFloatingBike));
        assertTrue(stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.isCarStation));
        assertTrue(stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.isKeepingVehicleRentalAtDestinationAllowed));

    }
}