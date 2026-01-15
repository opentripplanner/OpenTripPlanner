package org.opentripplanner.street.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.street.internal.DefaultStreetRepository;
import org.opentripplanner.street.model.StreetConstants;
import org.opentripplanner.street.model.StreetModelDetails;

public class DefaultStreetLimitationParametersServiceTest {

  @Test
  public void maxCarSpeed() {
    var repository = new DefaultStreetRepository();

    var service = new DefaultStreetLimitationParametersService(repository);

    assertEquals(StreetConstants.DEFAULT_MAX_CAR_SPEED, service.maxCarSpeed());
    assertEquals(StreetConstants.DEFAULT_MAX_CAR_SPEED, service.maxCarSpeed());

    // Update repository and see reflected changes in service. If we do not want this
    // (current strategy) we could cashe the StreetLimitationParameters in the service.
    var maxSpeed = 25f;
    int maxAreaNodes = 100;
    repository.setStreetModelDetails(new StreetModelDetails(maxSpeed, maxAreaNodes));

    assertEquals(maxSpeed, service.maxCarSpeed());
    assertEquals(maxAreaNodes, service.maxAreaNodes());
  }
}
