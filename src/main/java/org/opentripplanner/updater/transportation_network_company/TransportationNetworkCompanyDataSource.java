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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public abstract class TransportationNetworkCompanyDataSource {
    // This value should be no longer than 30 minutes (according to Uber API docs) TODO check Lyft time limit
    private static final int cacheTimeSeconds = 120;

    private Cache<Position, List<ArrivalTime>> arrivalTimeCache =
        CacheBuilder.newBuilder().expireAfterWrite(cacheTimeSeconds, TimeUnit.SECONDS).build();
    private Cache<RideEstimateRequest, List<RideEstimate>> rideEstimateCache =
        CacheBuilder.newBuilder().expireAfterWrite(cacheTimeSeconds, TimeUnit.SECONDS).build();

    public abstract TransportationNetworkCompany getType();

    // get the next arrivals for a specific location
    public List<ArrivalTime> getArrivalTimes(double latitude, double longitude) throws ExecutionException {
        Position position = new Position(truncateValue(latitude), truncateValue(longitude));
        return arrivalTimeCache.get(position, () -> queryArrivalTimes(position));
    }

    protected abstract List<ArrivalTime> queryArrivalTimes(Position position) throws IOException;

    public abstract List<String> getAcceptedRideTypes ();

    // get the estimated trip time
    public RideEstimate getRideEstimate(
        String rideType,
        double startLatitude,
        double startLongitude,
        double endLatitude,
        double endLongitude
    ) throws ExecutionException {
        // Truncate lat/lon values in order to reduce the number of API requests made.
        double roundedStartLat = truncateValue(startLatitude);
        double roundedStartLon = truncateValue(startLongitude);
        double roundedEndLat = truncateValue(endLatitude);
        double roundedEndLon = truncateValue(endLongitude);
        RideEstimateRequest request = new RideEstimateRequest(roundedStartLat, roundedStartLon, roundedEndLat, roundedEndLon);
        List<RideEstimate> rideEstimates = rideEstimateCache.get(request, () -> queryRideEstimates(request));


        for (RideEstimate rideEstimate : rideEstimates) {
            if (rideEstimate.rideType.equals(rideType)) {
                return rideEstimate;
            }
        }

        return null;
    }

    /**
     * Truncate double values by the specified precision factor.
     */
    private static double truncateValue(double value) {
        double precisionFactor = 10000.0;
        return Math.round(value * precisionFactor) / precisionFactor;
    }

    protected abstract List<RideEstimate> queryRideEstimates(
        RideEstimateRequest request
    ) throws IOException;
}
