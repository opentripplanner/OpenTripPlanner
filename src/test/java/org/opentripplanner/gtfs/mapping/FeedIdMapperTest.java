package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.opentripplanner.model.FeedId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

public class FeedIdMapperTest {

    @Test
    public void testMapAgencyAndId() throws Exception {
        org.onebusaway.gtfs.model.AgencyAndId inputId = new org.onebusaway.gtfs.model.AgencyAndId(
                "A", "1"
        );

        FeedId mappedId = mapAgencyAndId(inputId);

        assertEquals("A", mappedId.getAgencyId());
        assertEquals("1", mappedId.getId());
    }

    @Test
    public void testMapAgencyAndIdWithNulls() throws Exception {
        org.onebusaway.gtfs.model.AgencyAndId inputId = new org.onebusaway.gtfs.model.AgencyAndId();

        FeedId mappedId = mapAgencyAndId(inputId);

        assertNull(mappedId.getAgencyId());
        assertNull(mappedId.getId());
    }
}