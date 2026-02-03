package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.rutebanken.netex.model.AllPublicTransportModesEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.StopPlace;

class StopPlaceTypeMapperTest {

  private final StopPlaceTypeMapper stopPlaceTypeMapper = new StopPlaceTypeMapper();

  @Test
  void mapWithoutTransportMode() {
    var transitMode = stopPlaceTypeMapper.map(new StopPlace());
    assertNull(transitMode.mainMode());
    assertNull(transitMode.subMode());
  }

  @Test
  void mapWithTransportModeOnly() {
    var transitMode = stopPlaceTypeMapper.map(
      new StopPlace().withTransportMode(AllPublicTransportModesEnumeration.RAIL)
    );
    assertEquals(TransitMode.RAIL, transitMode.mainMode());
    assertNull(transitMode.subMode());
  }

  @Test
  void mapCableway() {
    var transitMode = stopPlaceTypeMapper.map(
      new StopPlace().withTransportMode(AllPublicTransportModesEnumeration.CABLEWAY)
    );
    assertEquals(TransitMode.GONDOLA, transitMode.mainMode());
    assertNull(transitMode.subMode());
  }

  @Test
  void mapWithSubMode() {
    var transitMode = stopPlaceTypeMapper.map(
      new StopPlace()
        .withTransportMode(AllPublicTransportModesEnumeration.RAIL)
        .withRailSubmode(RailSubmodeEnumeration.REGIONAL_RAIL)
    );
    assertEquals(TransitMode.RAIL, transitMode.mainMode());
    assertEquals("regionalRail", transitMode.subMode());
  }

  @Test
  void mapWithSubModeOnly() {
    var transitMode = stopPlaceTypeMapper.map(
      new StopPlace().withRailSubmode(RailSubmodeEnumeration.REGIONAL_RAIL)
    );
    assertEquals(TransitMode.RAIL, transitMode.mainMode());
    assertEquals("regionalRail", transitMode.subMode());
  }

  @Test
  void checkSubModePrecedenceOverMainMode() {
    var transitMode = stopPlaceTypeMapper.map(
      new StopPlace()
        .withTransportMode(AllPublicTransportModesEnumeration.RAIL)
        .withBusSubmode(BusSubmodeEnumeration.SIGHTSEEING_BUS)
    );
    assertEquals(TransitMode.BUS, transitMode.mainMode());
    assertEquals("sightseeingBus", transitMode.subMode());
  }
}
