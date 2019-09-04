package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.rutebanken.netex.model.*;

import static org.junit.Assert.*;

public class TransportModeMapperTest {
    TransportModeMapper transportModeMapper = new TransportModeMapper();

    @Test
    public void mapWithTransportModeOnly() {
        assertEquals(
                700,
                transportModeMapper.getTransportMode(
                        AllVehicleModesOfTransportEnumeration.BUS,
                        null));
    }

    @Test
    public void mapWithSubMode() {
        assertEquals(
                102,
                transportModeMapper.getTransportMode(
                        AllVehicleModesOfTransportEnumeration.RAIL,
                        new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.LONG_DISTANCE)));
    }

    @Test
    public void checkSubModePrecedensOverMainMode() {
        assertEquals(
                1005,
                transportModeMapper.getTransportMode(
                        AllVehicleModesOfTransportEnumeration.BUS,
                        new TransportSubmodeStructure()
                                .withWaterSubmode(WaterSubmodeEnumeration.INTERNATIONAL_PASSENGER_FERRY)));
    }
}
