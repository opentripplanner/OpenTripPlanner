package org.opentripplanner.ext.fares;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.fare.ItineraryFare;
import org.opentripplanner.model.plan.PlanTestConstants;

class ItineraryFareDataLoaderTest implements PlanTestConstants {

  @Test
  void batchesMultipleLegsFromTheSameItineraryIntoOneCall()
    throws ExecutionException, InterruptedException {
    var callCount = new AtomicInteger(0);
    var loader = ItineraryFareDataLoader.create(itinerary -> {
      callCount.incrementAndGet();
      return ItineraryFare.empty();
    });

    var itinerary = newItinerary(A, 0).bus(1, 0, 50, B).bus(2, 52, 100, C).build();

    // Both legs request fares for the same itinerary
    var future1 = loader.load(itinerary);
    var future2 = loader.load(itinerary);
    loader.dispatch();

    future1.get();
    future2.get();

    assertEquals(1, callCount.get(), "calculateFares must be called once for both legs");
  }

  @Test
  void callsCalculateFaresForEachDistinctItinerary()
    throws ExecutionException, InterruptedException {
    var callCount = new AtomicInteger(0);
    var loader = ItineraryFareDataLoader.create(itinerary -> {
      callCount.incrementAndGet();
      return ItineraryFare.empty();
    });

    var i1 = newItinerary(A, 0).bus(1, 0, 50, B).build();
    var i2 = newItinerary(A, 0).bus(2, 0, 50, B).build();

    var future1 = loader.load(i1);
    var future2 = loader.load(i2);
    loader.dispatch();

    future1.get();
    future2.get();

    assertEquals(2, callCount.get(), "calculateFares must be called once per distinct itinerary");
  }
}
