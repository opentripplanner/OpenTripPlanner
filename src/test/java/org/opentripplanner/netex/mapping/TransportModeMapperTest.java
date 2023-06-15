package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.mapping.TransportModeMapper.UnsupportedModeException;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.rutebanken.netex.model.WaterSubmodeEnumeration;

class TransportModeMapperTest {

  private static final TransportSubmodeStructure VALID_SUBMODE_STRUCTURE = new TransportSubmodeStructure()
    .withRailSubmode(RailSubmodeEnumeration.LONG_DISTANCE);
  private static final EnumSet<AllVehicleModesOfTransportEnumeration> SUPPORTED_MODES = EnumSet.of(
    AllVehicleModesOfTransportEnumeration.AIR,
    AllVehicleModesOfTransportEnumeration.BUS,
    AllVehicleModesOfTransportEnumeration.CABLEWAY,
    AllVehicleModesOfTransportEnumeration.COACH,
    AllVehicleModesOfTransportEnumeration.FUNICULAR,
    AllVehicleModesOfTransportEnumeration.METRO,
    AllVehicleModesOfTransportEnumeration.RAIL,
    AllVehicleModesOfTransportEnumeration.TAXI,
    AllVehicleModesOfTransportEnumeration.TRAM,
    AllVehicleModesOfTransportEnumeration.WATER
  );
  private final TransportModeMapper transportModeMapper = new TransportModeMapper();

  @Test
  void mapWithTransportModeOnly() throws UnsupportedModeException {
    var transitMode = transportModeMapper.map(AllVehicleModesOfTransportEnumeration.BUS, null);
    assertEquals(TransitMode.BUS, transitMode.mainMode());
    assertNull(transitMode.subMode());
  }

  @Test
  void mapWithSubMode() throws UnsupportedModeException {
    var transitMode = transportModeMapper.map(
      AllVehicleModesOfTransportEnumeration.RAIL,
      VALID_SUBMODE_STRUCTURE
    );
    assertEquals(TransitMode.RAIL, transitMode.mainMode());
    assertEquals("longDistance", transitMode.subMode());
  }

  @Test
  void checkSubModePrecedenceOverMainMode() throws UnsupportedModeException {
    var transitMode = transportModeMapper.map(
      AllVehicleModesOfTransportEnumeration.BUS,
      new TransportSubmodeStructure()
        .withWaterSubmode(WaterSubmodeEnumeration.INTERNATIONAL_PASSENGER_FERRY)
    );
    assertEquals(TransitMode.FERRY, transitMode.mainMode());
    assertEquals("internationalPassengerFerry", transitMode.subMode());
  }

  @Test
  void supportedMode() {
    SUPPORTED_MODES.forEach(mode -> {
      try {
        transportModeMapper.map(mode, null);
      } catch (UnsupportedModeException e) {
        fail("Mode " + mode + " should be supported", e);
      }
    });
  }

  @Test
  void unsupportedMode() {
    EnumSet
      .complementOf(SUPPORTED_MODES)
      .forEach(mode -> {
        assertThrows(UnsupportedModeException.class, () -> transportModeMapper.map(mode, null));
      });

    assertThrows(UnsupportedModeException.class, () -> transportModeMapper.map(null, null));
  }
}
