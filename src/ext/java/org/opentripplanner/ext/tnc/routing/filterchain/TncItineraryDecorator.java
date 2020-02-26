package org.opentripplanner.ext.tnc.routing.filterchain;

import org.opentripplanner.ext.tnc.api.model.TransportationNetworkCompanySummary;
import org.opentripplanner.ext.tnc.routing.TransportationNetworkCompanyService;
import org.opentripplanner.ext.tnc.routing.error.TransportationNetworkCompanyAvailabilityException;
import org.opentripplanner.ext.tnc.routing.model.ArrivalTime;
import org.opentripplanner.ext.tnc.routing.model.RideEstimate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.routing.core.RoutingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TncItineraryDecorator implements ItineraryFilter {
    private static final Logger LOG = LoggerFactory.getLogger(TncItineraryDecorator.class);
    private List<Itinerary> delete = new ArrayList<>();

    private final TransportationNetworkCompanyService service;
    private final RoutingRequest request;


    public TncItineraryDecorator(
            TransportationNetworkCompanyService service, RoutingRequest request
    ) {
        this.service = service;
        this.request = request;
    }

    @Override
    public String name() {
        return "TNC-itinerary-decorator";
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        itineraries.forEach(this::addTncDataToItinerary);
        return removeTncItinerariesWhitchCouldNotBeDecorated(itineraries);
    }

    /**
     * TODO TNC - Move this to the appropriate place
     *
     * Adds TNC data to legs with {@link Leg#hailedCar}=true. This makes asynchronous, concurrent
     * requests to the TNC provider's API for price and ETA estimates and associates this data with
     * its respective TNC leg.
     *
     * @return boolean. If false, this means that the availability of TNC service cannot be confirmed.
     */
    private void addTncDataToItinerary(Itinerary itinerary) {
        // Don't do anything further if the request doesn't involve TNC routing
        if (!request.useTransportationNetworkCompany) {
            return;
        }

        // Graph graph = path.getRoutingContext().graph;
        String companies = request.companies;
        if (companies == null) {
            // no companies, therefore this request doesn't have any TNC data to add. Return true
            // to indicate no need for removal of this itinerary.
            return;
        }
        // Store async tasks in lists for any TNC legs that need info.
        List<Callable<List<ArrivalTime>>> arrivalEstimateTasks = new ArrayList<>();
        List<Callable<List<RideEstimate>>> priceEstimateTasks = new ArrayList<>();
        // Keep track of TNC legs here (so the TNC responses can be filled in later).
        List<Leg> tncLegs = new ArrayList<>();
        List<Boolean> tncLegsAreFromOrigin = new ArrayList<>();

        // Accumulate TNC request tasks for each TNC leg.
        for (int i = 0; i < itinerary.legs.size(); i++) {
            Leg leg = itinerary.legs.get(i);
            if (!leg.hailedCar) continue;
            tncLegs.add(leg);
            // If handling the first or second leg, do not attempt to get an arrival estimate for
            // the leg from location and instead use the trip's start location.  Do this is because:
            // 1.  If it is the first leg, this means the trip began with a user taking a TNC
            // 2.  If it is the second leg and the first leg was walking, the itinerary includes
            // walking a little bit to the TNC pickup location, but the graph search still used the
            // ETA for the request's from location.
            //
            // This avoids unnecessary/redundant API requests to TNC providers.
            Place from = leg.from;
            if (request.transportationNetworkCompanyEtaAtOrigin > -1 &&
                    (i == 0 || (i == 1 && itinerary.legs.get(0).mode.equals("WALK")))) {
                from = new Place(request.from.lng, request.from.lat, request.from.label);
                tncLegsAreFromOrigin.add(true);
            } else {
                tncLegsAreFromOrigin.add(false);
            }
            Place finalFrom = from;
            priceEstimateTasks.add(() -> service.getRideEstimates(companies, finalFrom, leg.to));
            arrivalEstimateTasks.add(() -> service.getArrivalTimes(companies, finalFrom));
        }

        // TODO Move some where
        // This variable is used to keep track of whether an API error was encountered. If an API
        // error happens, this calls into question whether the TNC trip is possible at all. This
        // typically happens when a TNC company says that it does not provide service at a requested
        // GPS coordinate. Since the TNC companies don't have readily available APIs that describe
        // where they provide service, it isn't possible to know whether service exists until
        // querying at a certain GPS coordinate. This follows an underlying assumption of the TNC
        // routing which is that OTP assumes that TNC service is available anywhere within walking
        // distance of transit.

        if (tncLegs.size() > 0) {
            // Use a thread pool so that requests are asynchronous and concurrent. # of threads
            // should accommodate 2x however many TNC legs there are.
            ExecutorService pool = Executors.newFixedThreadPool(tncLegs.size() * 2);

            try {
                // Execute TNC requests.
                List<Future<List<ArrivalTime>>> etaResults = pool.invokeAll(arrivalEstimateTasks);
                List<Future<List<RideEstimate>>> priceResults = pool.invokeAll(priceEstimateTasks);
                int resultCount = priceResults.size() + etaResults.size();
                LOG.info("Collating {} TNC results for {} legs for {}", resultCount, tncLegs.size(), itinerary);
                // Collate results into itinerary legs.
                for (int i = 0; i < tncLegs.size(); i++) {
                    // Choose the TNC result with the fastest ride time or ride time and ETA time if it is the first leg
                    int bestTime = Integer.MAX_VALUE;
                    ArrivalTime bestArrivalTime = null;
                    RideEstimate bestRideEstimate = null;

                    List<ArrivalTime> arrivalTimes = etaResults.get(i).get();
                    List<RideEstimate> rideEstimates = priceResults.get(i).get();
                    boolean tncLegIsFromOrigin = tncLegsAreFromOrigin.get(i);

                    for (ArrivalTime arrivalTime : arrivalTimes) {
                        for (RideEstimate rideEstimate : rideEstimates) {
                            // check if the arrival and ride estimate match and also if the
                            // arrival and ride estimate match the wheelchair accessibility option
                            // set in the routing request
                            if (
                                    arrivalTime.company.equals(rideEstimate.company) &&
                                            arrivalTime.productId.equals(rideEstimate.rideType) &&
                                            arrivalTime.wheelchairAccessible == request.wheelchairAccessible &&
                                            rideEstimate.wheelchairAccessible == request.wheelchairAccessible
                            ) {
                                int combinedTime = rideEstimate.duration +
                                        (tncLegIsFromOrigin ? arrivalTime.estimatedSeconds : 0);
                                if (combinedTime < bestTime) {
                                    bestTime = combinedTime;
                                    bestArrivalTime = arrivalTime;
                                    bestRideEstimate = rideEstimate;
                                }
                            }
                        }
                    }
                    if (bestArrivalTime == null || bestRideEstimate == null) {
                        // this occurs when TNC service is actually not available at a certain
                        // location which results in empty responses for arrival and ride estimates.
                        // The error thrown here is caught within this method below.
                        throw new TransportationNetworkCompanyAvailabilityException();
                    }
                    tncLegs.get(i).tncData = new TransportationNetworkCompanySummary(
                            bestRideEstimate,
                            bestArrivalTime
                    );
                }
            } catch (TransportationNetworkCompanyAvailabilityException e) {
                LOG.warn("Removing itinerary due to TNC unavailability");
                delete.add(itinerary);
            } catch (Exception e) {
                LOG.error("Error fetching TNC data", e);
                delete.add(itinerary);
            }
            finally {
                // Shutdown thread pool.
                pool.shutdown();
            }
        }
    }

    private ArrayList<Itinerary> removeTncItinerariesWhitchCouldNotBeDecorated(List<Itinerary> itineraries) {
        ArrayList<Itinerary> newList = new ArrayList<>(itineraries);
        newList.removeAll(delete);
        return newList;
    }
}
