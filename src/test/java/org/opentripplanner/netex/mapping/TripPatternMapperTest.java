package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.JourneyPatternRefStructure;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.PointsInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.RouteRefStructure;
import org.rutebanken.netex.model.ScheduledStopPoint;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.StopPointInJourneyPatternRefStructure;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.TimetabledPassingTimes_RelStructure;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.netex.mapping.TripPatternMapper.calculateOtpTime;

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

        // Set up NeTEx data structure to map. This includes JourneyPattern and related entities

        TripPatternMapper tripPatternMapper = new TripPatternMapper();
        RouteMapper routeMapper = new RouteMapper();

        NetexImportDataIndex netexIndex = new NetexImportDataIndex();
        OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder();

        Line line = new Line();
        line.setId("RUT:Line:1");
        line.setName(new MultilingualString().withValue("Line 1"));
        line.setTransportMode(AllVehicleModesOfTransportEnumeration.BUS);

        Route route = routeMapper.mapRoute(line, transitBuilder, netexIndex, TimeZone.getDefault().toString());
        transitBuilder.getRoutes().add(route);

        org.rutebanken.netex.model.Route netexRoute = new org.rutebanken.netex.model.Route();
        netexRoute.setId("RUT:Route:1");
        netexIndex.routeById.add(netexRoute);

        JourneyPattern journeyPattern = new JourneyPattern();
        journeyPattern.setId("RUT:JourneyPattern:1");

        RouteRefStructure routeRefStructure = new RouteRefStructure().withRef("RUT:Route:1");
        journeyPattern.setRouteRef(routeRefStructure);

        Collection<TimetabledPassingTime> timetabledPassingTimes = new ArrayList<>();
        TimetabledPassingTime timetabledPassingTime1= new TimetabledPassingTime();
        timetabledPassingTime1.setDepartureTime(LocalTime.of(5, 0));
        timetabledPassingTimes.add(timetabledPassingTime1);
        TimetabledPassingTime timetabledPassingTime2= new TimetabledPassingTime();
        timetabledPassingTime2.setDepartureTime(LocalTime.of(5, 4));
        timetabledPassingTimes.add(timetabledPassingTime2);
        TimetabledPassingTime timetabledPassingTime3= new TimetabledPassingTime();
        timetabledPassingTime3.setDepartureTime(LocalTime.of(5, 10));
        timetabledPassingTimes.add(timetabledPassingTime3);
        TimetabledPassingTime timetabledPassingTime4= new TimetabledPassingTime();
        timetabledPassingTime4.setDepartureTime(LocalTime.of(5, 15));
        timetabledPassingTimes.add(timetabledPassingTime4);
        TimetabledPassingTime timetabledPassingTime5= new TimetabledPassingTime();
        timetabledPassingTime5.setDepartureTime(LocalTime.of(5, 22));
        timetabledPassingTimes.add(timetabledPassingTime5);

        ServiceJourney serviceJourney = new ServiceJourney()
                .withPassingTimes(new TimetabledPassingTimes_RelStructure()
                        .withTimetabledPassingTime(timetabledPassingTimes));
        serviceJourney.setId("RUT:ServiceJourney:1");

        JAXBElement<JourneyPatternRefStructure> journeyPatternRef =
                getJourneyPatternRef(journeyPattern.getId());
        serviceJourney.setJourneyPatternRef(journeyPatternRef);

        JAXBElement<LineRefStructure> lineRef =
                getLineRef(line.getId());
        serviceJourney.setLineRef(lineRef);

        netexRoute.setLineRef(lineRef);

        serviceJourney.setJourneyPatternRef(journeyPatternRef);

        StopPointInJourneyPattern stopPointInJourneyPattern1 = new StopPointInJourneyPattern();
        stopPointInJourneyPattern1.setId("RUT:StopPointInJourneyPattern:1");
        JAXBElement<StopPointInJourneyPatternRefStructure> stopPointInJourneyPatternRef1 =
                getStopPointInJourneyPatternRef(stopPointInJourneyPattern1.getId());
        timetabledPassingTime1.setPointInJourneyPatternRef(stopPointInJourneyPatternRef1);

        StopPointInJourneyPattern stopPointInJourneyPattern2 = new StopPointInJourneyPattern();
        stopPointInJourneyPattern2.setId("RUT:StopPointInJourneyPattern:2");
        JAXBElement<StopPointInJourneyPatternRefStructure> stopPointInJourneyPatternRef2 =
                getStopPointInJourneyPatternRef(stopPointInJourneyPattern2.getId());
        timetabledPassingTime2.setPointInJourneyPatternRef(stopPointInJourneyPatternRef2);

        StopPointInJourneyPattern stopPointInJourneyPattern3 = new StopPointInJourneyPattern();
        stopPointInJourneyPattern3.setId("RUT:StopPointInJourneyPattern:3");
        JAXBElement<StopPointInJourneyPatternRefStructure> stopPointInJourneyPatternRef3 =
                getStopPointInJourneyPatternRef(stopPointInJourneyPattern3.getId());
        timetabledPassingTime3.setPointInJourneyPatternRef(stopPointInJourneyPatternRef3);

        StopPointInJourneyPattern stopPointInJourneyPattern4 = new StopPointInJourneyPattern();
        stopPointInJourneyPattern4.setId("RUT:StopPointInJourneyPattern:4");
        JAXBElement<StopPointInJourneyPatternRefStructure> stopPointInJourneyPatternRef4 =
                getStopPointInJourneyPatternRef(stopPointInJourneyPattern4.getId());
        timetabledPassingTime4.setPointInJourneyPatternRef(stopPointInJourneyPatternRef4);

        StopPointInJourneyPattern stopPointInJourneyPattern5 = new StopPointInJourneyPattern();
        stopPointInJourneyPattern5.setId("RUT:StopPointInJourneyPattern:5");
        JAXBElement<StopPointInJourneyPatternRefStructure> stopPointInJourneyPatternRef5 =
                getStopPointInJourneyPatternRef(stopPointInJourneyPattern5.getId());
        timetabledPassingTime5.setPointInJourneyPatternRef(stopPointInJourneyPatternRef5);

        Collection<PointInLinkSequence_VersionedChildStructure> pointsInLink = new ArrayList<>();
        pointsInLink.add(stopPointInJourneyPattern1);
        pointsInLink.add(stopPointInJourneyPattern2);
        pointsInLink.add(stopPointInJourneyPattern3);
        pointsInLink.add(stopPointInJourneyPattern4);
        pointsInLink.add(stopPointInJourneyPattern5);

        journeyPattern.setPointsInSequence(new PointsInJourneyPattern_RelStructure()
                .withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(pointsInLink));

        ScheduledStopPoint scheduledStopPoint1 = new ScheduledStopPoint();
        scheduledStopPoint1.setId("RUT:ScheduledStopPoint:1");
        ScheduledStopPoint scheduledStopPoint2 = new ScheduledStopPoint();
        scheduledStopPoint2.setId("RUT:ScheduledStopPoint:2");
        ScheduledStopPoint scheduledStopPoint3 = new ScheduledStopPoint();
        scheduledStopPoint3.setId("RUT:ScheduledStopPoint:3");
        ScheduledStopPoint scheduledStopPoint4 = new ScheduledStopPoint();
        scheduledStopPoint4.setId("RUT:ScheduledStopPoint:4");
        ScheduledStopPoint scheduledStopPoint5 = new ScheduledStopPoint();
        scheduledStopPoint5.setId("RUT:ScheduledStopPoint:5");

        JAXBElement<ScheduledStopPointRefStructure> scheduledStopPointRef1 =
                getScheduledStopPointRef(stopPointInJourneyPattern1.getId());
        stopPointInJourneyPattern1.setScheduledStopPointRef(scheduledStopPointRef1);

        JAXBElement<ScheduledStopPointRefStructure> scheduledStopPointRef2 =
                getScheduledStopPointRef(stopPointInJourneyPattern2.getId());
        stopPointInJourneyPattern2.setScheduledStopPointRef(scheduledStopPointRef2);

        JAXBElement<ScheduledStopPointRefStructure> scheduledStopPointRef3 =
                getScheduledStopPointRef(stopPointInJourneyPattern3.getId());
        stopPointInJourneyPattern3.setScheduledStopPointRef(scheduledStopPointRef3);

        JAXBElement<ScheduledStopPointRefStructure> scheduledStopPointRef4 =
                getScheduledStopPointRef(stopPointInJourneyPattern4.getId());
        stopPointInJourneyPattern4.setScheduledStopPointRef(scheduledStopPointRef4);

        JAXBElement<ScheduledStopPointRefStructure> scheduledStopPointRef5 =
                getScheduledStopPointRef(stopPointInJourneyPattern5.getId());
        stopPointInJourneyPattern5.setScheduledStopPointRef(scheduledStopPointRef5);

        Quay quay1 = new Quay();
        quay1.setId("NSR:Quay:1");
        Quay quay2 = new Quay();
        quay2.setId("NSR:Quay:2");
        Quay quay3 = new Quay();
        quay3.setId("NSR:Quay:3");
        Quay quay4 = new Quay();
        quay4.setId("NSR:Quay:4");
        Quay quay5 = new Quay();
        quay5.setId("NSR:Quay:5");

        Stop stop1 = new Stop();
        stop1.setId(FeedScopedIdFactory.createFeedScopedId("NSR:Quay:1"));
        Stop stop2 = new Stop();
        stop2.setId(FeedScopedIdFactory.createFeedScopedId("NSR:Quay:2"));
        Stop stop3 = new Stop();
        stop3.setId(FeedScopedIdFactory.createFeedScopedId("NSR:Quay:3"));
        Stop stop4 = new Stop();
        stop4.setId(FeedScopedIdFactory.createFeedScopedId("NSR:Quay:4"));
        Stop stop5 = new Stop();
        stop5.setId(FeedScopedIdFactory.createFeedScopedId("NSR:Quay:5"));

        transitBuilder.getStops().add(stop1);
        transitBuilder.getStops().add(stop2);
        transitBuilder.getStops().add(stop3);
        transitBuilder.getStops().add(stop4);
        transitBuilder.getStops().add(stop5);

        netexIndex.quayIdByStopPointRef.add(stopPointInJourneyPattern1.getId(), quay1.getId());
        netexIndex.quayIdByStopPointRef.add(stopPointInJourneyPattern2.getId(), quay2.getId());
        netexIndex.quayIdByStopPointRef.add(stopPointInJourneyPattern3.getId(), quay3.getId());
        netexIndex.quayIdByStopPointRef.add(stopPointInJourneyPattern4.getId(), quay4.getId());
        netexIndex.quayIdByStopPointRef.add(stopPointInJourneyPattern5.getId(), quay5.getId());

        DayTypeRefs_RelStructure dayTypeRefs_relStructure = new DayTypeRefs_RelStructure();
        serviceJourney.setDayTypes(dayTypeRefs_relStructure);
        netexIndex.serviceJourneyByPatternId.add(journeyPattern.getId(), serviceJourney);

        // Do the actual mapping of NeTEx JourneyPattern to OTP TripPattern

        tripPatternMapper.mapTripPattern(journeyPattern, transitBuilder, netexIndex);

        // Assert that the mapping is correct

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

    private JAXBElement<ScheduledStopPointRefStructure> getScheduledStopPointRef(String id) {
        return new JAXBElement<>(
                new QName(""),
                ScheduledStopPointRefStructure.class,
                new ScheduledStopPointRefStructure().withRef(id));
    }

    private JAXBElement<StopPointInJourneyPatternRefStructure> getStopPointInJourneyPatternRef(String id) {
        return new JAXBElement<>(
                new QName(""),
                StopPointInJourneyPatternRefStructure.class,
                new StopPointInJourneyPatternRefStructure().withRef(id));
    }

    private JAXBElement<JourneyPatternRefStructure> getJourneyPatternRef(String id) {
        return new JAXBElement<>(
                new QName(""),
                JourneyPatternRefStructure.class,
                new JourneyPatternRefStructure().withRef(id));
    }

    private JAXBElement<LineRefStructure> getLineRef(String id) {
        return new JAXBElement<>(
                new QName(""),
                LineRefStructure.class,
                new LineRefStructure().withRef(id));
    }
}