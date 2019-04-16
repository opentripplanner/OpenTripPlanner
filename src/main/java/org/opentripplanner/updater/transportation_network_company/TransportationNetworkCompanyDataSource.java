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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.opentripplanner.routing.transportation_network_company.ArrivalTime;
import org.opentripplanner.routing.transportation_network_company.RideEstimate;
import org.opentripplanner.routing.transportation_network_company.TransportationNetworkCompany;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public abstract class TransportationNetworkCompanyDataSource {
    // This value should be no longer than 30 minutes (according to Uber API docs) TODO check Lyft time limit
    private static final int cacheTimeSeconds = 120;

    private Cache<Position, List<ArrivalTime>> arrivalTimeCache =
        CacheBuilder.newBuilder().expireAfterWrite(cacheTimeSeconds, TimeUnit.SECONDS).build();
    private Cache<RideEstimateRequest, List<RideEstimate>> rideEstimateCache =
        CacheBuilder.newBuilder().expireAfterWrite(cacheTimeSeconds, TimeUnit.SECONDS).build();

    protected String wheelChairAccessibleRideType;

    // Abstract method to return the TransportationNetworkCompany enum type
    public abstract TransportationNetworkCompany getTransportationNetworkCompanyType();

    // get the next arrivals for a specific location
    public List<ArrivalTime> getArrivalTimes(
        double latitude,
        double longitude
    ) throws ExecutionException {
        Position position = new Position(latitude, longitude);
        return arrivalTimeCache.get(position, () -> queryArrivalTimes(position));
    }

    protected abstract List<ArrivalTime> queryArrivalTimes(Position position) throws IOException;

    // get the estimated trip time for a specific rideType
    public List<RideEstimate> getRideEstimates(
        double startLatitude,
        double startLongitude,
        double endLatitude,
        double endLongitude
    ) throws ExecutionException {
        // Truncate lat/lon values in order to reduce the number of API requests made.
        RideEstimateRequest request = new RideEstimateRequest(startLatitude, startLongitude, endLatitude, endLongitude);
        return rideEstimateCache.get(request, () -> queryRideEstimates(request));
    }

    protected abstract List<RideEstimate> queryRideEstimates(
        RideEstimateRequest request
    ) throws IOException;

    protected boolean productIsWheelChairAccessible(String productId) {
        return productId.equals(wheelChairAccessibleRideType);
    }
}