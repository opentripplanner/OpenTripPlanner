package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.*;

import java.util.TimeZone;

import static org.junit.Assert.*;

// TODO OTP2 - Test route color

public class RouteMapperTest {

    RouteMapper routeMapper = new RouteMapper();

    @Test
    public void mapRouteWithDefaultAgency() {
        NetexImportDataIndex netexImportDataIndex = new NetexImportDataIndex();
        Network network = new Network();
        Line line = createExampleLine();
        network.setId("RUT:Network:1");
        netexImportDataIndex.networkByLineId.add(line.getId(), network);

        Route route = routeMapper.mapRoute(line, new OtpTransitServiceBuilder(), netexImportDataIndex, TimeZone.getDefault().toString());

        assertEquals( FeedScopedIdFactory.createFeedScopedId("RUT:Line:1"), route.getId());
        assertEquals("Line 1", route.getLongName());
        assertEquals("L1", route.getShortName());
    }

    @Test
    public void mapRouteWithAgencySpecified() {
        NetexImportDataIndex netexIndex = new NetexImportDataIndex();
        Network network = new Network();
        Line line = createExampleLine();
        network.setId("RUT:Network:1");
        netexIndex.networkByLineId.add(line.getId(), network);

        OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder();
        Authority authority = new Authority();
        authority.setId("RUT:Authority:1");
        authority.setName(new MultilingualString().withValue("Ruter"));
        Agency agency = AgencyMapper.mapAgency(authority, TimeZone.getDefault().toString());
        transitBuilder.getAgenciesById().add(agency);

        netexIndex.authoritiesByNetworkId.add(network.getId(), authority);

        Route route = routeMapper.mapRoute(line, transitBuilder, netexIndex, TimeZone.getDefault().toString());

        assertEquals("RUT:Authority:1", route.getAgency().getId());
    }

    private Line createExampleLine() {
        Line line = new Line();
        line.setId("RUT:Line:1");
        line.setTransportMode(AllVehicleModesOfTransportEnumeration.METRO);
        line.setId("RUT:Line:1");
        line.setName(new MultilingualString().withValue("Line 1"));
        line.setPublicCode("L1");
        return line;
    }
}