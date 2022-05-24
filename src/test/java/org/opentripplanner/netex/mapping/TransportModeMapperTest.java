package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.TransitSubMode;
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
    final T2<TransitMode, TransitSubMode> transitMode = transportModeMapper.map(
      AllVehicleModesOfTransportEnumeration.BUS,
      null
    );
    assertEquals(TransitMode.BUS, transitMode.first);
    assertEquals(TransitSubMode.UNKNOWN, transitMode.second);
  }

  @Test
  public void mapWithSubMode() throws UnsupportedModeException {
    final T2<TransitMode, TransitSubMode> transitMode = transportModeMapper.map(
      AllVehicleModesOfTransportEnumeration.RAIL,
      VALID_SUBMODE_STRCUTURE
    );
    assertEquals(TransitMode.RAIL, transitMode.first);
    assertEquals(TransitSubMode.LONGDISTANCE, transitMode.second);
  }

  @Test
  public void checkSubModePrecedensOverMainMode() throws UnsupportedModeException {
    final T2<TransitMode, TransitSubMode> transitMode = transportModeMapper.map(
      AllVehicleModesOfTransportEnumeration.BUS,
      new TransportSubmodeStructure()
        .withWaterSubmode(WaterSubmodeEnumeration.INTERNATIONAL_PASSENGER_FERRY)
    );
    assertEquals(TransitMode.FERRY, transitMode.first);
    assertEquals(TransitSubMode.INTERNATIONALPASSENGERFERRY, transitMode.second);
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
