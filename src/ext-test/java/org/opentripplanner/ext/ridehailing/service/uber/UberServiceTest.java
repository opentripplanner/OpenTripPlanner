package org.opentripplanner.ext.ridehailing.service.uber;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.ridehailing.service.oauth.OAuthService;
import org.opentripplanner.framework.geometry.WgsCoordinate;

class UberServiceTest {

  final String PRICES_URI = "file:src/ext-test/resources/ridehailing/uber-price-estimates.json";
  final String ARRIVAL_TIMES_URI =
    "file:src/ext-test/resources/ridehailing/uber-arrival-estimates.json";
  OAuthService authService = () -> "a token";

  UberService service = new UberService(authService, PRICES_URI, ARRIVAL_TIMES_URI);

  @Test
  void arrivalTimes() throws ExecutionException {
    var estimates = service.arrivalTimes(WgsCoordinate.GREENWICH);
    assertEquals(8, estimates.size());
  }

  @Test
  void rideEstimates() throws ExecutionException {
    var estimates = service.rideEstimates(WgsCoordinate.GREENWICH, WgsCoordinate.GREENWICH);
    assertEquals(8, estimates.size());
  }
}
