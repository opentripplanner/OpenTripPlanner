package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.rutebanken.netex.model.WaterSubmodeEnumeration;

import static org.junit.Assert.assertEquals;

public class TransportModeMapperTest {
    private TransportModeMapper transportModeMapper = new TransportModeMapper();

    @Test
    public void mapWithTransportModeOnly() {
        assertEquals(
                700,
                transportModeMapper.getTransportMode(
                        AllVehicleModesOfTransportEnumeration.BUS,
                        null
                )
        );
    }

    @Test
    public void mapWithSubMode() {
        assertEquals(
                102,
                transportModeMapper.getTransportMode(
                        AllVehicleModesOfTransportEnumeration.RAIL,
                        new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.LONG_DISTANCE)
                )
        );
    }

    @Test
    public void checkSubModePrecedensOverMainMode() {
        assertEquals(
                1005,
                transportModeMapper.getTransportMode(
                        AllVehicleModesOfTransportEnumeration.BUS,
                        new TransportSubmodeStructure()
                                .withWaterSubmode(WaterSubmodeEnumeration.INTERNATIONAL_PASSENGER_FERRY)
                )
        );
    }
}
