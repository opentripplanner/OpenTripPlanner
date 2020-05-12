package org.opentripplanner.graph_builder.module;

import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.BoardingArea;
import org.opentripplanner.model.Entrance;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.GroupOfStations;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.Pathway;
import org.opentripplanner.model.PathwayNode;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.StationElement;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.edgetype.ElevatorAlightEdge;
import org.opentripplanner.routing.edgetype.ElevatorBoardEdge;
import org.opentripplanner.routing.edgetype.ElevatorHopEdge;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.ElevatorOffboardVertex;
import org.opentripplanner.routing.vertextype.ElevatorOnboardVertex;
import org.opentripplanner.routing.vertextype.TransitBoardingAreaVertex;
import org.opentripplanner.routing.vertextype.TransitEntranceVertex;
import org.opentripplanner.routing.vertextype.TransitPathwayNodeVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AddTransitModelEntitiesToGraph {
    private static final Logger LOG = LoggerFactory.getLogger(AddTransitModelEntitiesToGraph.class);


    private GtfsFeedId feedId;

    private final OtpTransitService transitService;

    // Map of all station elements and their vertices in the graph
    private Map<StationElement, Vertex> stationElementNodes = new HashMap<>();

    private final int subwayAccessTime;

    public static void addToGraph(GtfsContext context, Graph graph) {
        new AddTransitModelEntitiesToGraph(context).applyToGraph(graph);
    }

    public static void addToGraph(
            GtfsFeedId feedId, OtpTransitService transitModel, int subwayAccessTime, Graph graph
    ) {
        new AddTransitModelEntitiesToGraph(feedId, transitModel, subwayAccessTime).applyToGraph(graph);
    }

    private AddTransitModelEntitiesToGraph(GtfsContext context) {
        this(context.getFeedId(), context.getTransitService(), 0);
    }

    /**
     * @param subwayAccessTime a positive integer for the extra time to access a subway platform, if negative the
     *                         default value of zero is used.
     */
    private AddTransitModelEntitiesToGraph(GtfsFeedId feedId, OtpTransitService transitModel, int subwayAccessTime) {
        this.feedId = feedId;
        this.transitService = transitModel;
        this.subwayAccessTime = Math.max(subwayAccessTime, 0);
    }

    private void applyToGraph(Graph graph) {
        addStopsToGraphAndGenerateStopVertexes(graph);
        addStationsToGraph(graph);
        addMultiModalStationsToGraph(graph);
        addGroupsOfStationsToGraph(graph);
        addEntrancesToGraph(graph);
        addPathwayNodesToGraph(graph);
        addBoardingAreasToGraph(graph);

        // Although pathways are loaded from GTFS they are street data, so we will put them in the street graph.
        createPathwayEdgesAndAddThemToGraph(graph);
        addFeedInfoToGraph(graph);
        addAgenciesToGraph(graph);

        /* Interpret the transfers explicitly defined in transfers.txt. */
        addTransfersToGraph(graph);

    }

    private void addStopsToGraphAndGenerateStopVertexes(Graph graph) {
        // Compute the set of modes for each stop based on all the TripPatterns it is part of
        Map<Stop, Set<TransitMode>> stopModeMap = new HashMap<>();

        for (TripPattern pattern : transitService.getTripPatterns()) {
            TransitMode mode = pattern.getMode();
            graph.addTransitMode(mode);
            for (Stop stop : pattern.getStops()) {
                Set<TransitMode> set = stopModeMap.computeIfAbsent(stop, s -> new HashSet<>());
                set.add(mode);
            }
        }

        // Add a vertex representing the stop.
        // It is now possible for these vertices to not be connected to any edges.
        for (Stop stop : transitService.getAllStops()) {
            Set<TransitMode> modes = stopModeMap.get(stop);
            TransitStopVertex stopVertex = new TransitStopVertex(graph, stop, modes);
            if (modes != null && modes.contains(TransitMode.SUBWAY)) {
                stopVertex.setStreetToStopTime(subwayAccessTime);
            }

            // Add stops to internal index for Pathways to be created from this map
            stationElementNodes.put(stop, stopVertex);
        }
    }

    private void addStationsToGraph(Graph graph) {
        for (Station station : transitService.getAllStations()) {
            graph.stationById.put(station.getId(), station);
        }
    }

    private void addMultiModalStationsToGraph(Graph graph) {
        for (MultiModalStation multiModalStation : transitService.getAllMultiModalStations()) {
            graph.multiModalStationById.put(multiModalStation.getId(), multiModalStation);
        }
    }

    private void addGroupsOfStationsToGraph(Graph graph) {
        for (GroupOfStations groupOfStations : transitService.getAllGroupsOfStations()) {
            graph.groupOfStationsById.put(groupOfStations.getId(), groupOfStations);
        }
    }

    private void addEntrancesToGraph(Graph graph) {
        for (Entrance entrance : transitService.getAllEntrances()) {
            TransitEntranceVertex entranceVertex = new TransitEntranceVertex(graph, entrance);
            stationElementNodes.put(entrance, entranceVertex);
        }
    }

    private void addPathwayNodesToGraph(Graph graph) {
        for (PathwayNode node : transitService.getAllPathwayNodes()) {
            TransitPathwayNodeVertex nodeVertex = new TransitPathwayNodeVertex(graph, node);
            stationElementNodes.put(node, nodeVertex);
        }
    }

    private void addBoardingAreasToGraph(Graph graph) {
        for (BoardingArea boardingArea : transitService.getAllBoardingAreas()) {
            TransitBoardingAreaVertex boardingAreaVertex = new TransitBoardingAreaVertex(graph, boardingArea);
            stationElementNodes.put(boardingArea, boardingAreaVertex);
            if (boardingArea.getParentStop() != null) {
                new PathwayEdge(
                    boardingAreaVertex,
                    stationElementNodes.get(boardingArea.getParentStop()),
                    boardingArea.getName()
                );

                new PathwayEdge(
                    stationElementNodes.get(boardingArea.getParentStop()),
                    boardingAreaVertex,
                    boardingArea.getName()
                );
            }
        }
    }

    private void createPathwayEdgesAndAddThemToGraph(Graph graph) {
        for (Pathway pathway : transitService.getAllPathways()) {
            Vertex fromVertex = stationElementNodes.get(pathway.getFromStop());
            Vertex toVertex = stationElementNodes.get(pathway.getToStop());

            if (fromVertex != null && toVertex != null) {
                // Elevator
                if (pathway.getPathwayMode() == 5) {
                    createElevatorEdgesAndAddThemToGraph(graph, pathway, fromVertex, toVertex);
                }
                else {
                    new PathwayEdge(
                        fromVertex,
                        toVertex,
                        pathway.getName(),
                        pathway.getTraversalTime(),
                        pathway.getLength(),
                        pathway.getStairCount(),
                        pathway.getSlope(),
                        pathway.isPathwayModeWheelchairAccessible()
                    );
                    if (pathway.isBidirectional()) {
                        new PathwayEdge(
                            toVertex,
                            fromVertex,
                            pathway.getReversedName(),
                            pathway.getTraversalTime(),
                            pathway.getLength(),
                            -1 * pathway.getStairCount(),
                            -1 * pathway.getSlope(),
                            pathway.isPathwayModeWheelchairAccessible()
                        );
                    }
                }
            }
            else {
                if (fromVertex == null) {
                    LOG.warn("The 'fromVertex' is missing for pathway from stop: " + pathway
                        .getFromStop()
                        .getId());
                }
                if (toVertex == null) {
                    LOG.warn("The 'toVertex' is missing for pathway to stop: " + pathway
                        .getToStop()
                        .getId());
                }
            }
        }
    }

    /**
     * Create elevator edges from pathways. As pathway based elevators are not vertices, but edges
     * in the pathway model, we have to model each possible movement as an onboard-offboard pair,
     * instead of having only one set of vertices per level and edges between them.
     */
    private void createElevatorEdgesAndAddThemToGraph(
        Graph graph,
        Pathway pathway,
        Vertex fromVertex,
        Vertex toVertex
    ) {
        StationElement fromStation = fromVertex.getStationElement();
        String fromVertexLevelName = fromStation == null
            ? fromVertex.getName()
            : fromStation.getLevelName();
        Double fromVertexLevelIndex = fromStation == null ? null : fromStation.getLevelIndex();

        StationElement toStation = toVertex.getStationElement();
        String toVertexLevelName = toStation == null
            ? toVertex.getName()
            : toStation.getLevelName();
        Double toVertexLevelIndex = toStation == null ? null : toStation.getLevelIndex();

        double levels = 1;
        if (fromVertexLevelIndex != null && toVertexLevelIndex != null
            && !fromVertexLevelIndex.equals(toVertexLevelIndex)) {
            levels = Math.abs(fromVertexLevelIndex - toVertexLevelIndex);
        }

        ElevatorOffboardVertex fromOffboardVertex = new ElevatorOffboardVertex(
            graph,
            fromVertex.getLabel() + "_" + pathway.getId() + "_offboard",
            fromVertex.getX(),
            fromVertex.getY(),
            fromVertexLevelName
        );
        ElevatorOffboardVertex toOffboardVertex = new ElevatorOffboardVertex(
            graph,
            toVertex.getLabel() + "_" + pathway.getId() + "_offboard",
            toVertex.getX(),
            toVertex.getY(),
            toVertexLevelName
        );

        new PathwayEdge(fromVertex, fromOffboardVertex, fromVertex.getName());
        new PathwayEdge(toOffboardVertex, toVertex, toVertex.getName());

        ElevatorOnboardVertex fromOnboardVertex = new ElevatorOnboardVertex(
            graph,
            fromVertex.getLabel() + "_" + pathway.getId() + "_onboard",
            fromVertex.getX(),
            fromVertex.getY(),
            fromVertexLevelName
        );
        ElevatorOnboardVertex toOnboardVertex = new ElevatorOnboardVertex(
            graph,
            toVertex.getLabel() + "_" + pathway.getId() + "_onboard",
            toVertex.getX(),
            toVertex.getY(),
            toVertexLevelName
        );

        new ElevatorBoardEdge(fromOffboardVertex, fromOnboardVertex);
        new ElevatorAlightEdge(toOnboardVertex, toOffboardVertex, toVertexLevelName);

        StreetTraversalPermission permission = StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;
        new ElevatorHopEdge(
            fromOnboardVertex,
            toOnboardVertex,
            permission,
            levels,
            pathway.getTraversalTime()
        );

        if (pathway.isBidirectional()) {
            new PathwayEdge(fromOffboardVertex, fromVertex, fromVertex.getName());
            new PathwayEdge(toVertex, toOffboardVertex, toVertex.getName());
            new ElevatorBoardEdge(toOffboardVertex, toOnboardVertex);
            new ElevatorAlightEdge(
                fromOnboardVertex,
                fromOffboardVertex,
                fromVertexLevelName
            );
            new ElevatorHopEdge(
                toOnboardVertex,
                fromOnboardVertex,
                permission,
                levels,
                pathway.getTraversalTime()
            );
        }
    }

    private void addFeedInfoToGraph(Graph graph) {
        for (FeedInfo info : transitService.getAllFeedInfos()) {
            graph.addFeedInfo(info);
        }
    }

    private void addAgenciesToGraph(Graph graph) {
        for (Agency agency : transitService.getAllAgencies()) {
            graph.addAgency(feedId.getId(), agency);
        }
    }

    private void addTransfersToGraph(Graph graph) {
        Collection<Transfer> transfers = transitService.getAllTransfers();
        TransferTable transferTable = graph.getTransferTable();
        for (Transfer sourceTransfer : transfers) {
            transferTable.addTransfer(sourceTransfer);
        }
    }

}
