package org.opentripplanner.ext.tnc.api.routing;

import org.opentripplanner.api.model.ApiItinerary;
import org.opentripplanner.api.model.ApiLeg;
import org.opentripplanner.api.model.TransportationNetworkCompanySummary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.transportation_network_company.ArrivalTime;
import org.opentripplanner.routing.transportation_network_company.RideEstimate;
import org.opentripplanner.routing.transportation_network_company.TransportationNetworkCompanyService;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.opentripplanner.api.resource.TransportationNetworkCompanyResource.ACCEPTED_RIDE_TYPES;

public class TncItinerarySummaryDataMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TncItinerarySummaryDataMapper.class);

    /**
     * Adds TNC data to legs with {@link Leg#hailedCar}=true. This makes asynchronous, concurrent requests to the TNC
     * provider's API for price and ETA estimates and associates this data with its respective TNC leg.
     */
    public static void addTncDataToItinerary(Graph graph, ApiItinerary itinerary, String companies) {
        if (OTPFeature.TncRouting.isOff() || companies == null) {
            return;
        }

        String rideType = companies.equals("UBER") ? ACCEPTED_RIDE_TYPES[1] : ACCEPTED_RIDE_TYPES[0];
        // Store async tasks in lists for any TNC legs that need info.
        List<Callable<List<ArrivalTime>>> arrivalEstimateTasks = new ArrayList<>();
        List<Callable<RideEstimate>> priceEstimateTasks = new ArrayList<>();
        // Keep track of TNC legs here (so the TNC responses can be filled in later).
        List<ApiLeg> tncLegs = new ArrayList<>();
        TransportationNetworkCompanyService service = graph.getService(TransportationNetworkCompanyService.class);
        // Accumulate TNC request tasks for each TNC leg.
        for (ApiLeg leg : itinerary.legs) {
            if (!leg.hailedCar) continue;
            tncLegs.add(leg);
            // FIXME: Use an enum to handle this check.
            priceEstimateTasks.add(() -> service.getRideEstimate(companies, rideType, leg.from, leg.to));
            arrivalEstimateTasks.add(() -> service.getArrivalTimes(leg.from.lat, leg.from.lon, companies));
        }
        if (tncLegs.size() > 0) {
            // Use a thread pool so that requests are asynchronous and concurrent. # of threads should accommodate
            // 2x however many TNC legs there are.
            ExecutorService pool = Executors.newFixedThreadPool(tncLegs.size() * 2);
            try {
                // Execute TNC requests.
                List<Future<List<ArrivalTime>>> etaResults = pool.invokeAll(arrivalEstimateTasks);
                List<Future<RideEstimate>> priceResults = pool.invokeAll(priceEstimateTasks);
                int resultCount = priceResults.size() + etaResults.size();

                LOG.info("Collating {} TNC results for {} legs for {}", resultCount, tncLegs.size(), itinerary);
                // Collate results into itinerary legs. ETA and price result lists will have the same length.
                for (int i = 0; i < etaResults.size(); i++) {
                    ArrivalTime eta = null;
                    List<ArrivalTime> arrivalTimes = etaResults.get(i).get();
                    for (ArrivalTime arrivalTime : arrivalTimes) {
                        if (rideType.equals(arrivalTime.productId)) {
                            eta = arrivalTime;
                            break;
                        }
                    }
                    tncLegs.get(i).tncData = new TransportationNetworkCompanySummary(priceResults.get(i).get(), eta);
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Error fetching TNC data");
                e.printStackTrace();
            }
            // Shutdown thread pool.
            pool.shutdown();
        }
    }
}
