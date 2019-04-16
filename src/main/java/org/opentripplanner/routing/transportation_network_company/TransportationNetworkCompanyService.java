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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TransportationNetworkCompanyService implements Serializable {

    private static Logger LOG = LoggerFactory.getLogger(TransportationNetworkCompanyService.class);

    private Map<TransportationNetworkCompany, TransportationNetworkCompanyDataSource> sources =
        new HashMap<>();

    public void addSource(TransportationNetworkCompanyDataSource source) {
        sources.put(source.getTransportationNetworkCompanyType(), source);
    }

    /**
     * Get the ETA estimates from the specified TNC companies
     *
     * @param companies  A comma-separated string listing the companies to request from
     * @param place  The pickup point from which to request an ETA from
     * @return A list of ArrivalEstimates.  If none are found, or no companies match, an empty list will be returned.
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public List<ArrivalTime> getArrivalTimes(
        String companies, Place place
    ) throws ExecutionException, InterruptedException {
        List<ArrivalTime> arrivalTimes = new ArrayList<>();

        List<TransportationNetworkCompanyDataSource> companiesToRequestFrom = parseCompanies(companies);
        if (companiesToRequestFrom.size() == 0) {
            return arrivalTimes;
        }

        LOG.debug("Finding TNC arrival times for {} companies", companiesToRequestFrom.size());

        // add a request for all matching companies
        ExecutorService pool = Executors.newFixedThreadPool(companiesToRequestFrom.size());
        List<Callable<List<ArrivalTime>>> tasks = new ArrayList<>();

        for (TransportationNetworkCompanyDataSource transportationNetworkCompany : companiesToRequestFrom) {
            tasks.add(() -> {
                LOG.debug("Finding TNC arrival times for {} ({},{})", transportationNetworkCompany.getTransportationNetworkCompanyType(), place.lat, place.lon);
                return transportationNetworkCompany.getArrivalTimes(place.lat, place.lon);
            });
        }

        List<Future<List<ArrivalTime>>> results = pool.invokeAll(tasks);

        LOG.debug("Collecting results");

        for (Future<List<ArrivalTime>> future : results) {
            arrivalTimes.addAll(future.get());
        }
        pool.shutdown();

        return arrivalTimes;
    }

    private List<TransportationNetworkCompanyDataSource> parseCompanies(String companies) {
        List<TransportationNetworkCompanyDataSource> companyDataSources = new ArrayList<>();

        // parse list of tnc companies
        for (String company : companies.split(",")) {
            companyDataSources.add(getTransportationNetworkCompanyDataSource(company));
        }

        if (companyDataSources.size() == 0) {
            LOG.warn("No Transportation Network Companies matched in companies query of `{}`", companies);
        }

        return companyDataSources;
    }

    public List<RideEstimate> getRideEstimates(
        String companies,
        Place fromPlace,
        Place toPlace
    ) throws ExecutionException, InterruptedException {
        List<RideEstimate> rideEstimates = new ArrayList<>();

        List<TransportationNetworkCompanyDataSource> companiesToRequestFrom = parseCompanies(companies);
        if (companiesToRequestFrom.size() == 0) {
            return rideEstimates;
        }

        // add a request for all matching companies
        ExecutorService pool = Executors.newFixedThreadPool(companiesToRequestFrom.size());
        List<Callable<List<RideEstimate>>> tasks = new ArrayList<>();

        for (TransportationNetworkCompanyDataSource transportationNetworkCompany : companiesToRequestFrom) {
            tasks.add(() -> {
                LOG.debug(
                    "Finding TNC ride/price estimates for {} for trip ({},{}) -> ({},{})",
                    transportationNetworkCompany.getTransportationNetworkCompanyType(),
                    fromPlace.lat,
                    fromPlace.lon,
                    toPlace.lat,
                    toPlace.lon
                );
                return transportationNetworkCompany.getRideEstimates(
                    fromPlace.lat,
                    fromPlace.lon,
                    toPlace.lat,
                    toPlace.lon
                );
            });
        }

        List<Future<List<RideEstimate>>> results = pool.invokeAll(tasks);

        LOG.debug("Collecting results");

        for (Future<List<RideEstimate>> future : results) {
            rideEstimates.addAll(future.get());
        }
        pool.shutdown();

        return rideEstimates;
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