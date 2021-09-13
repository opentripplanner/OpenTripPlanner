package org.opentripplanner.updater.vehicle_rental.datasources;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
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

    @Test
    void makeStationFromV10() {
        var dataSource = new GbfsVehicleRentalDataSource(new GbfsVehicleRentalDataSourceParameters(
                "file:src/test/resources/gbfs/helsinki/gbfs.json",
                "en",
                false,
                false,
                new HashMap<>()
        ));

        dataSource.setup();

        assertTrue(dataSource.update());

        List<VehicleRentalStation> stations = dataSource.getStations();
        assertEquals(10, stations.size());
        assertTrue(stations.stream().anyMatch(vehicleRentalStation -> vehicleRentalStation.name.toString().equals("Kasarmitori")));
        assertTrue(stations.stream().anyMatch(vehicleRentalStation -> vehicleRentalStation.allowDropoff));
        assertTrue(stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.isFloatingBike));
        assertTrue(stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.isCarStation));
        assertTrue(stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.isKeepingVehicleRentalAtDestinationAllowed));

    }
}