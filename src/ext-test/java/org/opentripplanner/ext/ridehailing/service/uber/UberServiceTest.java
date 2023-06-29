package org.opentripplanner.ext.ridehailing.service.uber;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.ridehailing.service.oauth.OAuthService;
import org.opentripplanner.framework.geometry.WgsCoordinate;

class UberServiceTest {

  final String UBER_POOL_ID = "26546650-e557-4a7b-86e7-6a3942445247";
  final String UBER_XL_ID = "821415d8-3bd5-4e27-9604-194e4359a449";
  final String PRICES_URI = "file:src/ext-test/resources/ridehailing/uber-price-estimates.json";
  final String ARRIVAL_TIMES_URI =
    "file:src/ext-test/resources/ridehailing/uber-arrival-estimates.json";
  OAuthService authService = () -> "a token";

  UberService service = new UberService(
    authService,
    PRICES_URI,
    ARRIVAL_TIMES_URI,
    List.of(UBER_POOL_ID),
    UBER_XL_ID
  );

  @Test
  void arrivalTimes() throws ExecutionException {
    var estimates = service.arrivalTimes(WgsCoordinate.GREENWICH, false);
    assertEquals(7, estimates.size());

    var first = estimates.get(0);
    assertEquals("uberX", first.displayName());
  }

  @Test
  void wheelchairArrivalTimes() throws ExecutionException {
    var estimates = service.arrivalTimes(WgsCoordinate.GREENWICH, true);
    assertEquals(1, estimates.size());

    var first = estimates.get(0);
    assertEquals("uberXL", first.displayName());
  }

  @Test
  void rideEstimates() throws ExecutionException {
    var estimates = service.rideEstimates(WgsCoordinate.GREENWICH, WgsCoordinate.GREENWICH, false);
    assertEquals(7, estimates.size());

    var firstEstimate = estimates.get(0);

    assertEquals("uberX", firstEstimate.productName());
  }

  @Test
  void wheelchairRideEstimates() throws ExecutionException {
    var estimates = service.rideEstimates(WgsCoordinate.GREENWICH, WgsCoordinate.GREENWICH, true);
    assertEquals(1, estimates.size());

    var firstEstimate = estimates.get(0);

    assertEquals("uberXL", firstEstimate.productName());
  }
}
