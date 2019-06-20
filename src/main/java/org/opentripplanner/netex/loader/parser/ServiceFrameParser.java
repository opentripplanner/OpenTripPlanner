package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.netex.loader.util.ReadOnlyHierarchicalVersionMapById;
import org.rutebanken.netex.model.Authority;
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
import org.rutebanken.netex.model.ServiceFrame;
import org.rutebanken.netex.model.StopAssignmentsInFrame_RelStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class ServiceFrameParser {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFrameParser.class);

    private final ReadOnlyHierarchicalVersionMapById<Quay> quayById;

    private final HierarchicalMapById<Network> networkById;

    private final HierarchicalMapById<GroupOfLines> groupOfLinesById;

    private final HierarchicalMapById<Authority> authorityById;

    private final Collection<Route> routes = new ArrayList<>();

    private final Collection<Line> lines = new ArrayList<>();

    private final Map<String, Network> networkByLineId = new HashMap<>();

    private final Map<String, GroupOfLines> groupOfLinesByLineId = new HashMap<>();

    private final Collection<JourneyPattern> journeyPatterns = new ArrayList<>();

    private final Collection<DestinationDisplay> destinationDisplays = new ArrayList<>();

    private final Map<String, Authority> authorityByGroupOfLinesId = new HashMap<>();

    private final Map<String, String> quayIdByStopPointRef = new HashMap<>();

    ServiceFrameParser(
            ReadOnlyHierarchicalVersionMapById<Quay> quayById,
            HierarchicalMapById<Authority> authorityById,
            HierarchicalMapById<Network> networkById,
            HierarchicalMapById<GroupOfLines> groupOfLinesById
    ) {
        this.quayById = quayById;
        this.authorityById = authorityById;
        this.networkById = networkById;
        this.groupOfLinesById = groupOfLinesById;
    }

    void parse(ServiceFrame sf) {
        parseStopAssignments(sf.getStopAssignments());
        parseRoutes(sf.getRoutes());
        parseNetwork(sf.getNetwork());
        parseLines(sf.getLines());
        parseJourneyPatterns(sf.getJourneyPatterns());
        parseDestinationDisplays(sf.getDestinationDisplays());
    }

    void setResultOnIndex(NetexImportDataIndex index) {
        index.routeById.addAll(routes);
        index.lineById.addAll(lines);
        index.networkByLineId.addAll(networkByLineId);
        index.groupOfLinesByLineId.addAll(groupOfLinesByLineId);
        index.journeyPatternsById.addAll(journeyPatterns);
        index.destinationDisplayById.addAll(destinationDisplays);
        index.authoritiesByGroupOfLinesId.addAll(authorityByGroupOfLinesId);
        index.quayIdByStopPointRef.addAll((quayIdByStopPointRef));
    }

    private void parseStopAssignments(StopAssignmentsInFrame_RelStructure stopAssignments) {
        if (stopAssignments == null) return;

        for (JAXBElement stopAssignment : stopAssignments.getStopAssignment()) {
            if (stopAssignment.getValue() instanceof PassengerStopAssignment) {
                PassengerStopAssignment assignment = (PassengerStopAssignment) stopAssignment
                        .getValue();
                String quayRef = assignment.getQuayRef().getRef();
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

        networkById.add(network);

        // TODO OTP2 - Add this responsibility to Index

        String orgRef = network.getTransportOrganisationRef().getValue().getRef();
        Authority authority = authorityById.lookup(orgRef);


        GroupsOfLinesInFrame_RelStructure groupsOfLines = network.getGroupsOfLines();

        if (groupsOfLines != null) {
            Collection<GroupOfLines> groupOfLines = groupsOfLines.getGroupOfLines();
            for (GroupOfLines group : groupOfLines) {
                groupOfLinesById.add(group);
                if (authority != null) {
                    authorityByGroupOfLinesId.put(group.getId(), authority);
                }
            }
        }
    }

    private void parseLines(LinesInFrame_RelStructure lines) {
        if (lines == null) return;

        for (JAXBElement element : lines.getLine_()) {
            if (element.getValue() instanceof Line) {
                Line line = (Line) element.getValue();
                this.lines.add(line);

                String groupRef = line.getRepresentedByGroupRef().getRef();
                Network network = networkById.lookup(groupRef);

                if (network != null) {
                    networkByLineId.put(line.getId(), network);
                } else {
                    GroupOfLines groupOfLines = groupOfLinesById.lookup(groupRef);
                    if (groupOfLines != null) {
                        groupOfLinesByLineId.put(line.getId(), groupOfLines);
                    }
                }
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
