package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

public class FeedScopedIdMapperTest {

    @Test
    public void testMapAgencyAndId() throws Exception {
        org.onebusaway.gtfs.model.AgencyAndId inputId = new org.onebusaway.gtfs.model.AgencyAndId(
                "A", "1"
        );

        FeedScopedId mappedId = mapAgencyAndId(inputId);

        assertEquals("A", mappedId.getFeedId());
        assertEquals("1", mappedId.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapAgencyAndIdWithNulls() throws Exception {
        mapAgencyAndId(new org.onebusaway.gtfs.model.AgencyAndId());
    }
}