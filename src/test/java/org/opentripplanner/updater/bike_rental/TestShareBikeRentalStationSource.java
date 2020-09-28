package org.opentripplanner.updater.bike_rental;

import junit.framework.TestCase;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.UpdaterDataSourceParameters;

import java.util.List;

public class TestShareBikeRentalStationSource extends TestCase {

    public void testShareBike() {

        ShareBikeRentalDataSource shareBikeSource =
            new ShareBikeRentalDataSource(new UpdaterDataSourceParameters() {
                @Override
                public String getUrl() {
                    return "file:src/test/resources/bike/share-bike.json?SystemID=dummyid";
                }
              // Only needed to create the data source
              @Override public DataSourceType type() { return null; }
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

    public void testShareBikeMissingSystemIDParameter() {

        ShareBikeRentalDataSource shareBikeSource =
            new ShareBikeRentalDataSource(new UpdaterDataSourceParameters() {
                @Override
                public String getUrl() {
                    return "file:src/test/resources/bike/share-bike.json";
                }
              // Only needed to create the data source
              @Override public DataSourceType type() { return null; }
            });
        assertTrue(shareBikeSource.update());
        List<BikeRentalStation> rentalStations = shareBikeSource.getStations();
        BikeRentalStation prinsen = rentalStations.get(0);
        
        //  Should be random value
        assertFalse(prinsen.networks.contains("dummyid"));
    }
}
