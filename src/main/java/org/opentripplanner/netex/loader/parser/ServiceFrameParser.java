package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.loader.util.ReadOnlyHierarchicalVersionMapById;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.DestinationDisplaysInFrame_RelStructure;
import org.rutebanken.netex.model.GroupOfLines;
import org.rutebanken.netex.model.GroupsOfLinesInFrame_RelStructure;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.JourneyPatternsInFrame_RelStructure;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.LinesInFrame_RelStructure;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.RoutesInFrame_RelStructure;
import org.rutebanken.netex.model.Service_VersionFrameStructure;
import org.rutebanken.netex.model.StopAssignmentsInFrame_RelStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class ServiceFrameParser extends NetexParser<Service_VersionFrameStructure> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFrameParser.class);

    private final ReadOnlyHierarchicalVersionMapById<Quay> quayById;

    private final Collection<Network> networks = new ArrayList<>();

    private final Collection<GroupOfLines> groupOfLines = new ArrayList<>();

    private final Collection<Route> routes = new ArrayList<>();

    private final Collection<Line> lines = new ArrayList<>();

    private final Map<String, String> networkIdByGroupOfLineId = new HashMap<>();

    private final Collection<JourneyPattern> journeyPatterns = new ArrayList<>();

    private final Collection<DestinationDisplay> destinationDisplays = new ArrayList<>();

    private final Map<String, String> quayIdByStopPointRef = new HashMap<>();

    ServiceFrameParser(ReadOnlyHierarchicalVersionMapById<Quay> quayById) {
        this.quayById = quayById;
    }

    @Override
    void parse(Service_VersionFrameStructure frame) {
        parseStopAssignments(frame.getStopAssignments());
        parseRoutes(frame.getRoutes());
        parseNetwork(frame.getNetwork());
        parseLines(frame.getLines());
        parseJourneyPatterns(frame.getJourneyPatterns());
        parseDestinationDisplays(frame.getDestinationDisplays());

        // Keep list sorted alphabetically
        logUnknownElement(LOG, frame.getAdditionalNetworks());
        logUnknownElement(LOG, frame.getCommonSections());
        logUnknownElement(LOG, frame.getConnections());
        logUnknownElement(LOG, frame.getDirections());
        logUnknownElement(LOG, frame.getDisplayAssignments());
        logUnknownElement(LOG, frame.getFlexibleLinkProperties());
        logUnknownElement(LOG, frame.getFlexiblePointProperties());
        logUnknownElement(LOG, frame.getGeneralSections());
        logUnknownElement(LOG, frame.getGroupsOfLines());
        logUnknownElement(LOG, frame.getGroupsOfLinks());
        logUnknownElement(LOG, frame.getGroupsOfPoints());
        logUnknownElement(LOG, frame.getLineNetworks());
        logUnknownElement(LOG, frame.getLogicalDisplays());
        logUnknownElement(LOG, frame.getNotices());
        logUnknownElement(LOG, frame.getNoticeAssignments());
        logUnknownElement(LOG, frame.getPassengerInformationEquipments());
        logUnknownElement(LOG, frame.getRouteLinks());
        logUnknownElement(LOG, frame.getRoutePoints());
        logUnknownElement(LOG, frame.getRoutingConstraintZones());
        logUnknownElement(LOG, frame.getScheduledStopPoints());
        logUnknownElement(LOG, frame.getServiceExclusions());
        logUnknownElement(LOG, frame.getServiceLinks());
        logUnknownElement(LOG, frame.getServicePatterns());
        logUnknownElement(LOG, frame.getStopAreas());
        logUnknownElement(LOG, frame.getTariffZones());
        logUnknownElement(LOG, frame.getTimeDemandTypes());
        logUnknownElement(LOG, frame.getTimeDemandTypeAssignments());
        logUnknownElement(LOG, frame.getTimingPoints());
        logUnknownElement(LOG, frame.getTimingLinks());
        logUnknownElement(LOG, frame.getTimingLinkGroups());
        logUnknownElement(LOG, frame.getTimingPatterns());
        logUnknownElement(LOG, frame.getTransferRestrictions());

        checkCommonProperties(LOG, frame);
    }

    @Override
    void setResultOnIndex(NetexImportDataIndex index) {
        // update entities
        index.destinationDisplayById.addAll(destinationDisplays);
        index.groupOfLinesById.addAll(groupOfLines);
        index.journeyPatternsById.addAll(journeyPatterns);
        index.lineById.addAll(lines);
        index.networkById.addAll(networks);
        index.quayIdByStopPointRef.addAll((quayIdByStopPointRef));
        index.routeById.addAll(routes);

        // update references
        index.networkIdByGroupOfLineId.addAll(networkIdByGroupOfLineId);
    }

    private void parseStopAssignments(StopAssignmentsInFrame_RelStructure stopAssignments) {
        if (stopAssignments == null) return;

        for (JAXBElement stopAssignment : stopAssignments.getStopAssignment()) {
            if (stopAssignment.getValue() instanceof PassengerStopAssignment) {
                PassengerStopAssignment assignment = (PassengerStopAssignment) stopAssignment
                        .getValue();
                String quayRef = assignment.getQuayRef().getRef();

                // TODO OTP2 - This check belongs to the mapping or as a separate validation
                // TODO OTP2 - step. The problem is that we do not want to relay on the
                // TODO OTP2 - the order in witch elements are loaded.
                Quay quay = quayById.lookupLastVersionById(quayRef);
                if (quay != null) {
                    quayIdByStopPointRef
                            .put(assignment.getScheduledStopPointRef().getValue()
                                    .getRef(), quay.getId());
                } else {
                    LOG.warn("Quay " + quayRef + " not found in stop place file.");
                }
            }
        }
    }

    private void parseRoutes(RoutesInFrame_RelStructure routes) {
        if (routes == null) return;

        for (JAXBElement element : routes.getRoute_()) {
            if (element.getValue() instanceof Route) {
                Route route = (Route) element.getValue();
                this.routes.add(route);
            }
        }
    }

    private void parseNetwork(Network network) {
        if (network == null) return;

        networks.add(network);

        GroupsOfLinesInFrame_RelStructure groupsOfLines = network.getGroupsOfLines();

        if (groupsOfLines != null) {
            parseGroupOfLines(groupsOfLines.getGroupOfLines(), network);
        }
    }

    private void parseGroupOfLines(Collection<GroupOfLines> groupOfLines, Network network) {
        for (GroupOfLines group : groupOfLines) {
            networkIdByGroupOfLineId.put(network.getId(), group.getId());
        }
    }

    private void parseLines(LinesInFrame_RelStructure lines) {
        if (lines == null) return;

        for (JAXBElement element : lines.getLine_()) {
            if (element.getValue() instanceof Line) {
                this.lines.add((Line) element.getValue());
            }
        }
    }

    private void parseJourneyPatterns(JourneyPatternsInFrame_RelStructure journeyPatterns) {
        if (journeyPatterns == null) return;

        for (JAXBElement pattern : journeyPatterns.getJourneyPattern_OrJourneyPatternView()) {
            if (pattern.getValue() instanceof JourneyPattern) {
                this.journeyPatterns.add((JourneyPattern) pattern.getValue());
            }
        }
    }

    private void parseDestinationDisplays(DestinationDisplaysInFrame_RelStructure destDisplays) {
        if (destDisplays == null) return;

        this.destinationDisplays.addAll(destDisplays.getDestinationDisplay());
    }
}
