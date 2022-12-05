package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.VehicleModeEnumeration;

public class StopPlaceTypeMapperTest {

  private final StopPlaceTypeMapper stopPlaceTypeMapper = new StopPlaceTypeMapper();

  @Test
  public void mapWithoutTransportMode() {
    var transitMode = stopPlaceTypeMapper.map(new StopPlace());
    assertNull(transitMode.mainMode());
    assertNull(transitMode.subMode());
  }

  @Test
  public void mapWithTransportModeOnly() {
    var transitMode = stopPlaceTypeMapper.map(
      new StopPlace().withTransportMode(VehicleModeEnumeration.RAIL)
    );
    assertEquals(TransitMode.RAIL, transitMode.mainMode());
    assertNull(transitMode.subMode());
  }

  @Test
  public void mapWithSubMode() {
    var transitMode = stopPlaceTypeMapper.map(
      new StopPlace()
        .withTransportMode(VehicleModeEnumeration.RAIL)
        .withRailSubmode(RailSubmodeEnumeration.REGIONAL_RAIL)
    );
    assertEquals(TransitMode.RAIL, transitMode.mainMode());
    assertEquals("regionalRail", transitMode.subMode());
  }

  @Test
  public void mapWithSubModeOnly() {
    var transitMode = stopPlaceTypeMapper.map(
      new StopPlace().withRailSubmode(RailSubmodeEnumeration.REGIONAL_RAIL)
    );
    assertEquals(TransitMode.RAIL, transitMode.mainMode());
    assertEquals("regionalRail", transitMode.subMode());
  }

  @Test
  public void checkSubModePrecedensOverMainMode() {
    var transitMode = stopPlaceTypeMapper.map(
      new StopPlace()
        .withTransportMode(VehicleModeEnumeration.RAIL)
        .withBusSubmode(BusSubmodeEnumeration.SIGHTSEEING_BUS)
    );
    assertEquals(TransitMode.BUS, transitMode.mainMode());
    assertEquals("sightseeingBus", transitMode.subMode());
  }
}
