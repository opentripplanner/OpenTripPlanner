package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.rutebanken.netex.model.Line;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RouteMapper.class);

    private TransportTypeMapper transportTypeMapper = new TransportTypeMapper();

    org.opentripplanner.model.Route mapRoute(Line line, OtpTransitServiceBuilder transitBuilder){

        org.opentripplanner.model.Route otpRoute = new org.opentripplanner.model.Route();

        if (line.getOperatorRef() == null) {
            LOG.warn("Line " + line.getId() + " does not have an operator.");
            Agency unknownAgency = new Agency();
            unknownAgency.setId("Unknown");
            unknownAgency.setName("Unknown agency");
            otpRoute.setAgency(unknownAgency);
        } else {
            String agencyId = line.getOperatorRef().getRef();
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