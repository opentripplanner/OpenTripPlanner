package org.opentripplanner.ext.smoovebikerental;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;

class SmooveBikeRentalDataSourceTest {

    @Test
    void makeStation() {
        SmooveBikeRentalDataSource source = new SmooveBikeRentalDataSource(
                new SmooveBikeRentalDataSourceParameters(
                        "file:src/test/resources/bike/smoove.json",
                        null,
                        true,
                        Map.of()
                )
        );
        assertTrue(source.update());
        List<VehicleRentalPlace> rentalStations = source.getStations();

        // Invalid station without coordinates shoulf be ignored, so only 3
        assertEquals(3, rentalStations.size());
        for (VehicleRentalPlace rentalStation : rentalStations) {
            System.out.println(rentalStation);
        }

        VehicleRentalPlace hamn = rentalStations.get(0);
        assertEquals("Hamn", hamn.getName().toString());
        assertEquals("A04", hamn.getStationId());
        // Ignore whitespace in coordinates string
        assertEquals(24.952269, hamn.getLongitude());
        assertEquals(60.167913, hamn.getLatitude());
        assertEquals(11, hamn.getSpacesAvailable());
        assertEquals(1, hamn.getVehiclesAvailable());
        assertTrue(hamn.isAllowOverloading());
        assertTrue(hamn.allowDropoffNow());

        VehicleRentalPlace fake = rentalStations.get(1);
        assertEquals("Fake", fake.getName().toString());
        assertEquals("B05", fake.getStationId());
        assertEquals(24.0, fake.getLongitude());
        assertEquals(60.0, fake.getLatitude());
        // operative: false overrides available bikes and slots
        assertEquals(0, fake.getSpacesAvailable());
        assertEquals(0, fake.getVehiclesAvailable());
        assertTrue(fake.allowDropoffNow());

        VehicleRentalPlace foo = rentalStations.get(2);
        assertEquals("Foo", foo.getName().toString());
        assertEquals("B06", foo.getStationId());
        assertEquals(25.0, foo.getLongitude());
        assertEquals(61.0, foo.getLatitude());
        assertEquals(5, foo.getSpacesAvailable());
        assertEquals(5, foo.getVehiclesAvailable());
        assertTrue(foo.allowDropoffNow());
        // Ignores mismatch with total_slots
    }

    @Test
    void makeStationWithoutOverloading() {
        SmooveBikeRentalDataSource source = new SmooveBikeRentalDataSource(
                new SmooveBikeRentalDataSourceParameters(
                        "file:src/test/resources/bike/smoove.json",
                        null,
                        false,
                        Map.of()
                )
        );
        assertTrue(source.update());
        List<VehicleRentalPlace> rentalStations = source.getStations();

        VehicleRentalPlace hamn = rentalStations.get(0);
        assertEquals(11, hamn.getSpacesAvailable());
        assertFalse(hamn.isAllowOverloading());
        // spaces available and overloading is not allowed
        assertTrue(hamn.allowDropoffNow());

        VehicleRentalPlace fake = rentalStations.get(1);
        assertEquals(0, fake.getSpacesAvailable());
        // no spaces and overloading is not allowed
        assertFalse(fake.allowDropoffNow());
    }
}