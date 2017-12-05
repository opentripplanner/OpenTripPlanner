package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RouteMapper.class);

    private TransportTypeMapper transportTypeMapper = new TransportTypeMapper();
    private AgencyMapper agencyMapper = new AgencyMapper();

    org.opentripplanner.model.Route mapRoute(Line line, OtpTransitServiceBuilder transitBuilder, NetexDao netexDao, String timeZone){
        org.opentripplanner.model.Route otpRoute = new org.opentripplanner.model.Route();
        Network network = netexDao.lookupNetworkByLineId(line.getId());

        if (network == null || netexDao.lookupAuthoritiesByNetworkId(network.getId()) == null) {
            // Get agencies from serviceJourneys
            LOG.warn("No authority found for " + line.getId());
            Agency agency = agencyMapper.getDefaultAgency(timeZone);
            otpRoute.setAgency(agency);
            if (!transitBuilder.getAgencies().stream().anyMatch(a -> a.getId().equals(agency.getId()))) {
                transitBuilder.getAgencies().add(agency);
            }
        } else {
            Authority authority = netexDao.lookupAuthoritiesByNetworkId(network.getId());
            String agencyId = authority.getId();
            Agency agency = transitBuilder.getAgencies().stream().filter(a -> a.getId().equals(agencyId)).findFirst().get();
            otpRoute.setAgency(agency);
        }
        otpRoute.setId(FeedScopedIdFactory.createFeedScopedId(line.getId()));
        otpRoute.setLongName(line.getName().getValue());
        otpRoute.setShortName(line.getPublicCode());
        otpRoute.setType(transportTypeMapper.mapTransportType(line.getTransportMode().value()));

        return otpRoute;
    }
}