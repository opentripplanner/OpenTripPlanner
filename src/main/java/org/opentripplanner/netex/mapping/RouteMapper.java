package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.loader.util.HierarchicalMap;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.GroupOfLines;
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

    private final HierarchicalMap<String, Network> networkByLineId;

    private final HierarchicalMap<String, GroupOfLines> groupOfLinesByLineId;

    private final NetexImportDataIndex netexIndex;

    private final AgencyMapper agencyMapper;

    RouteMapper(
            OtpTransitServiceBuilder transitBuilder,
            HierarchicalMap<String, Network> networkByLineId,
            HierarchicalMap<String, GroupOfLines> groupOfLinesByLineId,
            NetexImportDataIndex netexIndex,
            String timeZone
    ) {
        this.transitBuilder = transitBuilder;
        this.networkByLineId = networkByLineId;
        this.groupOfLinesByLineId = groupOfLinesByLineId;
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
    private Agency findOrCreateAgency(
            Line line
    ) {
        String lineId = line.getId();
        // Find authority, first in *Network* and then if not found look in *GroupOfLines*
        Network network = networkByLineId.lookup(lineId);
        GroupOfLines groupOfLines = groupOfLinesByLineId.lookup(lineId);
        Authority authority = netexIndex.lookupAuthority(groupOfLines, network);

        if(authority != null) {
            return transitBuilder.getAgenciesById().get(authority.getId());
        }

        // No authority found in Network or GroupOfLines.
        // Use the default agency, create if necessary
        LOG.warn("No authority found for " + lineId);
        Agency agency = agencyMapper.createDefaultAgency();
        EntityById<String, Agency> agenciesById = transitBuilder.getAgenciesById();
        if (!agenciesById.containsKey(agency.getId())) {
            agenciesById.add(agency);
        }
        return agency;
    }
}