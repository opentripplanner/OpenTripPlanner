package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.PresentationStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * Maps NeTEx line to OTP Route.
 */
class RouteMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RouteMapper.class);

    private final HexBinaryAdapter hexBinaryAdapter = new HexBinaryAdapter();
    private final TransportModeMapper transportModeMapper = new TransportModeMapper();

    private final OtpTransitServiceBuilder transitBuilder;

    private final NetexImportDataIndex netexIndex;

    private final AgencyMapper agencyMapper;

    RouteMapper(
            OtpTransitServiceBuilder transitBuilder,
            NetexImportDataIndex netexIndex,
            String timeZone
    ) {
        this.transitBuilder = transitBuilder;
        this.netexIndex = netexIndex;
        this.agencyMapper = new AgencyMapper(timeZone);
    }

    org.opentripplanner.model.Route mapRoute(
            Line line
    ){
        org.opentripplanner.model.Route otpRoute = new org.opentripplanner.model.Route();

        otpRoute.setAgency(findOrCreateAgency(line));

        otpRoute.setId(FeedScopedIdFactory.createFeedScopedId(line.getId()));
        otpRoute.setLongName(line.getName().getValue());
        otpRoute.setShortName(line.getPublicCode());
        otpRoute.setType(transportModeMapper.getTransportMode(line.getTransportMode(), line.getTransportSubmode()));

        if (line.getPresentation() != null) {
            PresentationStructure presentation = line.getPresentation();
            if (presentation.getColour() != null) {
                otpRoute.setColor(hexBinaryAdapter.marshal(presentation.getColour()));
            }
            if (presentation.getTextColour() != null) {
                otpRoute.setTextColor(hexBinaryAdapter.marshal(presentation.getTextColour()));
            }
        }

        return otpRoute;
    }

    /**
     * Find an agency by mapping the GroupOfLines/Network Authority. If no authority is found
     * a default agency is created and returned.
     */
    private Agency findOrCreateAgency(Line line) {
        String groupRef = line.getRepresentedByGroupRef().getRef();

        Network network = netexIndex.lookupNetworkForLine(groupRef);

        if(network != null) {
            String orgRef = network.getTransportOrganisationRef().getValue().getRef();
            Agency agency = transitBuilder.getAgenciesById().get(orgRef);
            if(agency != null) return agency;
        }

        // No authority found in Network or GroupOfLines.
        // Use the default agency, create if necessary
        LOG.warn("No authority found for " + line.getId());
        Agency agency = agencyMapper.createDefaultAgency();
        EntityById<String, Agency> agenciesById = transitBuilder.getAgenciesById();
        if (!agenciesById.containsKey(agency.getId())) {
            agenciesById.add(agency);
        }
        return agency;
    }
}