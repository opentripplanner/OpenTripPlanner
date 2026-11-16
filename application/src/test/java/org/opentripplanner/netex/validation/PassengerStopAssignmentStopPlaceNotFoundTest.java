package org.opentripplanner.netex.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.netex.index.api.HMapValidationRule.Status.DISCARD;
import static org.opentripplanner.netex.index.api.HMapValidationRule.Status.OK;

import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.NetexTestDataSupport;
import org.opentripplanner.netex.index.NetexEntityIndex;

class PassengerStopAssignmentStopPlaceNotFoundTest {

  private static final String STOP_PLACE_ID = NetexTestDataSupport.STOP_PLACE_ID;

  @Test
  void stopPlaceExists() {
    var index = new NetexEntityIndex();
    index.stopPlaceById.add(NetexTestDataSupport.createStopPlace());

    assertEquals(OK, createRule(index).validate(STOP_PLACE_ID));
  }

  @Test
  void stopPlaceNotFound() {
    var index = new NetexEntityIndex();

    assertEquals(DISCARD, createRule(index).validate(STOP_PLACE_ID));
  }

  private static PassengerStopAssignmentStopPlaceNotFound createRule(NetexEntityIndex index) {
    var rule = new PassengerStopAssignmentStopPlaceNotFound();
    rule.setup(index.readOnlyView());
    return rule;
  }
}
