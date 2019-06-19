package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.math.BigInteger;
import java.time.LocalTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.netex.mapping.StopTimesMapper.calculateOtpTime;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (29.11.2017)
 */
public class TripPatternMapperTest {

    private static final BigInteger ZERO = BigInteger.valueOf(0);
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);

    private static final LocalTime QUARTER_PAST_FIVE = LocalTime.of(5, 15);

    @Test
    public void testCalculateOtpTime() {
        assertEquals(18900, calculateOtpTime(QUARTER_PAST_FIVE, ZERO));
        assertEquals(105300, calculateOtpTime(QUARTER_PAST_FIVE, ONE));
        assertEquals(191700, calculateOtpTime(QUARTER_PAST_FIVE, TWO));
    }

    @Test
    public void testMapTripPattern() {

        TripPatternStructure tripPatternStructure = new TripPatternStructure();

        OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder();

        TripPatternMapper tripPatternMapper = new TripPatternMapper(
                transitBuilder,
                tripPatternStructure.getOtpRouteByid(),
                tripPatternStructure.getRouteById(),
                tripPatternStructure.getJourneyPatternById(),
                tripPatternStructure.getQuayIdByStopPointRef(),
                new HierarchicalMapById<>(),
                tripPatternStructure.getServiceJourneyByPatternId(),
                tripPatternStructure.getStopsById()
        );

        tripPatternMapper.mapTripPattern(tripPatternStructure.getJourneyPattern());

        assertEquals(1, transitBuilder.getTripPatterns().size());

        TripPattern tripPattern = transitBuilder.getTripPatterns().values().stream().findFirst().get();

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