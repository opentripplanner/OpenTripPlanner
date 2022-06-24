package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.mapping.TransportModeMapper.UnsupportedModeException;
import org.opentripplanner.transit.model.network.TransitMode;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.rutebanken.netex.model.WaterSubmodeEnumeration;

public class TransportModeMapperTest {

  public static final TransportSubmodeStructure VALID_SUBMODE_STRCUTURE = new TransportSubmodeStructure()
    .withRailSubmode(RailSubmodeEnumeration.LONG_DISTANCE);
  private final TransportModeMapper transportModeMapper = new TransportModeMapper();

  @Test
  public void mapWithTransportModeOnly() throws UnsupportedModeException {
    var transitMode = transportModeMapper.map(AllVehicleModesOfTransportEnumeration.BUS, null);
    assertEquals(TransitMode.BUS, transitMode.mainMode());
    assertEquals(null, transitMode.subMode());
  }

  @Test
  public void mapWithSubMode() throws UnsupportedModeException {
    var transitMode = transportModeMapper.map(
      AllVehicleModesOfTransportEnumeration.RAIL,
      VALID_SUBMODE_STRCUTURE
    );
    assertEquals(TransitMode.RAIL, transitMode.mainMode());
    assertEquals("longDistance", transitMode.subMode());
  }

  @Test
  public void checkSubModePrecedensOverMainMode() throws UnsupportedModeException {
    var transitMode = transportModeMapper.map(
      AllVehicleModesOfTransportEnumeration.BUS,
      new TransportSubmodeStructure()
        .withWaterSubmode(WaterSubmodeEnumeration.INTERNATIONAL_PASSENGER_FERRY)
    );
    assertEquals(TransitMode.FERRY, transitMode.mainMode());
    assertEquals("internationalPassengerFerry", transitMode.subMode());
  }

  @Test
  public void unsupportedMode() {
    Assertions.assertThrows(
      UnsupportedModeException.class,
      () -> transportModeMapper.map(AllVehicleModesOfTransportEnumeration.UNKNOWN, null)
    );
    Assertions.assertThrows(
      UnsupportedModeException.class,
      () -> transportModeMapper.map(null, null)
    );
  }
}
