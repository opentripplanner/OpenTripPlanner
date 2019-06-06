package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.VehicleModeEnumeration;

import static org.junit.Assert.*;

// TODO OTP2 - Verify functionality of mapper and fill out test cases

public class StopPlaceTypeMapperTest {
    StopPlaceTypeMapper stopPlaceTypeMapper = new StopPlaceTypeMapper();

    @Test
    public void mapStopPlaceTypes() {
        assertEquals(700, stopPlaceTypeMapper.getTransportMode(new StopPlace()
                .withTransportMode(VehicleModeEnumeration.BUS)
                .withBusSubmode(BusSubmodeEnumeration.REGIONAL_BUS)));
    }

}
