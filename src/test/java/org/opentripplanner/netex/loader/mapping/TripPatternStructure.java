package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.loader.util.HierarchicalMap;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.netex.loader.util.HierarchicalMultimap;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opentripplanner.netex.loader.mapping.MappingSupport.createWrappedRef;

class TripPatternStructure {

    private HierarchicalMapById<DestinationDisplay> destinationDisplayById = new HierarchicalMapById<>();

    private EntityById<FeedScopedId, Stop> stopsById = new EntityById<>();

    private HierarchicalMap<String, String> quayIdByStopPointRef = new HierarchicalMap<>();

    private JourneyPattern journeyPattern = new JourneyPattern();

    private List<TimetabledPassingTime> timetabledPassingTimes = new ArrayList<>();

    private HierarchicalMultimap<String, ServiceJourney> serviceJourneyByPatternId = new HierarchicalMultimap<>();

    private HierarchicalMapById<Route> routesById = new HierarchicalMapById<>();

    private Map<String, StopTime> stopTimesById = new HashMap<>();

    private HierarchicalMapById<Route> tripsById = new HierarchicalMapById<>();

    private HierarchicalMapById<JourneyPattern> journeyPatternById = new HierarchicalMapById<>();

    private final EntityById<FeedScopedId, org.opentripplanner.model.Route> otpRouteByid = new EntityById<>();

