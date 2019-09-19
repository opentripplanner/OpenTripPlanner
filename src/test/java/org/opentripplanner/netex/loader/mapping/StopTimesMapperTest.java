package org.opentripplanner.netex.loader.mapping;

import org.junit.Test;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;

import java.math.BigInteger;
import java.time.LocalTime;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class StopTimesMapperTest {

    private static final BigInteger ZERO = BigInteger.valueOf(0);
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);

    private static final LocalTime QUARTER_PAST_FIVE = LocalTime.of(5, 15);

    @Test
    public void testCalculateOtpTime() {
        assertEquals(18900, StopTimesMapper.calculateOtpTime(QUARTER_PAST_FIVE, ZERO));
        assertEquals(105300, StopTimesMapper.calculateOtpTime(QUARTER_PAST_FIVE, ONE));
        assertEquals(191700, StopTimesMapper.calculateOtpTime(QUARTER_PAST_FIVE, TWO));
    }

    @Test
    public void testMapStopTimes() {
        TripPatternStructure tripPatternStructure = new TripPatternStructure();

        StopTimesMapper stopTimesMapper = new StopTimesMapper(
                tripPatternStructure.getDestinationDisplayById(),
                tripPatternStructure.getStopsById(),
                tripPatternStructure.getQuayIdByStopPointRef()
        );

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