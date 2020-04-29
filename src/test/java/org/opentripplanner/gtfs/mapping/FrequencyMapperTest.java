package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Frequency;
import org.onebusaway.gtfs.model.Trip;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FrequencyMapperTest {
    private static final String FEED_ID = "FEED";

    private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

    private static final Integer ID = 45;

    private static final int START_TIME = 1200;

    private static final int END_TIME = 2300;

    private static final int EXACT_TIMES = 1;

    private static final int HEADWAY_SECS = 2;

    private static final int LABEL_ONLY = 1;

    private static final Trip TRIP = new Trip();

    private static final Frequency FREQUENCY = new Frequency();

    static {
        TRIP.setId(AGENCY_AND_ID);

        FREQUENCY.setId(ID);
        FREQUENCY.setStartTime(START_TIME);
        FREQUENCY.setEndTime(END_TIME);
        FREQUENCY.setExactTimes(EXACT_TIMES);
        FREQUENCY.setHeadwaySecs(HEADWAY_SECS);
        FREQUENCY.setLabelOnly(LABEL_ONLY);
        FREQUENCY.setTrip(TRIP);
    }

    private FrequencyMapper subject = new FrequencyMapper(
            new TripMapper(new RouteMapper(new AgencyMapper(FEED_ID))));

    @Test
    public void testMapCollection() throws Exception {
        assertNull(null, subject.map((Collection<Frequency>) null));
        assertTrue(subject.map(Collections.emptyList()).isEmpty());
        assertEquals(1, subject.map(Collections.singleton(FREQUENCY)).size());
    }

    @Test
    public void testMap() throws Exception {
        org.opentripplanner.model.Frequency result = subject.map(FREQUENCY);

        assertEquals(START_TIME, result.getStartTime());
        assertEquals(END_TIME, result.getEndTime());
        assertEquals(EXACT_TIMES, result.getExactTimes());
        assertEquals(HEADWAY_SECS, result.getHeadwaySecs());
        assertEquals(LABEL_ONLY, result.getLabelOnly());
        assertNotNull(result.getTrip());
    }

    @Test
    public void testMapWithNulls() throws Exception {
        org.opentripplanner.model.Frequency result = subject.map(new Frequency());

        assertEquals(0, result.getStartTime());
        assertEquals(0, result.getEndTime());
        assertEquals(0, result.getExactTimes());
        assertEquals(0, result.getHeadwaySecs());
        assertEquals(0, result.getLabelOnly());
        assertNull(result.getTrip());
    }

    /** Mapping the same object twice, should return the the same instance. */
    @Test
    public void testMapCache() throws Exception {
        org.opentripplanner.model.Frequency result1 = subject.map(FREQUENCY);
        org.opentripplanner.model.Frequency result2 = subject.map(FREQUENCY);

        assertTrue(result1 == result2);
    }

}