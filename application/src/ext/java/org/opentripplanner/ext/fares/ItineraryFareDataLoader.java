package org.opentripplanner.ext.fares;

import java.util.concurrent.CompletableFuture;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.opentripplanner.model.fare.ItineraryFare;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.fares.FareService;

/**
 * DataLoader that computes fares once per itinerary, batching all leg fare requests for the same
 * itinerary into a single {@link FareService#calculateFares} call.
 */
public class ItineraryFareDataLoader {

  public static final String KEY = "itinerary-fare";

  public static DataLoader<Itinerary, ItineraryFare> create(FareService fareService) {
    BatchLoader<Itinerary, ItineraryFare> batchLoader = itineraries ->
      CompletableFuture.supplyAsync(() ->
        itineraries.stream().map(fareService::calculateFares).toList()
      );
    return DataLoaderFactory.newDataLoader(batchLoader);
  }
}
