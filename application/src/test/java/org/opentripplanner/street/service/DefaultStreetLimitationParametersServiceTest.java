package org.opentripplanner.street.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.StreetConstants;
import org.opentripplanner.street.model.StreetLimitationParameters;

public class DefaultStreetLimitationParametersServiceTest {

  @Test
  public void getMaxCarSpeed() {
    var maxSpeed = 25f;
    var model = new StreetLimitationParameters();
    model.initMaxCarSpeed(maxSpeed);
    var service = new DefaultStreetLimitationParametersService(model);
    assertEquals(maxSpeed, service.getMaxCarSpeed());
  }

  @Test
  public void getDefaultMaxCarSpeed() {
    var model = new StreetLimitationParameters();
    var service = new DefaultStreetLimitationParametersService(model);
    assertEquals(StreetConstants.DEFAULT_MAX_CAR_SPEED, service.getMaxCarSpeed());
  }
}
