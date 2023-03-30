package org.opentripplanner.ext.ridehailing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.ridehailing.TestRideHailingService.DEFAULT_ARRIVAL_TIMES;
import static org.opentripplanner.ext.ridehailing.model.RideHailingProvider.UBER;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.ridehailing.model.ArrivalTime;
import org.opentripplanner.ext.ridehailing.model.RideEstimate;
import org.opentripplanner.ext.ridehailing.model.RideHailingLeg;
import org.opentripplanner.ext.ridehailing.model.RideHailingProvider;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.transit.model.basic.Money;

class RideHailingFilterTest implements PlanTestConstants {

  public static final RideEstimate RIDE_ESTIMATE = new RideEstimate(
    UBER,
    Duration.ofMinutes(15),
    Money.usDollars(1500),
    Money.usDollars(3000),
    "foo",
    "UberX",
    false
  );
  RideHailingService mockService = new TestRideHailingService(
    DEFAULT_ARRIVAL_TIMES,
    List.of(RIDE_ESTIMATE)
  );
  RideHailingService failingService = new RideHailingService() {
    @Override
    public RideHailingProvider provider() {
      return UBER;
    }

    @Override
    public List<ArrivalTime> arrivalTimes(WgsCoordinate coordinate) {
      throw new RuntimeException();
    }

    @Override
    public List<RideEstimate> rideEstimates(WgsCoordinate start, WgsCoordinate end)
      throws ExecutionException {
      throw new ExecutionException(new IOException());
    }
  };

  Itinerary i = TestItineraryBuilder
    .newItinerary(A)
    .drive(T11_30, PlanTestConstants.T11_50, B)
    .build();

  @Test
  void noServices() {
    var filter = new RideHailingFilter(List.of());

    var filtered = filter.filter(List.of(i));

    assertEquals(List.of(), filtered);
  }

  @Test
  void addRideHailingInformation() {
    var filter = new RideHailingFilter(List.of(mockService));

    var filtered = filter.filter(List.of(i));

    var leg = filtered.get(0).getLegs().get(0);

    assertInstanceOf(RideHailingLeg.class, leg);

    var hailingLeg = (RideHailingLeg) leg;

    assertEquals(RIDE_ESTIMATE, hailingLeg.rideEstimate());
  }

  @Test
  void failingService() {
    var filter = new RideHailingFilter(List.of(failingService));

    var filtered = filter.filter(List.of(i));

    assertTrue(filtered.get(0).isFlaggedForDeletion());
  }
}
