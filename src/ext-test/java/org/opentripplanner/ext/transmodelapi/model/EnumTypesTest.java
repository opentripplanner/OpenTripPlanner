package org.opentripplanner.ext.transmodelapi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.ROUTING_ERROR_CODE;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.response.RoutingErrorCode;

class EnumTypesTest {

  @Test
  void assertAllRoutingErrorCodesAreMapped() {
    var expected = EnumSet.allOf(RoutingErrorCode.class);
    var values = EnumSet.copyOf(
      ROUTING_ERROR_CODE.getValues().stream().map(it -> (RoutingErrorCode) it.getValue()).toList()
    );
    assertEquals(expected, values);
  }
}
