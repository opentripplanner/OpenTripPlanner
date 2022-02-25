package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Pathway;
import org.onebusaway.gtfs.model.Stop;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;

public class PathwayMapperTest {

    private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

    private static final int PATHWAY_MODE = 2;

    private static final int TRAVERSAL_TIME = 3000;

    private static final Pathway PATHWAY = new Pathway();

    private static final Stop FROM_STOP = new Stop();

    private static final Stop TO_STOP = new Stop();

    static {
        FROM_STOP.setId(AGENCY_AND_ID);
        TO_STOP.setId(AGENCY_AND_ID);

        PATHWAY.setId(AGENCY_AND_ID);
        PATHWAY.setFromStop(FROM_STOP);
        PATHWAY.setToStop(TO_STOP);
        PATHWAY.setPathwayMode(PATHWAY_MODE);
        PATHWAY.setTraversalTime(TRAVERSAL_TIME);
    }

    private PathwayMapper subject = new PathwayMapper(
        new StopMapper(),
        new EntranceMapper(),
        new PathwayNodeMapper(),
        new BoardingAreaMapper()
    );

    @Test
    public void testMapCollection() throws Exception {
        assertNull(subject.map((Collection<Pathway>) null));
        assertTrue(subject.map(Collections.emptyList()).isEmpty());
        assertEquals(1, subject.map(Collections.singleton(PATHWAY)).size());
    }

    @Test
    public void testMap() throws Exception {
        org.opentripplanner.model.Pathway result = subject.map(PATHWAY);

        assertEquals("A:1", result.getId().toString());
        assertNotNull(result.getFromStop());
        assertNotNull(result.getToStop());
        assertEquals(PATHWAY_MODE, result.getPathwayMode());
        assertEquals(TRAVERSAL_TIME, result.getTraversalTime());
    }

    @Test
    public void testMapWithNulls() throws Exception {
        Pathway input = new Pathway();
        input.setId(AGENCY_AND_ID);

        org.opentripplanner.model.Pathway result = subject.map(input);

        assertNotNull(result.getId());
        assertNull(result.getFromStop());
        assertNull(result.getToStop());
        assertEquals(0, result.getPathwayMode());
        assertEquals(0, result.getTraversalTime());
    }

    /** Mapping the same object twice, should return the the same instance. */
    @Test
    public void testMapCache() throws Exception {
        org.opentripplanner.model.Pathway result1 = subject.map(PATHWAY);
        org.opentripplanner.model.Pathway result2 = subject.map(PATHWAY);

        assertTrue(result1 == result2);
    }
}