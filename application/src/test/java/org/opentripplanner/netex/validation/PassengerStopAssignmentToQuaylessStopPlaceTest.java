package org.opentripplanner.netex.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.netex.index.api.HMapValidationRule.Status.DISCARD;
import static org.opentripplanner.netex.index.api.HMapValidationRule.Status.OK;

import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.NetexTestDataSupport;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.opentripplanner.netex.index.api.HMapValidationRule.Status;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.QuayRefStructure;
import org.rutebanken.netex.model.StopPlace;

class PassengerStopAssignmentToQuaylessStopPlaceTest {

  private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
  private static final String STOP_PLACE_ID = NetexTestDataSupport.STOP_PLACE_ID;

  @Test
  void stopPlaceWithQuay() {
    var stopPlace = NetexTestDataSupport.createStopPlace(NetexTestDataSupport.createQuay());

    assertEquals(OK, validate(stopPlace));
  }

  @Test
  void stopPlaceWithoutQuays() {
    var stopPlace = NetexTestDataSupport.createStopPlace();

    assertEquals(DISCARD, validate(stopPlace));
  }

  @Test
  void stopPlaceWithEmptyQuays() {
    var stopPlace = NetexTestDataSupport.createStopPlace().withQuays(
      OBJECT_FACTORY.createQuays_RelStructure()
    );

    assertEquals(DISCARD, validate(stopPlace));
  }

  /**
   * Quay references are not supported by the stop mapping, so such a stop place ends up as a
   * station without any child stops.
   */
  @Test
  void stopPlaceWithOnlyQuayRefs() {
    var quayRef = new QuayRefStructure().withRef(NetexTestDataSupport.QUAY_ID);
    var stopPlace = NetexTestDataSupport.createStopPlace().withQuays(
      OBJECT_FACTORY.createQuays_RelStructure().withQuayRefOrQuay(
        OBJECT_FACTORY.createQuayRef(quayRef)
      )
    );

    assertEquals(DISCARD, validate(stopPlace));
  }

  /**
   * Quays without coordinates are dropped by the stop mapping, so such a stop place ends up as a
   * station without any child stops.
   */
  @Test
  void stopPlaceWithOnlyQuayWithoutCoordinates() {
    var quay = NetexTestDataSupport.createQuay().withCentroid(null);
    var stopPlace = NetexTestDataSupport.createStopPlace(quay);

    assertEquals(DISCARD, validate(stopPlace));
  }

  @Test
  void stopPlaceNotFound() {
    var rule = new PassengerStopAssignmentToQuaylessStopPlace();
    rule.setup(new NetexEntityIndex().readOnlyView());

    assertEquals(DISCARD, rule.validate(STOP_PLACE_ID));
  }

  private static Status validate(StopPlace stopPlace) {
    var index = new NetexEntityIndex();
    index.stopPlaceById.add(stopPlace);
    var rule = new PassengerStopAssignmentToQuaylessStopPlace();
    rule.setup(index.readOnlyView());
    return rule.validate(STOP_PLACE_ID);
  }
}
