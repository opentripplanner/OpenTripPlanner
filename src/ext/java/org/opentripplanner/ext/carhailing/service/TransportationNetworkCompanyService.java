package org.opentripplanner.ext.carhailing.service;

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
import org.opentripplanner.ext.carhailing.CarHailingService;
import org.opentripplanner.ext.carhailing.model.ArrivalTime;
import org.opentripplanner.ext.carhailing.model.CarHailingProvider;
import org.opentripplanner.ext.carhailing.model.RideEstimate;
import org.opentripplanner.model.plan.Place;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransportationNetworkCompanyService implements Serializable {

  private static Logger LOG = LoggerFactory.getLogger(TransportationNetworkCompanyService.class);

  private Map<CarHailingProvider, CarHailingService> sources = new HashMap<>();

  public void addSource(CarHailingService source) {
    sources.put(source.carHailingCompany(), source);
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
  public List<ArrivalTime> getArrivalTimes(String companies, Place place)
    throws ExecutionException, InterruptedException {
    List<ArrivalTime> arrivalTimes = new ArrayList<>();

    List<CarHailingService> companiesToRequestFrom = parseCompanies(companies);
    if (companiesToRequestFrom.size() == 0) {
      return arrivalTimes;
    }

    LOG.debug("Finding TNC arrival times for {} companies", companiesToRequestFrom.size());

    // add a request for all matching companies
    ExecutorService pool = Executors.newFixedThreadPool(companiesToRequestFrom.size());
    List<Callable<List<ArrivalTime>>> tasks = new ArrayList<>();

    for (CarHailingService transportationNetworkCompany : companiesToRequestFrom) {
      tasks.add(() -> {
        LOG.debug(
          "Finding TNC arrival times for {} ({})",
          transportationNetworkCompany.carHailingCompany(),
          place.coordinate
        );
        return transportationNetworkCompany.arrivalTimes(place.coordinate);
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

  private List<CarHailingService> parseCompanies(String companies) {
    List<CarHailingService> companyDataSources = new ArrayList<>();

    // parse list of tnc companies
    for (String company : companies.split(",")) {
      companyDataSources.add(getTransportationNetworkCompanyDataSource(company));
    }

    if (companyDataSources.size() == 0) {
      LOG.warn("No Transportation Network Companies matched in companies query of `{}`", companies);
    }

    return companyDataSources;
  }

  public List<RideEstimate> getRideEstimates(String companies, Place fromPlace, Place toPlace)
    throws ExecutionException, InterruptedException {
    List<RideEstimate> rideEstimates = new ArrayList<>();

    List<CarHailingService> companiesToRequestFrom = parseCompanies(companies);
    if (companiesToRequestFrom.size() == 0) {
      return rideEstimates;
    }

    // add a request for all matching companies
    ExecutorService pool = Executors.newFixedThreadPool(companiesToRequestFrom.size());
    List<Callable<List<RideEstimate>>> tasks = new ArrayList<>();

    for (CarHailingService transportationNetworkCompany : companiesToRequestFrom) {
      tasks.add(() -> {
        LOG.debug(
          "Finding TNC ride/price estimates for {} for trip ({}) -> ({})",
          transportationNetworkCompany.carHailingCompany(),
          fromPlace.coordinate,
          toPlace.coordinate
        );
        return transportationNetworkCompany.getRideEstimates(
          fromPlace.coordinate,
          toPlace.coordinate
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

  private CarHailingService getTransportationNetworkCompanyDataSource(String company) {
    CarHailingProvider co = CarHailingProvider.valueOf(company);
    if (co == null) {
      throw new UnsupportedOperationException(
        "Transportation Network Company value " + company + " is not a valid type"
      );
    }

    if (!sources.containsKey(co)) {
      throw new UnsupportedOperationException(
        "Transportation Network Company value " + company + " is not configured in this router"
      );
    }

    return sources.get(co);
  }
}
