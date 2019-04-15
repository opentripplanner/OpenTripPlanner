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
        SmooveBikeRentalDataSource source = new SmooveBikeRentalDataSource(null);
        source.setUrl("file:src/test/resources/bike/smoove.json");
        assertTrue(source.update());
        List<BikeRentalStation> rentalStations = source.getStations();

        // Invalid station without coordinates should be ignored, so only 3
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
        assertEquals("[smoove]", hamn.networks.toString());

        BikeRentalStation fake = rentalStations.get(1);
        assertEquals("Fake", fake.name.toString());
        assertEquals("B05", fake.id);
        assertEquals("Station off", fake.state);
        assertEquals(24.0, fake.x);
        assertEquals(60.0, fake.y);

        BikeRentalStation foo = rentalStations.get(2);
        assertEquals("Foo", foo.name.toString());
        assertEquals("B06", foo.id);
        assertEquals(25.0, foo.x);
        assertEquals(61.0, foo.y);
        assertEquals(5, foo.spacesAvailable);
        assertEquals(5, foo.bikesAvailable);
        // Ignores mismatch with total_slots

        // Test giving network name to data source
        SmooveBikeRentalDataSource sourceWithCustomNetwork = new SmooveBikeRentalDataSource("Helsinki");
        sourceWithCustomNetwork.setUrl("file:src/test/resources/bike/smoove.json");
        assertTrue(sourceWithCustomNetwork.update());
        List<BikeRentalStation> rentalStationsWithCustomNetwork = sourceWithCustomNetwork.getStations();
        BikeRentalStation hamnWithCustomNetwork  = rentalStationsWithCustomNetwork.get(0);
        assertEquals("[Helsinki]", hamnWithCustomNetwork.networks.toString());
    }

    public void testSamocat() {
        SamocatScooterRentalDataSource source = new SamocatScooterRentalDataSource(null);
        source.setUrl("file:src/test/resources/bike/samocat.json");
        assertTrue(source.update());
        List<BikeRentalStation> rentalStations = source.getStations();

        // Invalid station without coordinates should be ignored, so only 3
        assertEquals(3, rentalStations.size());
        for (BikeRentalStation rentalStation : rentalStations) {
            System.out.println(rentalStation);
        }

        BikeRentalStation testikuja = rentalStations.get(0);
        assertEquals("Testikuja 3", testikuja.name.toString());
        assertEquals("0451", testikuja.id);
        // Ignore whitespace in coordinates string
        assertEquals(24.9355143, testikuja.x);
        assertEquals(60.1637284, testikuja.y);
        assertEquals(0, testikuja.spacesAvailable);
        assertEquals(0, testikuja.bikesAvailable);
        assertEquals("Station on", testikuja.state);
        assertEquals("[samocat]", testikuja.networks.toString());

        BikeRentalStation footie = rentalStations.get(1);
        assertEquals("Footie 3", footie.name.toString());
        assertEquals("0450", footie.id);
        assertEquals(3, footie.spacesAvailable);
        assertEquals(4, footie.bikesAvailable);
        assertEquals(24.958877, footie.x);
        assertEquals(60.194449, footie.y);

        BikeRentalStation bartie = rentalStations.get(2);
        assertEquals("Bartie 10", bartie.name.toString());
        assertEquals("3451", bartie.id);
        assertEquals(24.9537278, bartie.x);
        assertEquals(60.2177349, bartie.y);
        assertEquals(5, bartie.spacesAvailable);
        assertEquals(1, bartie.bikesAvailable);
        // Ignores mismatch with total_slots

        // Test giving network name to data source
        SamocatScooterRentalDataSource sourceWithCustomNetwork = new SamocatScooterRentalDataSource("vuosaari");
        sourceWithCustomNetwork.setUrl("file:src/test/resources/bike/samocat.json");
        assertTrue(sourceWithCustomNetwork.update());
        List<BikeRentalStation> rentalStationsWithCustomNetwork = sourceWithCustomNetwork.getStations();
        BikeRentalStation testitieWithCustomNetwork  = rentalStationsWithCustomNetwork.get(0);
        assertEquals("[vuosaari]", testitieWithCustomNetwork.networks.toString());
    }
}
