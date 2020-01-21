package org.opentripplanner.updater.transportation_network_company;

import org.junit.Test;
import org.opentripplanner.routing.transportation_network_company.ArrivalTime;
import org.opentripplanner.routing.transportation_network_company.RideEstimate;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NoApiTransportationNetworkCompanyDataSourceTest {

    private static NoApiTransportationNetworkCompanyDataSource source = new NoApiTransportationNetworkCompanyDataSource();

    @Test
    public void testGetArrivalTimes () throws IOException, ExecutionException {
        List<ArrivalTime> arrivalTimes = source.getArrivalTimes(1.2, 3.4);

        assertEquals(arrivalTimes.size(),  1);
        ArrivalTime arrival = arrivalTimes.get(0);
        assertEquals("no-api-tnc-service", arrival.displayName);
        assertEquals("no-api-tnc-service", arrival.productId);
        assertEquals(123, arrival.estimatedSeconds);
        assertEquals(true, arrival.wheelchairAccessible);
    }

    @Test
    public void testGetEstimatedRideTime () throws IOException, ExecutionException {
        List<RideEstimate> rideEstimates = source.getRideEstimates(
            1.2,
            3.4,
            1.201,
            3.401
        );

        assertEquals(rideEstimates.size(), 1);
        RideEstimate rideEstimate = rideEstimates.get(0);
        assertEquals("no-api-tnc-service", rideEstimate.rideType);
        assertEquals(0, rideEstimate.duration);
        assertEquals(0, rideEstimate.minCost, 0.001);
        assertEquals(0, rideEstimate.maxCost, 0.001);
        assertEquals(true, rideEstimate.wheelchairAccessible);
    }
}
