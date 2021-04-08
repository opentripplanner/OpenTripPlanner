package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMap;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (29.11.2017)
 */
public class TripPatternMapperTest {
    private static final FeedScopedId SERVICE_ID = new FeedScopedId("F", "S01");

    @Test
    public void testMapTripPattern() {

        NetexTestDataSample sample = new NetexTestDataSample();

        TripPatternMapper tripPatternMapper = new TripPatternMapper(
                MappingSupport.ID_FACTORY,
                new EntityById<>(),
                sample.getStopsById(),
                new EntityById<>(),
                sample.getOtpRouteByid(),
                Collections.emptySet(),
                sample.getRouteById(),
                sample.getJourneyPatternById(),
                sample.getQuayIdByStopPointRef(),
                new HierarchicalMap<>(),
                new HierarchicalMapById<>(),
                sample.getServiceJourneyById(),
                new HierarchicalMapById<>(),
                Map.of(NetexTestDataSample.SERVICE_JOURNEY_ID, SERVICE_ID),
                new Deduplicator()
        );

        TripPatternMapper.Result r = tripPatternMapper.mapTripPattern(sample.getJourneyPattern());

        assertEquals(1, r.tripPatterns.size());

        TripPattern tripPattern = r.tripPatterns.get(0);

        assertEquals(4, tripPattern.getStops().size());
        assertEquals(1, tripPattern.getTrips().size());

        List<Stop> stops = tripPattern.getStops();
        Trip trip = tripPattern.getTrips().get(0);

        assertEquals("RUT:ServiceJourney:1", trip.getId().getId());
        assertEquals("NSR:Quay:1", stops.get(0).getId().getId());
        assertEquals("NSR:Quay:2", stops.get(1).getId().getId());
        assertEquals("NSR:Quay:3", stops.get(2).getId().getId());
        assertEquals("NSR:Quay:4", stops.get(3).getId().getId());

        assertEquals(1, tripPattern.scheduledTimetable.tripTimes.size());

        TripTimes tripTimes = tripPattern.scheduledTimetable.tripTimes.get(0);

        assertEquals(4, tripTimes.getNumStops());

        assertEquals(18000, tripTimes.getDepartureTime(0));
        assertEquals(18240, tripTimes.getDepartureTime(1));
        assertEquals(18600, tripTimes.getDepartureTime(2));
        assertEquals(18900, tripTimes.getDepartureTime(3));
    }
}