    TripPatternStructure() {
        Line line = new Line();
        line.setId("RUT:Line:1");
        line.setName(new MultilingualString().withValue("Line 1"));
        line.setTransportMode(AllVehicleModesOfTransportEnumeration.BUS);

        org.opentripplanner.model.Route otpRoute = new org.opentripplanner.model.Route();
        otpRoute.setId(FeedScopedIdFactory.createFeedScopedId("RUT:Line:1"));
        otpRouteByid.add(otpRoute);

        TimetabledPassingTime passingTime1 = new TimetabledPassingTime()
                .withId("TTPT-1")
                .withDepartureTime(LocalTime.of(5, 0));
        TimetabledPassingTime passingTime2 = new TimetabledPassingTime()
                .withId("TTPT-2")
                .withDepartureTime(LocalTime.of(5, 4));
        TimetabledPassingTime passingTime3 = new TimetabledPassingTime()
                .withId("TTPT-3")
                .withDepartureTime(LocalTime.of(5, 10));
        TimetabledPassingTime passingTime4 = new TimetabledPassingTime()
                .withId("TTPT-4")
                .withDepartureTime(LocalTime.of(5, 15));
        TimetabledPassingTime passingTime5 = new TimetabledPassingTime()
                .withId("TTPT-5")
                .withDepartureTime(LocalTime.of(5, 22));

        timetabledPassingTimes.addAll(
                Arrays.asList(passingTime1, passingTime2, passingTime3, passingTime4, passingTime5)
        );

        Route route = new Route().withId("RUT:Route:1");

        ServiceJourney serviceJourney = new ServiceJourney()
                .withId("RUT:ServiceJourney:1")
                .withPassingTimes(new TimetabledPassingTimes_RelStructure()
                        .withTimetabledPassingTime(timetabledPassingTimes));

        DayTypeRefs_RelStructure dayTypeRefs_relStructure = new DayTypeRefs_RelStructure();
        serviceJourney.setDayTypes(dayTypeRefs_relStructure);

        JAXBElement<LineRefStructure> lineRef =
                createLineRef(line.getId());
        serviceJourney.setLineRef(lineRef);

        route.setLineRef(lineRef);

        routesById.add(route);

        RouteRefStructure routeRefStructure = new RouteRefStructure().withRef("RUT:Route:1");
        journeyPattern.setRouteRef(routeRefStructure);

        DestinationDisplay destinationDisplay1 = new DestinationDisplay()
                .withId("NSR:DestinationDisplay:1")
                .withFrontText(new MultilingualString().withValue("Bergen"));

        DestinationDisplay destinationDisplay2 = new DestinationDisplay()
                .withId("NSR:DestinationDisplay:2")
                .withFrontText(new MultilingualString().withValue("Stavanger"));

        destinationDisplayById.add(destinationDisplay1);
        destinationDisplayById.add(destinationDisplay2);

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

        stopsById.add(stop1);
        stopsById.add(stop2);
        stopsById.add(stop3);
        stopsById.add(stop4);
        stopsById.add(stop5);

        journeyPattern.setId("RUT:JourneyPattern:1");

        journeyPatternById.add(journeyPattern);

        serviceJourney.setJourneyPatternRef(createJourneyPatternRef(journeyPattern.getId()));

        StopPointInJourneyPattern stopPointInJourneyPattern1 = new StopPointInJourneyPattern();
        stopPointInJourneyPattern1.setId("RUT:StopPointInJourneyPattern:1");
        JAXBElement<StopPointInJourneyPatternRefStructure> stopPointInJourneyPatternRef1 =
                createStopPointInJourneyPatternRef(stopPointInJourneyPattern1.getId());
        passingTime1.setPointInJourneyPatternRef(stopPointInJourneyPatternRef1);

        stopPointInJourneyPattern1.setDestinationDisplayRef(createDestinationDisplayRef(destinationDisplay1.getId()).getValue());

        StopPointInJourneyPattern stopPointInJourneyPattern2 = new StopPointInJourneyPattern();
        stopPointInJourneyPattern2.setId("RUT:StopPointInJourneyPattern:2");
        JAXBElement<StopPointInJourneyPatternRefStructure> stopPointInJourneyPatternRef2 =
                createStopPointInJourneyPatternRef(stopPointInJourneyPattern2.getId());
        passingTime2.setPointInJourneyPatternRef(stopPointInJourneyPatternRef2);

        StopPointInJourneyPattern stopPointInJourneyPattern3 = new StopPointInJourneyPattern();
        stopPointInJourneyPattern3.setId("RUT:StopPointInJourneyPattern:3");
        JAXBElement<StopPointInJourneyPatternRefStructure> stopPointInJourneyPatternRef3 =
                createStopPointInJourneyPatternRef(stopPointInJourneyPattern3.getId());
        passingTime3.setPointInJourneyPatternRef(stopPointInJourneyPatternRef3);

        StopPointInJourneyPattern stopPointInJourneyPattern4 = new StopPointInJourneyPattern();
        stopPointInJourneyPattern4.setId("RUT:StopPointInJourneyPattern:4");
        JAXBElement<StopPointInJourneyPatternRefStructure> stopPointInJourneyPatternRef4 =
                createStopPointInJourneyPatternRef(stopPointInJourneyPattern4.getId());
        passingTime4.setPointInJourneyPatternRef(stopPointInJourneyPatternRef4);

        stopPointInJourneyPattern4.setDestinationDisplayRef(createDestinationDisplayRef(destinationDisplay2.getId()).getValue());

        StopPointInJourneyPattern stopPointInJourneyPattern5 = new StopPointInJourneyPattern();
        stopPointInJourneyPattern5.setId("RUT:StopPointInJourneyPattern:5");
        JAXBElement<StopPointInJourneyPatternRefStructure> stopPointInJourneyPatternRef5 =
                createStopPointInJourneyPatternRef(stopPointInJourneyPattern5.getId());
        passingTime5.setPointInJourneyPatternRef(stopPointInJourneyPatternRef5);

        JAXBElement<ScheduledStopPointRefStructure> scheduledStopPointRef1 =
                createScheduledStopPointRef(stopPointInJourneyPattern1.getId());
        stopPointInJourneyPattern1.setScheduledStopPointRef(scheduledStopPointRef1);

        JAXBElement<ScheduledStopPointRefStructure> scheduledStopPointRef2 =
                createScheduledStopPointRef(stopPointInJourneyPattern2.getId());
        stopPointInJourneyPattern2.setScheduledStopPointRef(scheduledStopPointRef2);

        JAXBElement<ScheduledStopPointRefStructure> scheduledStopPointRef3 =
                createScheduledStopPointRef(stopPointInJourneyPattern3.getId());
        stopPointInJourneyPattern3.setScheduledStopPointRef(scheduledStopPointRef3);

        JAXBElement<ScheduledStopPointRefStructure> scheduledStopPointRef4 =
                createScheduledStopPointRef(stopPointInJourneyPattern4.getId());
        stopPointInJourneyPattern4.setScheduledStopPointRef(scheduledStopPointRef4);

        JAXBElement<ScheduledStopPointRefStructure> scheduledStopPointRef5 =
                createScheduledStopPointRef(stopPointInJourneyPattern5.getId());
        stopPointInJourneyPattern5.setScheduledStopPointRef(scheduledStopPointRef5);

        Collection<PointInLinkSequence_VersionedChildStructure> pointsInLink = new ArrayList<>();
        pointsInLink.add(stopPointInJourneyPattern1);
        pointsInLink.add(stopPointInJourneyPattern2);
        pointsInLink.add(stopPointInJourneyPattern3);
        pointsInLink.add(stopPointInJourneyPattern4);
        pointsInLink.add(stopPointInJourneyPattern5);

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

        journeyPattern.setPointsInSequence(new PointsInJourneyPattern_RelStructure()
                .withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(pointsInLink));

        quayIdByStopPointRef.add(stopPointInJourneyPattern1.getId(), quay1.getId());
        quayIdByStopPointRef.add(stopPointInJourneyPattern2.getId(), quay2.getId());
        quayIdByStopPointRef.add(stopPointInJourneyPattern3.getId(), quay3.getId());
        quayIdByStopPointRef.add(stopPointInJourneyPattern4.getId(), quay4.getId());
        quayIdByStopPointRef.add(stopPointInJourneyPattern5.getId(), quay5.getId());

        serviceJourneyByPatternId.add(journeyPattern.getId(), serviceJourney);
    }

