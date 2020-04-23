package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.gtfs.mapping.TransitModeMapper;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.loader.NetexImportDataIndexReadOnlyView;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.OperatorRefStructure;
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

    private final FeedScopedIdFactory idFactory;
    private final EntityById<FeedScopedId, Agency> agenciesById;
    private final EntityById<FeedScopedId, Operator> operatorsById;
    private final NetexImportDataIndexReadOnlyView netexIndex;
    private final AuthorityToAgencyMapper authorityMapper;

    RouteMapper(
            FeedScopedIdFactory idFactory,
            EntityById<FeedScopedId, Agency> agenciesById,
            EntityById<FeedScopedId, Operator> operatorsById,
            NetexImportDataIndexReadOnlyView netexIndex,
            String timeZone
    ) {
        this.idFactory = idFactory;
        this.agenciesById = agenciesById;
        this.operatorsById = operatorsById;
        this.netexIndex = netexIndex;
        this.authorityMapper = new AuthorityToAgencyMapper(idFactory, timeZone);
    }

    org.opentripplanner.model.Route mapRoute(Line line){
        org.opentripplanner.model.Route otpRoute = new org.opentripplanner.model.Route();

        otpRoute.setId(idFactory.createId(line.getId()));
        otpRoute.setAgency(findOrCreateAuthority(line));
        otpRoute.setOperator(findOperator(line));
        otpRoute.setLongName(line.getName().getValue());
        otpRoute.setShortName(line.getPublicCode());
        int transportType = transportModeMapper.getTransportMode(
                line.getTransportMode(),
                line.getTransportSubmode()
        );
        otpRoute.setType(transportType);
        otpRoute.setMode(TransitModeMapper.mapMode(transportType));

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
     * a dummy agency is created and returned.
     */
    private Agency findOrCreateAuthority(Line line) {
        String groupRef = line.getRepresentedByGroupRef().getRef();

        // Find authority, first in *GroupOfLines* and then if not found look in *Network*
        Network network = netexIndex.lookupNetworkForLine(groupRef);

        if(network != null) {
            String orgRef = network.getTransportOrganisationRef().getValue().getRef();
            Agency agency = agenciesById.get(idFactory.createId(orgRef));
            if(agency != null) return agency;
        }
        // No authority found in Network or GroupOfLines.
        // Use the dummy agency, create if necessary
        return createOrGetDummyAgency(line);
    }

    private Agency createOrGetDummyAgency(Line line) {
        LOG.warn("No authority found for " + line.getId());

        Agency agency = agenciesById.get(idFactory.createId(authorityMapper.dummyAgencyId()));

        if (agency == null) {
            agency = authorityMapper.createDummyAgency();
            agenciesById.add(agency);
        }
        return agency;
    }

    private Operator findOperator(Line line) {
        OperatorRefStructure opeRef = line.getOperatorRef();

        if(opeRef == null) {
            return null;
        }
        return operatorsById.get(idFactory.createId(opeRef.getRef()));
    }
}