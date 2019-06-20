package org.opentripplanner.netex.mapping;

import org.junit.Ignore;
import org.junit.Test;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Network;

import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

// TODO OTP2 - Test route color

public class RouteMapperTest {

    @Test
    @Ignore
    public void mapRouteWithDefaultAgency() {
        NetexImportDataIndex netexImportDataIndex = new NetexImportDataIndex();
        Network network = new Network();
        Line line = createExampleLine();
        network.setId("RUT:Network:1");
        netexImportDataIndex.networkByLineId.add(line.getId(), network);

        RouteMapper routeMapper = new RouteMapper(
                new OtpTransitServiceBuilder(),
                netexImportDataIndex.networkByLineId,
                netexImportDataIndex.groupOfLinesByLineId,
                netexImportDataIndex,
                TimeZone.getDefault().toString());

        Route route = routeMapper.mapRoute(line);

        assertEquals( FeedScopedIdFactory.createFeedScopedId("RUT:Line:1"), route.getId());
        assertEquals("Line 1", route.getLongName());
        assertEquals("L1", route.getShortName());
    }

    @Test
    @Ignore
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
        AgencyMapper agencyMapper = new AgencyMapper(TimeZone.getDefault().toString());
        Agency agency = agencyMapper.mapAgency(authority);
        transitBuilder.getAgenciesById().add(agency);

        //
        // netexIndex.authoritiesByNetworkId.add(network.getId(), authority);

        RouteMapper routeMapper = new RouteMapper(
                transitBuilder,
                netexIndex.networkByLineId,
                netexIndex.groupOfLinesByLineId,
                netexIndex,
                TimeZone.getDefault().toString());

        Route route = routeMapper.mapRoute(line);

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
