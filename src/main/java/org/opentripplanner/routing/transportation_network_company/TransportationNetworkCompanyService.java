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

package org.opentripplanner.routing.transportation_network_company;

import org.opentripplanner.api.model.Place;
import org.opentripplanner.updater.transportation_network_company.TransportationNetworkCompanyDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class TransportationNetworkCompanyService implements Serializable {

    private static Logger LOG = LoggerFactory.getLogger(TransportationNetworkCompanyService.class);

    private Map<TransportationNetworkCompany, TransportationNetworkCompanyDataSource> sources =
        new HashMap<TransportationNetworkCompany, TransportationNetworkCompanyDataSource>();

    public void addSource(TransportationNetworkCompanyDataSource source) {
        sources.put(source.getType(), source);
    }

    public List<ArrivalTime> getArrivalTimes(
        Place place,
        String companies
    ) throws ExecutionException, InterruptedException {

        List<ArrivalTime> arrivalTimes = new ArrayList<ArrivalTime>();

        ArrayList<TransportationNetworkCompanyDataSource> companiesToRequestFrom
            = new ArrayList<TransportationNetworkCompanyDataSource>();

        // parse list of tnc companies
        for (String company : companies.split(",")) {
            companiesToRequestFrom.add(getTransportationNetworkCompanyDataSource(company));
        }

        if (companiesToRequestFrom.size() == 0) {
            return arrivalTimes;
        }

        LOG.info("Finding TNC arrival times for " + companiesToRequestFrom.size() + " companies");

        // add a request for all matching companies
        ExecutorService pool = Executors.newFixedThreadPool(10);
        List<Callable<List<ArrivalTime>>> tasks = new ArrayList<>();

        for (TransportationNetworkCompanyDataSource transportationNetworkCompany : companiesToRequestFrom) {
            tasks.add(new Callable<List<ArrivalTime>>() {
                public List<ArrivalTime> call() throws Exception {
                    LOG.info("Finding TNC arrival times for " + transportationNetworkCompany.getType());
                    return transportationNetworkCompany.getArrivalTimes(place.lat, place.lon);
                }
            });
        }

        List<Future<List<ArrivalTime>>> results = pool.invokeAll(tasks);

        LOG.info("Collecting results");

        for (Future<List<ArrivalTime>> future : results) {
            arrivalTimes.addAll(future.get());
        }
        pool.shutdown();

        return arrivalTimes;
    }

    public RideEstimate getRideEstimate(
        String company,
        String rideType,
        Place fromPlace,
        Place toPlace
    ) throws IOException, ExecutionException {
        TransportationNetworkCompanyDataSource source = getTransportationNetworkCompanyDataSource(company);
        return source.getRideEstimate(rideType, fromPlace.lat, fromPlace.lon, toPlace.lat, toPlace.lon);
    }

    private TransportationNetworkCompanyDataSource getTransportationNetworkCompanyDataSource(String company) {
        TransportationNetworkCompany co = TransportationNetworkCompany.valueOf(company);
        if (co == null) {
            throw new UnsupportedOperationException("Transportation Network Company value " +
                company +
                " is not a valid type"
            );
        }

        if (!sources.containsKey(co)) {
            throw new UnsupportedOperationException("Transportation Network Company value " +
                company +
                " is not configured in this router"
            );
        }

        return sources.get(co);
    }
}
