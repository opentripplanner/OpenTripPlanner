package org.opentripplanner.routing.transportation_network_company;

import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.error.TransportationNetworkCompanyAvailabilityException;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.updater.transportation_network_company.TransportationNetworkCompanyDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
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

    /**
     * A helper method for calculating the earliest possible ETA (estimated time of arrival) of a TNC vehicle and making
     * sure TNC service exists at origin for routing requests with TNC travel. This is not a future-proof solution as
     * TNC coverage areas could be different in the future.  For example, a trip planned months in advance may not take
     * into account a TNC company deciding to no longer provide service on that particular date in the future. The
     * current ETA estimate is only valid for perhaps 30 minutes into the future.
     *
     * Also, if "depart at" and leaving soonish, save earliest departure time for use later use when boarding the first
     * TNC before transit.  (See {@link StateEditor#boardHailedCar}).
     *
     * @param request The routing request to check and possibly update
     * @param router The router this request is associated with
     * @param companies The companies request parameter
     */
    public static void setEarliestTransportationNetworkCompanyEta(
        RoutingRequest request,
        Router router,
        String companies
    ) throws ParameterException {
        if (request.useTransportationNetworkCompany) {
            if (companies == null) {
                companies = "NOAPI";
            }

            request.companies = companies;

            TransportationNetworkCompanyService service =
                router.graph.getService(TransportationNetworkCompanyService.class);
            if (service == null) {
                LOG.error("Unconfigured Transportation Network Company service for router with id: " + router.id);
                throw new ParameterException(Message.TRANSPORTATION_NETWORK_COMPANY_CONFIG_INVALID);
            }

            List<ArrivalTime> arrivalEstimates;

            try {
                arrivalEstimates = service.getArrivalTimes(
                    companies,
                    new Place(
                        request.from.lng,
                        request.from.lat,
                        request.from.name
                    )
                );
            } catch (Exception e) {
                LOG.error("Unable to query TNC API for arrival estimates!");
                e.printStackTrace();
                throw new UnsupportedOperationException(
                    "Unable to verify availability of Transportation Network Company service due to error: " +
                        e.getMessage()
                );
            }

            /**
             * iterate through results and find earliest ETA of an acceptable ride type
             * this also checks if any of the ride types are wheelchair accessible or not
             * if the request requires a wheelchair accessible ride and no arrival estimates are
             * found, then the TransportationNetworkCompanyAvailabilityException will be thrown.
             */
            int earliestEta = Integer.MAX_VALUE;
            for (ArrivalTime arrivalEstimate : arrivalEstimates) {
                if (
                    arrivalEstimate.estimatedSeconds < earliestEta &&
                        request.wheelchairAccessible == arrivalEstimate.wheelchairAccessible
                ) {
                    earliestEta = arrivalEstimate.estimatedSeconds;
                }
            }

            if (earliestEta == Integer.MAX_VALUE) {
                // no acceptable ride types found
                throw new TransportationNetworkCompanyAvailabilityException();
            }

            // store the earliest ETA if planning a "depart at" trip that begins soonish (within + or - 30 minutes)
            long now = (new Date()).getTime() / 1000;
            long departureTimeWindow = 1800;
            if (
                request.arriveBy == false &&
                    request.dateTime < now + departureTimeWindow &&
                    request.dateTime > now - departureTimeWindow
            ) {
                request.transportationNetworkCompanyEtaAtOrigin = earliestEta;
            }
        }
    }
}
