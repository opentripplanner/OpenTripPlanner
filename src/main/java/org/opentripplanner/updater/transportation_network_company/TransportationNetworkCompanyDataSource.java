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

    private static final Logger LOG = LoggerFactory.getLogger(TransportationNetworkCompanyDataSource.class);

    private static final int cacheTimeSeconds = 30;

    private Cache<Position, List<ArrivalTime>> arrivalTimeCache =
        CacheBuilder.newBuilder().expireAfterWrite(cacheTimeSeconds, TimeUnit.SECONDS).build();
    private Cache<RideEstimateRequest, List<RideEstimate>> rideEstimateCache =
        CacheBuilder.newBuilder().expireAfterWrite(cacheTimeSeconds, TimeUnit.SECONDS).build();

    public abstract TransportationNetworkCompany getType();

    // get the next arrivals for a specific location
    public List<ArrivalTime> getArrivalTimes(double latitude, double longitude) throws IOException, ExecutionException {
        Position request = new Position(latitude, longitude);
        return arrivalTimeCache.get(
            request,
            new Callable<List<ArrivalTime>>() {
                @Override
                public List<ArrivalTime> call() throws IOException {
                    return queryArrivalTimes(request);
                }
            });
    }

    protected abstract List<ArrivalTime> queryArrivalTimes(Position request) throws IOException;

    // get the estimated trip time
    public RideEstimate getRideEstimate(
        String rideType,
        double startLatitude,
        double startLongitude,
        double endLatitude,
        double endLongitude
    ) throws IOException, ExecutionException {
        RideEstimateRequest request = new RideEstimateRequest(startLatitude, startLongitude, endLatitude, endLongitude);
        List<RideEstimate> rideEstimates = rideEstimateCache.get(
            request,
            new Callable<List<RideEstimate>>() {
                @Override
                public List<RideEstimate> call() throws IOException {
                    return queryRideEstimates(request);
                }
            });


        for (RideEstimate rideEstimate : rideEstimates) {
            if (rideEstimate.rideType.equals(rideType)) {
                return rideEstimate;
            }
        }

        return null;
    }

    protected abstract List<RideEstimate> queryRideEstimates(
        RideEstimateRequest request
    ) throws IOException;
}
