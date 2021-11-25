package org.opentripplanner.updater.vehicle_rental.datasources;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests the mapping between data coming from a {@link GbfsFeedLoader} to OTP station models.
 */
class GbfsVehicleRentalDataSourceTest {
    @Test
    void makeStationFromV22() {
        var dataSource = new GbfsVehicleRentalDataSource(new GbfsVehicleRentalDataSourceParameters(
                "file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json",
                "nb",
                false,
                new HashMap<>()
        ));

        dataSource.setup();

        assertTrue(dataSource.update());

        List<VehicleRentalPlace> stations = dataSource.getStations();
        assertEquals(6, stations.size());
        assertTrue(stations.stream().anyMatch(vehicleRentalStation -> vehicleRentalStation.getName().toString().equals("TORVGATA")));
        assertTrue(stations.stream().allMatch(vehicleRentalStation -> vehicleRentalStation.isAllowDropoff()));
        assertTrue(stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.isFloatingVehicle()));
        assertTrue(stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.isCarStation()));
        assertTrue(stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.isKeepingVehicleRentalAtDestinationAllowed()));

    }

    @Test
    void makeStationFromV10() {
        var dataSource = new GbfsVehicleRentalDataSource(new GbfsVehicleRentalDataSourceParameters(
                "file:src/test/resources/gbfs/helsinki/gbfs.json",
                "en",
                false,
                new HashMap<>()
        ));

        dataSource.setup();

        assertTrue(dataSource.update());

        List<VehicleRentalPlace> stations = dataSource.getStations();
        assertEquals(10, stations.size());
        assertTrue(stations.stream().anyMatch(vehicleRentalStation -> vehicleRentalStation.getName().toString().equals("Kasarmitori")));
        assertTrue(stations.stream().anyMatch(vehicleRentalStation -> vehicleRentalStation.isAllowDropoff()));
        assertTrue(stations.stream().anyMatch(vehicleRentalStation -> !vehicleRentalStation.isAllowDropoff()));
        assertTrue(stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.isFloatingVehicle()));
        assertTrue(stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.isCarStation()));
        assertTrue(stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.isKeepingVehicleRentalAtDestinationAllowed()));

    }
}