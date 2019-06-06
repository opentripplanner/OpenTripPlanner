package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;

import static org.junit.Assert.*;

// TODO OTP2 - Verify functionality of mapper and fill out test cases

public class TransportModeMapperTest {
    TransportModeMapper transportModeMapper = new TransportModeMapper();

    @Test
    public void mapTransportModes() {
        assertEquals(
                702,
                transportModeMapper.getTransportMode(
                        AllVehicleModesOfTransportEnumeration.BUS,
                        new TransportSubmodeStructure().withBusSubmode(BusSubmodeEnumeration.EXPRESS_BUS)));
    }
}
