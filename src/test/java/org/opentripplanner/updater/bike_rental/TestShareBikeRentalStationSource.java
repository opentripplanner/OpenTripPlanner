package org.opentripplanner.updater.bike_rental;

import junit.framework.TestCase;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.List;

public class TestShareBikeRentalStationSource extends TestCase {

    public void testShareBike() throws UnsupportedEncodingException, MalformedURLException {

        ShareBikeRentalDataSource shareBikeSource = new ShareBikeRentalDataSource();
        shareBikeSource.setUrl("file:src/test/resources/bike/share-bike.json?SystemID=dummyid");
        assertTrue(shareBikeSource.update());
        List<BikeRentalStation> rentalStations = shareBikeSource.getStations();
        assertEquals(17, rentalStations.size());
        for (BikeRentalStation rentalStation : rentalStations) {
            System.out.println(rentalStation);
        }
        BikeRentalStation prinsen = rentalStations.get(0);
        
        assertTrue(prinsen.networks.contains("dummyid"));
        
        assertEquals("01", prinsen.name.toString());
        assertEquals("dummyid_1", prinsen.id);
        assertEquals(10.392981, prinsen.x);
        assertEquals(63.426637, prinsen.y);
        assertEquals(9, prinsen.spacesAvailable);
        assertEquals(6, prinsen.bikesAvailable);
    }
}