    HierarchicalMapById<DestinationDisplay> getDestinationDisplayById() {
        return destinationDisplayById;
    }

    EntityById<FeedScopedId, Stop> getStopsById() {
        return stopsById;
    }

    EntityById<FeedScopedId, Stop> getTripsById() {
        return stopsById;
    }

    HierarchicalMap<String, String> getQuayIdByStopPointRef() {
        return quayIdByStopPointRef;
    }

    JourneyPattern getJourneyPattern() {
        return journeyPattern;
    }

    List<TimetabledPassingTime> getTimetabledPassingTimes() {
        return timetabledPassingTimes;
    }

    HierarchicalMultimap<String, ServiceJourney> getServiceJourneyByPatternId() {
        return serviceJourneyByPatternId;
    }

    HierarchicalMapById<Route> getRouteById() { return routesById; }

    Map<String, StopTime> getStopTimesById() { return stopTimesById; }

    HierarchicalMapById<JourneyPattern> getJourneyPatternById() { return journeyPatternById; }

    EntityById<FeedScopedId, org.opentripplanner.model.Route> getOtpRouteByid() { return otpRouteByid; }

    private static JAXBElement<ScheduledStopPointRefStructure> createScheduledStopPointRef(String id) {
        return createWrappedRef(id, ScheduledStopPointRefStructure.class);
    }

    private static JAXBElement<StopPointInJourneyPatternRefStructure> createStopPointInJourneyPatternRef(String id) {
        return createWrappedRef(id, StopPointInJourneyPatternRefStructure.class);
    }

    private static JAXBElement<JourneyPatternRefStructure> createJourneyPatternRef(String id) {
        return createWrappedRef(id, JourneyPatternRefStructure.class);
    }

    private static JAXBElement<LineRefStructure> createLineRef(String id) {
        return createWrappedRef(id, LineRefStructure.class);
    }
    private static JAXBElement<DestinationDisplayRefStructure> createDestinationDisplayRef(String id) {
        return createWrappedRef(id, DestinationDisplayRefStructure.class);
    }
}
