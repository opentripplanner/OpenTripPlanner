package org.opentripplanner.routing.transportation_network_company;

import org.opentripplanner.api.model.ApiPlace;
import org.opentripplanner.updater.transportation_network_company.TransportationNetworkCompanyDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
        sources.put(source.getType(), source);
    }

    public List<ArrivalTime> getArrivalTimes(double lat, double lon, String companies)
            throws ExecutionException, InterruptedException
    {

        List<ArrivalTime> arrivalTimes = new ArrayList<>();

        List<TransportationNetworkCompanyDataSource> companiesToRequestFrom
            = new ArrayList<>();

        // parse list of tnc companies
        for (String company : companies.split(",")) {
            companiesToRequestFrom.add(getTransportationNetworkCompanyDataSource(company));
        }

        if (companiesToRequestFrom.size() == 0) {
            return arrivalTimes;
        }

        LOG.debug("Finding TNC arrival times for {} companies", companiesToRequestFrom.size());

        // add a request for all matching companies
        ExecutorService pool = Executors.newFixedThreadPool(10);
        List<Callable<List<ArrivalTime>>> tasks = new ArrayList<>();

        for (TransportationNetworkCompanyDataSource transportationNetworkCompany : companiesToRequestFrom) {
            tasks.add(() -> {
                LOG.debug("Finding TNC arrival times for {} ({},{})", transportationNetworkCompany.getType(), lat, lon);
                return transportationNetworkCompany.getArrivalTimes(lat, lon);
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

    public RideEstimate getRideEstimate(
        String company,
        String rideType,
        ApiPlace fromPlace,
        ApiPlace toPlace
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
