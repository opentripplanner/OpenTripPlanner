package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import static org.junit.Assert.*;

public class TripMapperTest {

    TripMapper tripMapper = new TripMapper();

    @Test
    public void mapTrip() {
        OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder();
        Route route = new Route();
        route.setId(FeedScopedIdFactory.createFeedScopedId("RUT:Route:1"));
        transitBuilder.getRoutes().add(route);

        ServiceJourney serviceJourney = createExampleServiceJourney();

        JAXBElement<LineRefStructure> lineRefStructure =
                new JAXBElement<>(
                        new QName(""),
                        LineRefStructure.class,
                        new LineRefStructure().withRef("RUT:Route:1"));
        serviceJourney.setLineRef(lineRefStructure);

        Trip trip = tripMapper.mapServiceJourney(
                serviceJourney, transitBuilder,
                null,
                null);

        assertEquals(trip.getId(), FeedScopedIdFactory.createFeedScopedId("RUT:ServiceJourney:1"));
    }

    @Test
    public void mapTripWithRouteRefViaJourneyPattern() {
        OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder();
        Route route = new Route();
        route.setId(FeedScopedIdFactory.createFeedScopedId("RUT:Route:1"));
        transitBuilder.getRoutes().add(route);

        ServiceJourney serviceJourney = createExampleServiceJourney();

        JourneyPattern journeyPattern = new JourneyPattern();
        journeyPattern.setId("RUT:JourneyPattern:1");

        JAXBElement<LineRefStructure> lineRefStructure =
                new JAXBElement<>(
                        new QName(""),
                        LineRefStructure.class,
                        new LineRefStructure().withRef("RUT:Route:1"));

        RouteRefStructure routeRefStructure = new RouteRefStructure().withRef("RUT:Route:1");
        journeyPattern.setRouteRef(routeRefStructure);

        JAXBElement<JourneyPatternRefStructure> journeyPatternRefStructure =
                new JAXBElement<>(
                        new QName(""),
                        JourneyPatternRefStructure.class,
                        new JourneyPatternRefStructure().withRef("RUT:JourneyPattern:1"));

        serviceJourney.setJourneyPatternRef(journeyPatternRefStructure);

        org.rutebanken.netex.model.Route netexRoute = new org.rutebanken.netex.model.Route();
        netexRoute.setLineRef(lineRefStructure);
        netexRoute.setId("RUT:Route:1");

        HierarchicalMapById<org.rutebanken.netex.model.Route> routeById = new HierarchicalMapById<>();
        routeById.add(netexRoute);
        HierarchicalMapById<JourneyPattern> journeyPatternById = new HierarchicalMapById<>();
        journeyPatternById.add(journeyPattern);

        Trip trip = tripMapper.mapServiceJourney(
                serviceJourney,
                transitBuilder,
                routeById,
                journeyPatternById);

        assertEquals(trip.getId(), FeedScopedIdFactory.createFeedScopedId("RUT:ServiceJourney:1"));
    }

    private ServiceJourney createExampleServiceJourney() {
        ServiceJourney serviceJourney = new ServiceJourney();
        serviceJourney.setId("RUT:ServiceJourney:1");
        DayTypeRefs_RelStructure dayTypeRefs_relStructure = new DayTypeRefs_RelStructure();
        serviceJourney.setDayTypes(dayTypeRefs_relStructure);
        return serviceJourney;
    }
}