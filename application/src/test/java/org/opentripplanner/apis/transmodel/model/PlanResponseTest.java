package org.opentripplanner.apis.transmodel.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;

class PlanResponseTest {

  private static final RoutingError ERROR = new RoutingError(
    RoutingErrorCode.LOCATION_NOT_FOUND,
    InputField.FROM_PLACE
  );

  @Test
  void responseForInvalidRequest() {
    PlanResponse response = PlanResponse.ofErrors(List.of(ERROR));
    assertNotNull(response);
    assertEquals(List.of(ERROR), response.messages());
    assertNotNull(response.date());
    assertEquals(PlanResponse.UNKNOWN_ORIGIN, response.from());
    assertEquals(PlanResponse.UNKNOWN_DESTINATION, response.to());
  }
}
