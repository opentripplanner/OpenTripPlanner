package org.opentripplanner.updater.bike_rental;

import java.util.List;

import junit.framework.TestCase;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;

public class TestBikeRentalStationSource extends TestCase {

    public void testKeolisRennes() {

        KeolisRennesBikeRentalDataSource rennesSource = new KeolisRennesBikeRentalDataSource();
        rennesSource.setUrl("file:src/test/resources/bike/keolis-rennes.xml");
        assertTrue(rennesSource.update());
        List<BikeRentalStation> rentalStations = rennesSource.getStations();
        assertEquals(4, rentalStations.size());
        for (BikeRentalStation rentalStation : rentalStations) {
            System.out.println(rentalStation);
        }
        BikeRentalStation stSulpice = rentalStations.get(0);
        assertEquals("ZAC SAINT SULPICE", stSulpice.name.toString());
        assertEquals("75", stSulpice.id);
        assertEquals(-1.63528, stSulpice.x);
        assertEquals(48.1321, stSulpice.y);
        assertEquals(24, stSulpice.spacesAvailable);
        assertEquals(6, stSulpice.bikesAvailable);
        BikeRentalStation kergus = rentalStations.get(3);
        assertEquals("12", kergus.id);
    }

    public void testSmoove() {
        SmooveBikeRentalDataSource source = new SmooveBikeRentalDataSource();
        source.setUrl("file:src/test/resources/bike/smoove.json");
        assertTrue(source.update());
        List<BikeRentalStation> rentalStations = source.getStations();

        // Invalid station without coordinates shoulf be ignored, so only 3
        assertEquals(3, rentalStations.size());
        for (BikeRentalStation rentalStation : rentalStations) {
            System.out.println(rentalStation);
        }

        BikeRentalStation hamn = rentalStations.get(0);
        assertEquals("Hamn", hamn.name.toString());
        assertEquals("A04", hamn.id);
        // Ignore whitespace in coordinates string
        assertEquals(24.952269, hamn.x);
        assertEquals(60.167913, hamn.y);
        assertEquals(11, hamn.spacesAvailable);
        assertEquals(1, hamn.bikesAvailable);

        BikeRentalStation fake = rentalStations.get(1);
        assertEquals("Fake", fake.name.toString());
        assertEquals("B05", fake.id);
        assertEquals(24.0, fake.x);
        assertEquals(60.0, fake.y);
        // operative: false overrides available bikes and slots
        assertEquals(0, fake.spacesAvailable);
        assertEquals(0, fake.bikesAvailable);

        BikeRentalStation foo = rentalStations.get(2);
        assertEquals("Foo", foo.name.toString());
        assertEquals("B06", foo.id);
        assertEquals(25.0, foo.x);
        assertEquals(61.0, foo.y);
        assertEquals(5, foo.spacesAvailable);
        assertEquals(5, foo.bikesAvailable);
        // Ignores mismatch with total_slots
    }
}
