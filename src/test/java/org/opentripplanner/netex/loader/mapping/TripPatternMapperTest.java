package org.opentripplanner.netex.loader.mapping;

import org.junit.Test;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (29.11.2017)
 */
public class TripPatternMapperTest {


    @Test
    public void testMapTripPattern() {

        TripPatternStructure tripPatternStructure = new TripPatternStructure();

        TripPatternMapper tripPatternMapper = new TripPatternMapper(
                tripPatternStructure.getStopsById(),
                tripPatternStructure.getOtpRouteByid(),
                tripPatternStructure.getRouteById(),
                tripPatternStructure.getJourneyPatternById(),
                tripPatternStructure.getQuayIdByStopPointRef(),
                new HierarchicalMapById<>(),
                tripPatternStructure.getServiceJourneyByPatternId(),
                new Deduplicator()
        );

        TripPatternMapper.Result r = tripPatternMapper.mapTripPattern(tripPatternStructure.getJourneyPattern());

        assertEquals(1, r.tripPatterns.size());

        TripPattern tripPattern = r.tripPatterns.get(0);

        assertEquals(5, tripPattern.getStops().size());
        assertEquals(1, tripPattern.getTrips().size());

        List<Stop> stops = tripPattern.getStops();
        Trip trip = tripPattern.getTrips().get(0);

        assertEquals("RUT:ServiceJourney:1", trip.getId().getId());
        assertEquals("NSR:Quay:1", stops.get(0).getId().getId());
        assertEquals("NSR:Quay:2", stops.get(1).getId().getId());
        assertEquals("NSR:Quay:3", stops.get(2).getId().getId());
        assertEquals("NSR:Quay:4", stops.get(3).getId().getId());
        assertEquals("NSR:Quay:5", stops.get(4).getId().getId());

        assertEquals(1, tripPattern.scheduledTimetable.tripTimes.size());

        TripTimes tripTimes = tripPattern.scheduledTimetable.tripTimes.get(0);

        assertEquals(5, tripTimes.getNumStops());

        assertEquals(18000, tripTimes.getDepartureTime(0));
        assertEquals(18240, tripTimes.getDepartureTime(1));
        assertEquals(18600, tripTimes.getDepartureTime(2));
        assertEquals(18900, tripTimes.getDepartureTime(3));
        assertEquals(19320, tripTimes.getDepartureTime(4));
    }
}