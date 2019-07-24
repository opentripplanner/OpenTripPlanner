package org.opentripplanner.updater.bike_rental;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import junit.framework.TestCase;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;

public class TestBikeRentalStationSource extends TestCase {
    
    private static final String NEXT_BIKE_TEST_DATA_URL = "file:src/test/resources/bike/next.xml";
    
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

    public void testSharingos() {
        SharingOSBikeRentalDataSource source = new SharingOSBikeRentalDataSource(null);
        source.setUrl("file:src/test/resources/bike/sharingos.json");
        assertTrue(source.update());
        List<BikeRentalStation> rentalStations = source.getStations();

        assertEquals(4, rentalStations.size());
        for (BikeRentalStation rentalStation : rentalStations) {
            System.out.println(rentalStation);
        }

        BikeRentalStation tempClosedStation = rentalStations.get(0);
        assertEquals("Temporarily closed test station", tempClosedStation.name.toString());
        assertEquals("1001", tempClosedStation.id);
        assertEquals(25.0515797, tempClosedStation.x);
        assertEquals(60.2930266, tempClosedStation.y);
        assertEquals(0, tempClosedStation.spacesAvailable);
        assertEquals(0, tempClosedStation.bikesAvailable);
        assertEquals("Station off", tempClosedStation.state);
        assertEquals("[sharingos]", tempClosedStation.networks.toString());

        BikeRentalStation permClosedStation = rentalStations.get(1);
        assertEquals("Closed test station", permClosedStation.name.toString());
        assertEquals("1002", permClosedStation.id);
        assertEquals(0, permClosedStation.spacesAvailable);
        assertEquals(0, permClosedStation.bikesAvailable);
        assertEquals(25.0364107, permClosedStation.x);
        assertEquals(60.2934127, permClosedStation.y);
        assertEquals("Station closed", permClosedStation.state);

        BikeRentalStation openTestStation = rentalStations.get(2);
        assertEquals("Open test station", openTestStation.name.toString());
        assertEquals("1003", openTestStation.id);
        assertEquals(25.0424261, openTestStation.x);
        assertEquals(60.2932159, openTestStation.y);
        assertEquals(3, openTestStation.spacesAvailable);
        assertEquals(2, openTestStation.bikesAvailable);
        assertEquals("Station on", openTestStation.state);

        BikeRentalStation overflowTestStation = rentalStations.get(3);
        assertEquals("Overflow test station", overflowTestStation.name.toString());
        assertEquals("1004", overflowTestStation.id);
        assertEquals(25.0434261, overflowTestStation.x);
        assertEquals(60.2931159, overflowTestStation.y);
        assertEquals(0, overflowTestStation.spacesAvailable);
        assertEquals(7, overflowTestStation.bikesAvailable);
        assertEquals("Station on", overflowTestStation.state);

        // Test giving network name to data source
        SharingOSBikeRentalDataSource sourceWithCustomNetwork = new SharingOSBikeRentalDataSource("vantaa");
        sourceWithCustomNetwork.setUrl("file:src/test/resources/bike/sharingos.json");
        assertTrue(sourceWithCustomNetwork.update());
        List<BikeRentalStation> rentalStationsWithCustomNetwork = sourceWithCustomNetwork.getStations();
        BikeRentalStation tempClosedStationWithCustomNetwork  = rentalStationsWithCustomNetwork.get(0);
        assertEquals("[vantaa]", tempClosedStationWithCustomNetwork.networks.toString());
    }

    public void testNext() {
        NextBikeRentalDataSource source = createAndUpdateNextBikeRentalDataSource(NEXT_BIKE_TEST_DATA_URL, null);
        List<BikeRentalStation> rentalStations = source.getStations();
        assertEquals(3, rentalStations.size());
        
        Map<String,BikeRentalStation> stationByName = rentalStations.stream()
                .peek(System.out::println)
                .collect(Collectors.toMap(BikeRentalStation::getName, station -> station));
        
        BikeRentalStation fullStation = stationByName.get("full station");
        assertEquals("1", fullStation.id);
        assertEquals(20.971, fullStation.x);
        assertEquals(52.261, fullStation.y);
        assertEquals(0, fullStation.spacesAvailable);
        assertEquals(2, fullStation.bikesAvailable);
        assertEquals("Station on", fullStation.state);
        assertEquals("[NextBike]", fullStation.networks.toString());
        
        BikeRentalStation freeStation = stationByName.get("free station");
        assertEquals("2", freeStation.id);
        assertEquals(20.972, freeStation.x);
        assertEquals(52.262, freeStation.y);
        assertEquals(1, freeStation.spacesAvailable);
        assertEquals(1, freeStation.bikesAvailable);
        
        BikeRentalStation noFreeRacksInfoStation = stationByName.get("no free racks station");
        assertEquals("3", noFreeRacksInfoStation.id);
        assertEquals(20.973, noFreeRacksInfoStation.x);
        assertEquals(52.263, noFreeRacksInfoStation.y);
        assertEquals(2, noFreeRacksInfoStation.spacesAvailable);
        assertEquals(0, noFreeRacksInfoStation.bikesAvailable);
        
        // Test giving network name to data source
        NextBikeRentalDataSource sourceWithCustomNetwork = createAndUpdateNextBikeRentalDataSource(NEXT_BIKE_TEST_DATA_URL, "oulu");
        List<BikeRentalStation> rentalStationsWithCustomNetwork = sourceWithCustomNetwork.getStations();
        BikeRentalStation tempClosedStationWithCustomNetwork  = rentalStationsWithCustomNetwork.get(0);
        assertEquals("[oulu]", tempClosedStationWithCustomNetwork.networks.toString());
    }
    
    private NextBikeRentalDataSource createAndUpdateNextBikeRentalDataSource(String url, String networkName) {
        NextBikeRentalDataSource source = new NextBikeRentalDataSource(networkName);
        source.setUrl(url);
        assertTrue(source.update());
        return source;
    }
    
}
