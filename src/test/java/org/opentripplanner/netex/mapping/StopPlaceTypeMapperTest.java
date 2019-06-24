package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.VehicleModeEnumeration;

import static org.junit.Assert.*;

public class StopPlaceTypeMapperTest {
    StopPlaceTypeMapper stopPlaceTypeMapper = new StopPlaceTypeMapper();

    @Test
    public void mapWithoutTransportMode() {
        assertEquals(3, stopPlaceTypeMapper.getTransportMode(new StopPlace()
        ));
    }

    @Test
    public void mapWithTransportModeOnly() {
        assertEquals(100, stopPlaceTypeMapper.getTransportMode(new StopPlace()
                .withTransportMode(VehicleModeEnumeration.RAIL)
        ));
    }

    @Test
    public void mapWithSubMode() {
        assertEquals(103, stopPlaceTypeMapper.getTransportMode(new StopPlace()
                .withTransportMode(VehicleModeEnumeration.RAIL)
                .withRailSubmode(RailSubmodeEnumeration.REGIONAL_RAIL)));
    }

    @Test
    public void mapWithSubModeOnly() {
        assertEquals(103, stopPlaceTypeMapper.getTransportMode(new StopPlace()
                .withRailSubmode(RailSubmodeEnumeration.REGIONAL_RAIL)));
    }

    @Test
    public void checkSubModePrecedensOverMainMode() {
        assertEquals(710, stopPlaceTypeMapper.getTransportMode(new StopPlace()
                .withTransportMode(VehicleModeEnumeration.RAIL)
                .withBusSubmode(BusSubmodeEnumeration.SIGHTSEEING_BUS)));
    }
}
