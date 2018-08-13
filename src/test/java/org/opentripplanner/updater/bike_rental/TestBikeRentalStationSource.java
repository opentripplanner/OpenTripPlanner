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
}
