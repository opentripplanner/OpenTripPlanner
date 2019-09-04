package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;

import java.util.List;

import static org.junit.Assert.*;

public class StopTimesMapperTest {
    @Test
    public void testMapStopTimes() {
        TripPatternStructure tripPatternStructure = new TripPatternStructure();

        StopTimesMapper stopTimesMapper = new StopTimesMapper(
                tripPatternStructure.getDestinationDisplayById(),
                tripPatternStructure.getStopsById(),
                tripPatternStructure.getQuayIdByStopPointRef());

        List<StopTime> stopTimes = stopTimesMapper.mapToStopTimes(
                tripPatternStructure.getJourneyPattern(),
                new Trip(),
                tripPatternStructure.getTimetabledPassingTimes());

        assertEquals(5, stopTimes.size());

        assertEquals("NSR:Quay:1", stopTimes.get(0).getStop().getId().getId());
        assertEquals("NSR:Quay:2", stopTimes.get(1).getStop().getId().getId());
        assertEquals("NSR:Quay:3", stopTimes.get(2).getStop().getId().getId());
        assertEquals("NSR:Quay:4", stopTimes.get(3).getStop().getId().getId());
        assertEquals("NSR:Quay:5", stopTimes.get(4).getStop().getId().getId());

        assertEquals(18000, stopTimes.get(0).getDepartureTime());
        assertEquals(18240, stopTimes.get(1).getDepartureTime());
        assertEquals(18600, stopTimes.get(2).getDepartureTime());
        assertEquals(18900, stopTimes.get(3).getDepartureTime());
        assertEquals(19320, stopTimes.get(4).getDepartureTime());

        assertEquals("Bergen", stopTimes.get(0).getStopHeadsign());
        assertEquals("Bergen", stopTimes.get(1).getStopHeadsign());
        assertEquals("Bergen", stopTimes.get(2).getStopHeadsign());
        assertEquals("Stavanger", stopTimes.get(3).getStopHeadsign());
        assertEquals("Stavanger", stopTimes.get(4).getStopHeadsign());
    }
}