package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.util.HierarchicalMap;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.netex.loader.util.HierarchicalMultimapById;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.Collection;

class ServiceFrameParser {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFrameParser.class);

    private final HierarchicalMultimapById<Quay> quayById;

    private final HierarchicalMapById<Network> networkById;

    private final HierarchicalMapById<GroupOfLines> groupOfLinesById;

    private final HierarchicalMapById<Authority> authorityById;

    private final HierarchicalMapById<Route> routeById = new HierarchicalMapById<>();

    private final HierarchicalMap<String, Authority> authorityByNetworkId = new HierarchicalMap<>();

    private final HierarchicalMapById<Line> lineById = new HierarchicalMapById<>();

    private final HierarchicalMap<String, Network> networkByLineId = new HierarchicalMap<>();

    private final HierarchicalMap<String, GroupOfLines> groupOfLinesByLineId = new HierarchicalMap<>();

    private final HierarchicalMapById<JourneyPattern> journeyPatternById = new HierarchicalMapById<>();

    private final HierarchicalMapById<DestinationDisplay> destinationDisplayById = new HierarchicalMapById<>();

    private final HierarchicalMap<String, Authority> authorityByGroupOfLinesId = new HierarchicalMap<>();

    private final HierarchicalMap<String, String> quayIdByStopPointRef = new HierarchicalMap<>();

    ServiceFrameParser(
            HierarchicalMultimapById<Quay> quayById,
            HierarchicalMapById<Authority> authorityById,
            HierarchicalMapById<Network> networkById,
            HierarchicalMapById<GroupOfLines> groupOfLinesById
    )  {
        this.quayById = quayById;
        this.authorityById = authorityById;
        this.networkById = networkById;
        this.groupOfLinesById = groupOfLinesById;
    }

    void parse(ServiceFrame sf) {
            //stop assignments
            StopAssignmentsInFrame_RelStructure stopAssignments = sf.getStopAssignments();
            if (stopAssignments != null) {
                Collection<JAXBElement<? extends StopAssignment_VersionStructure>> assignments = stopAssignments
                        .getStopAssignment();
                for (JAXBElement assignment : assignments) {
                    if (assignment.getValue() instanceof PassengerStopAssignment) {
                        PassengerStopAssignment passengerStopAssignment =
                                (PassengerStopAssignment) assignment.getValue();
                        String quayRef = passengerStopAssignment.getQuayRef().getRef();
                        Quay quay = quayById.lookupLastVersionById(quayRef);
                        if (quay != null) {
                            quayIdByStopPointRef.add(
                                    passengerStopAssignment.getScheduledStopPointRef().getValue().getRef(),
                                    quay.getId());
                        } else {
                            LOG.warn("Quay " + quayRef + " not found in stop place file.");
                        }
                    }
                }
            }

            //routes
            RoutesInFrame_RelStructure routes = sf.getRoutes();
            if (routes != null) {
                Collection<JAXBElement<? extends LinkSequence_VersionStructure>> route_ = routes
                        .getRoute_();
                for (JAXBElement element : route_) {
                    if (element.getValue() instanceof Route) {
                        Route route = (Route) element.getValue();
                        routeById.add(route);
                    }
                }
            }

            //network
            Network network = sf.getNetwork();
            if(network != null){
                networkById.add(network);

                String orgRef = network.getTransportOrganisationRef().getValue().getRef();

                Authority authority = authorityById.lookup(orgRef);

                if (authority != null) {
                    authorityByNetworkId.add(network.getId(), authority);
                }

                if (network.getGroupsOfLines() != null) {
                    GroupsOfLinesInFrame_RelStructure groupsOfLines = network.getGroupsOfLines();
                    Collection<GroupOfLines> groupOfLines = groupsOfLines.getGroupOfLines();
                    for (GroupOfLines group : groupOfLines) {
                        groupOfLinesById.add(group);
                        if (authority != null) {
                            authorityByGroupOfLinesId.add(group.getId(),
                                    authority);
                        }
                    }
                }
            }

            //lines
            LinesInFrame_RelStructure lines = sf.getLines();
            if(lines != null){
                Collection<JAXBElement<? extends DataManagedObjectStructure>> line_ = lines.getLine_();
                for (JAXBElement element : line_) {
                    if (element.getValue() instanceof Line) {
                        Line line = (Line) element.getValue();
                        lineById.add(line);
                        String groupRef = line.getRepresentedByGroupRef().getRef();
                        Network network2 = networkById.lookup(groupRef);
                        if (network2 != null) {
                            networkByLineId.add(line.getId(), network2);
                        }
                        else {
                            GroupOfLines groupOfLines = groupOfLinesById
                                    .lookup(groupRef);
                            if (groupOfLines != null) {
                                groupOfLinesByLineId.add(line.getId(),
                                        groupOfLines);
                            }
                        }
                    }
                }
            }

            //journeyPatterns
            JourneyPatternsInFrame_RelStructure journeyPatterns = sf.getJourneyPatterns();
            if (journeyPatterns != null) {
                Collection<JAXBElement<?>> journeyPattern_orJourneyPatternView = journeyPatterns
                        .getJourneyPattern_OrJourneyPatternView();
                for (JAXBElement pattern : journeyPattern_orJourneyPatternView) {
                    if (pattern.getValue() instanceof JourneyPattern) {
                        journeyPatternById.add(
                                (JourneyPattern) pattern.getValue());
                    }
                }
            }

            //destinationDisplays
            if (sf.getDestinationDisplays() != null) {
                for (DestinationDisplay destinationDisplay : sf.getDestinationDisplays().getDestinationDisplay()) {
                    destinationDisplayById.add(destinationDisplay);
                }
            }
    }

    HierarchicalMapById<Route> getRouteById() {
        return routeById;
    }

    HierarchicalMap<String, Authority> getAuthorityByNetworkId() {
        return authorityByNetworkId;
    }

    HierarchicalMapById<Line> getLineById() {
        return lineById;
    }

    HierarchicalMap<String, Network> getNetworkByLineId() {
        return networkByLineId;
    }

    HierarchicalMap<String, GroupOfLines> getGroupOfLinesByLineId() {
        return groupOfLinesByLineId;
    }

    HierarchicalMapById<JourneyPattern> getJourneyPatternById() {
        return journeyPatternById;
    }

    HierarchicalMapById<DestinationDisplay> getDestinationDisplayById() {
        return destinationDisplayById;
    }

    HierarchicalMap<String, Authority> getAuthorityByGroupOfLinesId() {
        return authorityByGroupOfLinesId;
    }

    HierarchicalMap<String, String> getQuayIdByStopPointRef() {
        return quayIdByStopPointRef;
    }

    HierarchicalMapById<GroupOfLines> getGroupOfLinesById() { return groupOfLinesById; }
}
