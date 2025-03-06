package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.netex.mapping.TransportModeMapper.UnsupportedModeException;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.rutebanken.netex.model.AirSubmodeEnumeration;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.CoachSubmodeEnumeration;
import org.rutebanken.netex.model.FunicularSubmodeEnumeration;
import org.rutebanken.netex.model.MetroSubmodeEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.TaxiSubmodeEnumeration;
import org.rutebanken.netex.model.TelecabinSubmodeEnumeration;
import org.rutebanken.netex.model.TramSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.rutebanken.netex.model.WaterSubmodeEnumeration;

class TransportModeMapperTest {

  private static final Map<
    AllVehicleModesOfTransportEnumeration,
    TransportSubmodeStructure
  > VALID_SUBMODE_STRUCTURES = Map.of(
    AllVehicleModesOfTransportEnumeration.AIR,
    new TransportSubmodeStructure().withAirSubmode(AirSubmodeEnumeration.DOMESTIC_FLIGHT),
    AllVehicleModesOfTransportEnumeration.BUS,
    new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS),
    AllVehicleModesOfTransportEnumeration.CABLEWAY,
    new TransportSubmodeStructure().withTelecabinSubmode(TelecabinSubmodeEnumeration.TELECABIN),
    AllVehicleModesOfTransportEnumeration.COACH,
    new TransportSubmodeStructure().withCoachSubmode(CoachSubmodeEnumeration.NATIONAL_COACH),
    AllVehicleModesOfTransportEnumeration.FUNICULAR,
    new TransportSubmodeStructure().withFunicularSubmode(FunicularSubmodeEnumeration.FUNICULAR),
    AllVehicleModesOfTransportEnumeration.METRO,
    new TransportSubmodeStructure().withMetroSubmode(MetroSubmodeEnumeration.METRO),
    AllVehicleModesOfTransportEnumeration.RAIL,
    new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.LONG_DISTANCE),
    AllVehicleModesOfTransportEnumeration.TAXI,
    new TransportSubmodeStructure().withTaxiSubmode(TaxiSubmodeEnumeration.COMMUNAL_TAXI),
    AllVehicleModesOfTransportEnumeration.TRAM,
    new TransportSubmodeStructure().withTramSubmode(TramSubmodeEnumeration.CITY_TRAM),
    AllVehicleModesOfTransportEnumeration.WATER,
    new TransportSubmodeStructure()
      .withWaterSubmode(WaterSubmodeEnumeration.INTERNATIONAL_PASSENGER_FERRY)
  );

  private static final EnumSet<AllVehicleModesOfTransportEnumeration> SUPPORTED_MODES =
    EnumSet.copyOf(VALID_SUBMODE_STRUCTURES.keySet());

  private final TransportModeMapper transportModeMapper = new TransportModeMapper();

  @Test
  void mapWithTransportModeOnly() throws UnsupportedModeException {
    var transitMode = transportModeMapper.map(AllVehicleModesOfTransportEnumeration.BUS, null);
    assertEquals(TransitMode.BUS, transitMode.mainMode());
    assertNull(transitMode.subMode());
  }

  @Test
  void mapCableway() throws UnsupportedModeException {
    var transitMode = transportModeMapper.map(AllVehicleModesOfTransportEnumeration.CABLEWAY, null);
    assertEquals(TransitMode.GONDOLA, transitMode.mainMode());
    assertNull(transitMode.subMode());
  }

  @Test
  void mapWithSubMode() throws UnsupportedModeException {
    var transitMode = transportModeMapper.map(
      AllVehicleModesOfTransportEnumeration.RAIL,
      VALID_SUBMODE_STRUCTURES.get(AllVehicleModesOfTransportEnumeration.RAIL)
    );
    assertEquals(TransitMode.RAIL, transitMode.mainMode());
    assertEquals("longDistance", transitMode.subMode());
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("createSubModeTestCases")
  void acceptAllValidSubModes(
    AllVehicleModesOfTransportEnumeration mode,
    TransportSubmodeStructure submodeStructure
  ) throws UnsupportedModeException {
    assertNotNull(transportModeMapper.map(mode, submodeStructure));
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
    EnumSet.complementOf(SUPPORTED_MODES).forEach(mode -> {
      assertThrows(UnsupportedModeException.class, () -> transportModeMapper.map(mode, null));
    });

    assertThrows(UnsupportedModeException.class, () -> transportModeMapper.map(null, null));
  }

  private static List<Arguments> createSubModeTestCases() {
    return VALID_SUBMODE_STRUCTURES.entrySet()
      .stream()
      .map(entry -> Arguments.of(entry.getKey(), entry.getValue()))
      .toList();
  }
}
