/* This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of
the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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

    private static NoApiTransportationNetworkCompanyDataSource source = new NoApiTransportationNetworkCompanyDataSource(
        123,
        true
    );

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
