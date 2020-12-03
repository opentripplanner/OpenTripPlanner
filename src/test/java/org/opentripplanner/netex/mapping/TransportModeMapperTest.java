package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.opentripplanner.model.modes.TransitMainMode;
import org.opentripplanner.model.modes.TransitMode;
import org.opentripplanner.model.modes.TransitModeService;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.rutebanken.netex.model.WaterSubmodeEnumeration;

import static org.junit.Assert.assertEquals;

public class TransportModeMapperTest {

  private TransportModeMapper transportModeMapper = new TransportModeMapper(TransitModeService.getDefault());

  private TransitModeService transitModeService = TransitModeService.getDefault();

  @Test
  public void mapWithTransportModeOnly() {
    assertEquals(
        TransitMode.BUS,
        transportModeMapper.map(AllVehicleModesOfTransportEnumeration.BUS, null)
    );
  }

  @Test
  public void mapWithSubMode() {
    assertEquals(
        transitModeService.getTransitMode(TransitMainMode.RAIL, "LONG_DISTANCE"),
        transportModeMapper.map(AllVehicleModesOfTransportEnumeration.RAIL,
            new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.LONG_DISTANCE)
        )
    );
  }

  @Test
  public void checkSubModePrecedensOverMainMode() {
    assertEquals(transitModeService.getTransitMode(TransitMainMode.FERRY,
        "INTERNATIONAL_PASSENGER_FERRY"
    ), transportModeMapper.map(AllVehicleModesOfTransportEnumeration.BUS,
        new TransportSubmodeStructure().withWaterSubmode(WaterSubmodeEnumeration.INTERNATIONAL_PASSENGER_FERRY)
    ));
  }
}
