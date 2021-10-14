package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMap;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.JourneyPatternRefStructure;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.RouteRefStructure;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;
import org.rutebanken.netex.model.ServiceJourney;

import javax.xml.bind.JAXBElement;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.netex.mapping.MappingSupport.ID_FACTORY;
import static org.opentripplanner.netex.mapping.MappingSupport.createWrappedRef;

public class TripMapperTest {

    private static final String ROUTE_ID = "RUT:Route:1";
    private static final String SERVICE_JOURNEY_ID = NetexTestDataSample.SERVICE_JOURNEY_ID;
    private static final String JOURNEY_PATTERN_ID = "RUT:JourneyPattern:1";
    private static final FeedScopedId SERVICE_ID = new FeedScopedId("F", "S001");

    private static final JAXBElement<LineRefStructure> LINE_REF = MappingSupport.createWrappedRef(
            ROUTE_ID, LineRefStructure.class
    );

    @Test
    public void mapTrip() {
        OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder();
        Route route = new Route(ID_FACTORY.createId(ROUTE_ID));
        transitBuilder.getRoutes().add(route);

        TripMapper tripMapper = new TripMapper(
            ID_FACTORY,
            transitBuilder.getOperatorsById(),
            transitBuilder.getRoutes(),
            new HierarchicalMapById<>(),
            new HierarchicalMap<>(),
            Map.of(SERVICE_JOURNEY_ID, SERVICE_ID),
            Collections.emptySet()
        );

        ServiceJourney serviceJourney = createExampleServiceJourney();

        serviceJourney.setLineRef(LINE_REF);

        Trip trip = tripMapper.mapServiceJourney(serviceJourney);

        assertEquals(trip.getId(), ID_FACTORY.createId(SERVICE_JOURNEY_ID));
    }

    @Test
    public void mapTripWithRouteRefViaJourneyPattern() {
        OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder();
        Route route = new Route(ID_FACTORY.createId(ROUTE_ID));
        transitBuilder.getRoutes().add(route);

        JourneyPattern journeyPattern = new JourneyPattern().withId(JOURNEY_PATTERN_ID);
        journeyPattern.setRouteRef(new RouteRefStructure().withRef(ROUTE_ID));

        ServiceJourney serviceJourney = createExampleServiceJourney();
        serviceJourney.setJourneyPatternRef(
                MappingSupport.createWrappedRef(JOURNEY_PATTERN_ID, JourneyPatternRefStructure.class)
        );

        org.rutebanken.netex.model.Route netexRoute = new org.rutebanken.netex.model.Route();
        netexRoute.setLineRef(LINE_REF);
        netexRoute.setId(ROUTE_ID);

        HierarchicalMapById<org.rutebanken.netex.model.Route> routeById = new HierarchicalMapById<>();
        routeById.add(netexRoute);
        HierarchicalMapById<JourneyPattern> journeyPatternById = new HierarchicalMapById<>();
        journeyPatternById.add(journeyPattern);

        TripMapper tripMapper = new TripMapper(
                ID_FACTORY,
                transitBuilder.getOperatorsById(),
                transitBuilder.getRoutes(),
                routeById,
                journeyPatternById,
                Map.of(SERVICE_JOURNEY_ID, SERVICE_ID),
                Collections.emptySet()
        );

        Trip trip = tripMapper.mapServiceJourney(serviceJourney);

        assertEquals(trip.getId(), ID_FACTORY.createId("RUT:ServiceJourney:1"));
    }

    private ServiceJourney createExampleServiceJourney() {
        ServiceJourney serviceJourney = new ServiceJourney();
        serviceJourney.setId("RUT:ServiceJourney:1");
        serviceJourney.setDayTypes(NetexTestDataSample.createEveryDayRefs());
        serviceJourney.setJourneyPatternRef(createWrappedRef(
            "RUT:JourneyPattern:1",
            JourneyPatternRefStructure.class
        ));
        return serviceJourney;
    }
}