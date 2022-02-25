package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.netex.mapping.MappingSupport.createJaxbElement;

import java.util.Collections;
import java.util.Set;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.BikeAccess;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Branding;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.BrandingRefStructure;
import org.rutebanken.netex.model.GroupOfLinesRefStructure;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.OrganisationRefStructure;
import org.rutebanken.netex.model.PresentationStructure;

public class RouteMapperTest {

    private static final String NETWORK_ID = "RUT:Network:1";
    private static final String AUTHORITY_ID = "RUT:Authority:1";
    private static final String BRANDING_ID = "RUT:Branding:1";
    private static final String RUT_LINE_ID = "RUT:Line:1";
    private static final String RUT_FERRY_WITHOUT_BICYCLES_ID = "RUT:Line:2:NoBicycles";

    private static final String TIME_ZONE = "GMT";

    private static final Set<String> EMPTY_FERRY_WITHOUT_BICYCLE_IDS = Collections.emptySet();

    @Test
    public void mapRouteWithDefaultAgency() {
        NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
        Line line = createExampleLine();

        RouteMapper routeMapper = new RouteMapper(
                new DataImportIssueStore(false),
                MappingSupport.ID_FACTORY,
                new EntityById<>(),
                new EntityById<>(),
                new EntityById<>(),
                netexEntityIndex.readOnlyView(),
                TimeZone.getDefault().toString(),
                EMPTY_FERRY_WITHOUT_BICYCLE_IDS
        );

        Route route = routeMapper.mapRoute(line);

        assertEquals(MappingSupport.ID_FACTORY.createId("RUT:Line:1"), route.getId());
        assertEquals("Line 1", route.getLongName());
        assertEquals("L1", route.getShortName());
    }

    @Test
    public void mapRouteWithAgencySpecified() {
        NetexEntityIndex netexIndex = new NetexEntityIndex();
        OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder();

        Network network = new Network()
                .withId(NETWORK_ID)
                .withTransportOrganisationRef(
                        createJaxbElement(new OrganisationRefStructure().withRef(AUTHORITY_ID))
                );

        netexIndex.networkById.add(network);
        netexIndex.authoritiesById.add(new Authority().withId(AUTHORITY_ID));

        transitBuilder.getAgenciesById().add(createAgency());

        Line line = createExampleLine();

        RouteMapper routeMapper = new RouteMapper(
                new DataImportIssueStore(false),
                MappingSupport.ID_FACTORY,
                transitBuilder.getAgenciesById(),
                transitBuilder.getOperatorsById(),
                transitBuilder.getBrandingsById(),
                netexIndex.readOnlyView(),
                TIME_ZONE,
                EMPTY_FERRY_WITHOUT_BICYCLE_IDS
        );

        Route route = routeMapper.mapRoute(line);

        assertEquals(AUTHORITY_ID, route.getAgency().getId().getId());
    }

    @Test
    public void mapRouteWithColor() {
        NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
        Line line = createExampleLine();
        byte[] color = new byte[]{127, 0, 0};
        byte[] textColor = new byte[]{0, 127, 0};
        line.setPresentation(
                new PresentationStructure()
                        .withColour(color)
                        .withTextColour(textColor));

        RouteMapper routeMapper = new RouteMapper(
                new DataImportIssueStore(false),
                MappingSupport.ID_FACTORY,
                new EntityById<>(),
                new EntityById<>(),
                new EntityById<>(),
                netexEntityIndex.readOnlyView(),
                TimeZone.getDefault().toString(),
                EMPTY_FERRY_WITHOUT_BICYCLE_IDS
        );

        Route route = routeMapper.mapRoute(line);

        assertEquals(route.getColor(), "7F0000");
        assertEquals(route.getTextColor(), "007F00");
    }

    @Test
    public void allowBicyclesOnFerries() {
        NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
        Line lineWithBicycles = createExampleFerry(RUT_LINE_ID);
        Line lineWithOutBicycles = createExampleFerry(RUT_FERRY_WITHOUT_BICYCLES_ID);

        RouteMapper routeMapper = new RouteMapper(
                new DataImportIssueStore(false),
                MappingSupport.ID_FACTORY,
                new EntityById<>(),
                new EntityById<>(),
                new EntityById<>(),
                netexEntityIndex.readOnlyView(),
                TimeZone.getDefault().toString(),
                Set.of(RUT_FERRY_WITHOUT_BICYCLES_ID)
        );

        Route ferryWithBicycles = routeMapper.mapRoute(lineWithBicycles);
        assertEquals(BikeAccess.ALLOWED, ferryWithBicycles.getBikesAllowed());

        Route ferryWithOutBicycles = routeMapper.mapRoute(lineWithOutBicycles);
        assertEquals(BikeAccess.NOT_ALLOWED, ferryWithOutBicycles.getBikesAllowed());
    }

    @Test
    public void mapRouteWithoutBranding() {
        NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
        Line line = createExampleLine();

        RouteMapper routeMapper = new RouteMapper(
                new DataImportIssueStore(false),
                MappingSupport.ID_FACTORY,
                new EntityById<>(),
                new EntityById<>(),
                new EntityById<>(),
                netexEntityIndex.readOnlyView(),
                TimeZone.getDefault().toString(),
                EMPTY_FERRY_WITHOUT_BICYCLE_IDS
        );

        Route route = routeMapper.mapRoute(line);

        assertNull(route.getBranding());
    }

    @Test
    public void mapRouteWithBranding() {
        NetexEntityIndex netexIndex = new NetexEntityIndex();
        OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder();

        transitBuilder.getBrandingsById()
                .add(new Branding(MappingSupport.ID_FACTORY.createId(BRANDING_ID)));

        Line line = createExampleLine();

        RouteMapper routeMapper = new RouteMapper(
                new DataImportIssueStore(false),
                MappingSupport.ID_FACTORY,
                transitBuilder.getAgenciesById(),
                transitBuilder.getOperatorsById(),
                transitBuilder.getBrandingsById(),
                netexIndex.readOnlyView(),
                TIME_ZONE,
                EMPTY_FERRY_WITHOUT_BICYCLE_IDS
        );

        Route route = routeMapper.mapRoute(line);

        Branding branding = route.getBranding();
        assertNotNull(branding);
        assertEquals(BRANDING_ID, branding.getId().getId());
    }

    private Line createExampleLine() {
        Line line = new Line();
        line.setId(RUT_LINE_ID);
        line.setTransportMode(AllVehicleModesOfTransportEnumeration.METRO);
        line.setName(new MultilingualString().withValue("Line 1"));
        line.setPublicCode("L1");
        line.setRepresentedByGroupRef(new GroupOfLinesRefStructure().withRef(NETWORK_ID));
        line.setBrandingRef(new BrandingRefStructure().withRef(BRANDING_ID));
        return line;
    }

    private Line createExampleFerry(String id) {
        var ferry = createExampleLine();
        ferry.setId(id);
        ferry.setTransportMode(AllVehicleModesOfTransportEnumeration.WATER);
        return ferry;
    }

    private Agency createAgency() {
        return new Agency(
                MappingSupport.ID_FACTORY.createId(AUTHORITY_ID),
                "Ruter AS",
                TIME_ZONE
        );
    }
}