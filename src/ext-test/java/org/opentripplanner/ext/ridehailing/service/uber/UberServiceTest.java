package org.opentripplanner.ext.ridehailing.service.uber;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.ridehailing.service.oauth.OAuthService;
import org.opentripplanner.framework.geometry.WgsCoordinate;

class UberServiceTest {

  final String UBER_POOL_ID = "26546650-e557-4a7b-86e7-6a3942445247";
  final String PRICES_URI = "file:src/ext-test/resources/ridehailing/uber-price-estimates.json";
  final String ARRIVAL_TIMES_URI =
    "file:src/ext-test/resources/ridehailing/uber-arrival-estimates.json";
  OAuthService authService = () -> "a token";

  UberService service = new UberService(
    authService,
    PRICES_URI,
    ARRIVAL_TIMES_URI,
    List.of(UBER_POOL_ID)
  );

  @Test
  void arrivalTimes() throws ExecutionException {
    var estimates = service.arrivalTimes(WgsCoordinate.GREENWICH);
    assertEquals(7, estimates.size());

    var first = estimates.get(0);
    assertEquals("uberX", first.displayName());
  }

  @Test
  void rideEstimates() throws ExecutionException {
    var estimates = service.rideEstimates(WgsCoordinate.GREENWICH, WgsCoordinate.GREENWICH);
    assertEquals(7, estimates.size());

    var firstEstimate = estimates.get(0);

    assertEquals("uberX", firstEstimate.productName());
  }
}
