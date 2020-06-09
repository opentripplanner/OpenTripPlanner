package org.opentripplanner.updater.bike_rental;

import junit.framework.TestCase;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.updater.UpdaterDataSourceParameters;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.List;

public class TestShareBikeRentalStationSource extends TestCase {

    public void testShareBike() {

        ShareBikeRentalDataSource shareBikeSource =
            new ShareBikeRentalDataSource(new UpdaterDataSourceParameters() {
                @Override
                public String getUrl() {
                    return "file:src/test/resources/bike/share-bike.json?SystemID=dummyid";
                }

                @Override
                public String getName() {
                    return null;
                }
            });
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

    public void testShareBikeMissingSystemIDParameter() throws UnsupportedEncodingException, MalformedURLException {

        ShareBikeRentalDataSource shareBikeSource =
            new ShareBikeRentalDataSource(new UpdaterDataSourceParameters() {
                @Override
                public String getUrl() {
                    return "file:src/test/resources/bike/share-bike.json";
                }

                @Override
                public String getName() {
                    return null;
                }
            });
        assertTrue(shareBikeSource.update());
        List<BikeRentalStation> rentalStations = shareBikeSource.getStations();
        BikeRentalStation prinsen = rentalStations.get(0);
        
        //  Should be random value
        assertFalse(prinsen.networks.contains("dummyid"));
    }
}
