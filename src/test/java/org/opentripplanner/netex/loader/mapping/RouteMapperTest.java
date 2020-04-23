package org.opentripplanner.netex.loader.mapping;

import org.junit.Test;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.GroupOfLinesRefStructure;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.OrganisationRefStructure;
import org.rutebanken.netex.model.PresentationStructure;

import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.netex.loader.mapping.MappingSupport.createJaxbElement;

public class RouteMapperTest {

    private static final String NETWORK_ID = "RUT:Network:1";
    private static final String AUTHORITY_ID = "RUT:Authority:1";
    private static final String RUT_LINE_ID = "RUT:Line:1";

    private static final String TIME_ZONE = "GMT";

    @Test
    public void mapRouteWithDefaultAgency() {
        NetexImportDataIndex netexImportDataIndex = new NetexImportDataIndex();
        Line line = createExampleLine();

        RouteMapper routeMapper = new RouteMapper(
                MappingSupport.ID_FACTORY,
                new EntityById<>(),
                new EntityById<>(),
                netexImportDataIndex.readOnlyView(),
                TimeZone.getDefault().toString()
        );

        Route route = routeMapper.mapRoute(line);

        assertEquals( MappingSupport.ID_FACTORY.createId("RUT:Line:1"), route.getId());
        assertEquals("Line 1", route.getLongName());
        assertEquals("L1", route.getShortName());
    }

    @Test
    public void mapRouteWithAgencySpecified() {
        NetexImportDataIndex netexIndex = new NetexImportDataIndex();
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
                MappingSupport.ID_FACTORY,
                transitBuilder.getAgenciesById(),
                transitBuilder.getOperatorsById(),
                netexIndex.readOnlyView(),
                TIME_ZONE
        );

        Route route = routeMapper.mapRoute(line);

        assertEquals(AUTHORITY_ID, route.getAgency().getId().getId());
    }

    @Test
    public void mapRouteWithColor() {
        NetexImportDataIndex netexImportDataIndex = new NetexImportDataIndex();
        Line line = createExampleLine();
        byte[] color = new byte[] {127, 0, 0};
        byte[] textColor = new byte[] {0, 127, 0};
        line.setPresentation(
                new PresentationStructure()
                        .withColour(color)
                        .withTextColour(textColor));

        RouteMapper routeMapper = new RouteMapper(
                MappingSupport.ID_FACTORY,
                new EntityById<>(),
                new EntityById<>(),
                netexImportDataIndex.readOnlyView(),
                TimeZone.getDefault().toString()
        );

        Route route = routeMapper.mapRoute(line);

        assertEquals( route.getColor(), "7F0000");
        assertEquals(route.getTextColor(), "007F00");
    }

    private Line createExampleLine() {
        Line line = new Line();
        line.setId(RUT_LINE_ID);
        line.setTransportMode(AllVehicleModesOfTransportEnumeration.METRO);
        line.setName(new MultilingualString().withValue("Line 1"));
        line.setPublicCode("L1");
        line.setRepresentedByGroupRef(new GroupOfLinesRefStructure().withRef(NETWORK_ID));
        return line;
    }

    private Agency createAgency() {
        Agency agency = new Agency();
        agency.setId(MappingSupport.ID_FACTORY.createId(AUTHORITY_ID));
        agency.setTimezone(TIME_ZONE);
        agency.setName("Ruter");
        return agency;
    }
}