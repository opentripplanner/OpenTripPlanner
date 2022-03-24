package org.opentripplanner.netex.mapping;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMap;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (29.11.2017)
 */
public class TripPatternMapperTest {
    private static final FeedScopedId SERVICE_ID = new FeedScopedId("F", "S01");

    @Test
    public void testMapTripPattern() {

        NetexTestDataSample sample = new NetexTestDataSample();

        TripPatternMapper tripPatternMapper = new TripPatternMapper(
                new DataImportIssueStore(false),
                MappingSupport.ID_FACTORY,
                new EntityById<>(),
                sample.getStopsById(),
                new EntityById<>(),
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

        TripPatternMapperResult r = tripPatternMapper.mapTripPattern(sample.getJourneyPattern());

        assertEquals(1, r.tripPatterns.size());

        TripPattern tripPattern = r.tripPatterns.values().stream().findFirst().orElseThrow();

        assertEquals(4, tripPattern.numberOfStops());
        assertEquals(1, tripPattern.scheduledTripsAsStream().count());

        Trip trip = tripPattern.scheduledTripsAsStream().findFirst().get();

        assertEquals("RUT:ServiceJourney:1", trip.getId().getId());
        assertEquals("NSR:Quay:1", tripPattern.getStop(0).getId().getId());
        assertEquals("NSR:Quay:2", tripPattern.getStop(1).getId().getId());
        assertEquals("NSR:Quay:3", tripPattern.getStop(2).getId().getId());
        assertEquals("NSR:Quay:4", tripPattern.getStop(3).getId().getId());

        assertEquals(1, tripPattern.getScheduledTimetable().getTripTimes().size());

        TripTimes tripTimes = tripPattern.getScheduledTimetable().getTripTimes().get(0);

        assertEquals(4, tripTimes.getNumStops());

        assertEquals(18000, tripTimes.getDepartureTime(0));
        assertEquals(18240, tripTimes.getDepartureTime(1));
        assertEquals(18600, tripTimes.getDepartureTime(2));
        assertEquals(18900, tripTimes.getDepartureTime(3));
    }
}