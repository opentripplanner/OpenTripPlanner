package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.TransitMode;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.VehicleModeEnumeration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StopPlaceTypeMapperTest {
    private final StopPlaceTypeMapper stopPlaceTypeMapper = new StopPlaceTypeMapper();

    @Test
    public void mapWithoutTransportMode() {
        final T2<TransitMode, String> transitMode = stopPlaceTypeMapper.map(new StopPlace());
        assertNull(transitMode.first);
        assertNull(transitMode.second);
    }

    @Test
    public void mapWithTransportModeOnly() {
        final T2<TransitMode, String> transitMode = stopPlaceTypeMapper.map(new StopPlace()
                .withTransportMode(VehicleModeEnumeration.RAIL)
        );
        assertEquals(TransitMode.RAIL, transitMode.first);
        assertNull(transitMode.second);
    }

    @Test
    public void mapWithSubMode() {
        final T2<TransitMode, String> transitMode = stopPlaceTypeMapper.map(new StopPlace()
                .withTransportMode(VehicleModeEnumeration.RAIL)
                .withRailSubmode(RailSubmodeEnumeration.REGIONAL_RAIL));
        assertEquals(TransitMode.RAIL, transitMode.first);
        assertEquals("regionalRail", transitMode.second);
    }

    @Test
    public void mapWithSubModeOnly() {
        final T2<TransitMode, String> transitMode = stopPlaceTypeMapper.map(new StopPlace()
                .withRailSubmode(RailSubmodeEnumeration.REGIONAL_RAIL));
        assertEquals(TransitMode.RAIL, transitMode.first);
        assertEquals("regionalRail", transitMode.second);
    }

    @Test
    public void checkSubModePrecedensOverMainMode() {
        final T2<TransitMode, String> transitMode = stopPlaceTypeMapper.map(new StopPlace()
                .withTransportMode(VehicleModeEnumeration.RAIL)
                .withBusSubmode(BusSubmodeEnumeration.SIGHTSEEING_BUS));
        assertEquals(TransitMode.BUS, transitMode.first);
        assertEquals("sightseeingBus", transitMode.second);
    }
}
