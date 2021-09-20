package org.opentripplanner.ext.smoovebikerental;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

class SmooveBikeRentalDataSourceTest {

    @Test
    void makeStation() {
        SmooveBikeRentalDataSource source = new SmooveBikeRentalDataSource(
                new SmooveBikeRentalDataSourceParameters(
                        "file:src/test/resources/bike/smoove.json",
                        null,
                        Map.of()
                )
        );
        assertTrue(source.update());
        List<VehicleRentalStation> rentalStations = source.getStations();

        // Invalid station without coordinates shoulf be ignored, so only 3
        assertEquals(3, rentalStations.size());
        for (VehicleRentalStation rentalStation : rentalStations) {
            System.out.println(rentalStation);
        }

        VehicleRentalStation hamn = rentalStations.get(0);
        assertEquals("Hamn", hamn.name.toString());
        assertEquals("A04", hamn.getStationId());
        // Ignore whitespace in coordinates string
        assertEquals(24.952269, hamn.longitude);
        assertEquals(60.167913, hamn.latitude);
        assertEquals(11, hamn.spacesAvailable);
        assertEquals(1, hamn.vehiclesAvailable);

        VehicleRentalStation fake = rentalStations.get(1);
        assertEquals("Fake", fake.name.toString());
        assertEquals("B05", fake.getStationId());
        assertEquals(24.0, fake.longitude);
        assertEquals(60.0, fake.latitude);
        // operative: false overrides available bikes and slots
        assertEquals(0, fake.spacesAvailable);
        assertEquals(0, fake.vehiclesAvailable);

        VehicleRentalStation foo = rentalStations.get(2);
        assertEquals("Foo", foo.name.toString());
        assertEquals("B06", foo.getStationId());
        assertEquals(25.0, foo.longitude);
        assertEquals(61.0, foo.latitude);
        assertEquals(5, foo.spacesAvailable);
        assertEquals(5, foo.vehiclesAvailable);
        // Ignores mismatch with total_slots
    }
}