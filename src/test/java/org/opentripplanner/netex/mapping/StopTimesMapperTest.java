package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMap;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;

import java.math.BigInteger;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class StopTimesMapperTest {

    private static final BigInteger ZERO = BigInteger.valueOf(0);
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);

    private static final LocalTime QUARTER_PAST_FIVE = LocalTime.of(5, 15);
    public static final Trip TRIP = new Trip(new FeedScopedId("F", "T1"));

    @Test
    public void testCalculateOtpTime() {
        assertEquals(18900, StopTimesMapper.calculateOtpTime(QUARTER_PAST_FIVE, ZERO));
        assertEquals(105300, StopTimesMapper.calculateOtpTime(QUARTER_PAST_FIVE, ONE));
        assertEquals(191700, StopTimesMapper.calculateOtpTime(QUARTER_PAST_FIVE, TWO));
    }

    @Test
    public void testMapStopTimes() {
        NetexTestDataSample sample = new NetexTestDataSample();

        StopTimesMapper stopTimesMapper = new StopTimesMapper(
                MappingSupport.ID_FACTORY,
                sample.getStopsById(),
                new EntityById<>(),
                sample.getDestinationDisplayById(),
                sample.getQuayIdByStopPointRef(),
                new HierarchicalMap<>(),
                new HierarchicalMapById<>(),
                new HierarchicalMap<>()
        );

        StopTimesMapper.MappedStopTimes result = stopTimesMapper.mapToStopTimes(
                sample.getJourneyPattern(),
                TRIP,
                sample.getTimetabledPassingTimes(),
                null
        );

        List<StopTime> stopTimes = result.stopTimes;

        assertEquals(4, stopTimes.size());

        assertStop(stopTimes.get(0),"NSR:Quay:1", 18000, "Bergen");
        assertStop(stopTimes.get(1),"NSR:Quay:2", 18240, "Bergen");
        assertStop(stopTimes.get(2),"NSR:Quay:3", 18600, "Stavanger");
        assertStop(stopTimes.get(3),"NSR:Quay:4", 18900, "Stavanger");

        Map<String, StopTime> map = result.stopTimeByNetexId;

        assertEquals(stopTimes.get(0), map.get("TTPT-1"));
        assertEquals(stopTimes.get(1), map.get("TTPT-2"));
        assertEquals(stopTimes.get(2), map.get("TTPT-3"));
        assertEquals(stopTimes.get(3), map.get("TTPT-4"));
    }

    private void assertStop(StopTime stopTime, String stopId, long departureTime, String headsign) {
        assertEquals(stopId, stopTime.getStop().getId().getId());
        assertEquals(departureTime, stopTime.getDepartureTime());
        assertEquals(headsign, stopTime.getStopHeadsign());
    }
}