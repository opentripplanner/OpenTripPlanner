package org.opentripplanner.updater.bike_rental.datasources;

import junit.framework.TestCase;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.updater.bike_rental.datasources.params.BikeRentalDataSourceParameters;

import java.util.List;

public class TestShareBikeRentalStationSource extends TestCase {

    public void testShareBike() {

        ShareBikeRentalDataSource shareBikeSource =
            new ShareBikeRentalDataSource(new BikeRentalDataSourceParameters(
                // Only needed to create the data source
                null,
                "file:src/test/resources/bike/share-bike.json?SystemID=dummyid",
                null,
                null
            ));
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
            new ShareBikeRentalDataSource(
                new BikeRentalDataSourceParameters(
                    // Only needed to create the data source
                    null,
                    "file:src/test/resources/bike/share-bike.json",
                    null,
                    null
                )
            );
        assertTrue(shareBikeSource.update());
        List<BikeRentalStation> rentalStations = shareBikeSource.getStations();
        BikeRentalStation prinsen = rentalStations.get(0);
        
        //  Should be random value
        assertFalse(prinsen.networks.contains("dummyid"));
    }
}
