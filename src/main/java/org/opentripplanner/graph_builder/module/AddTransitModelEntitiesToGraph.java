package org.opentripplanner.graph_builder.module;

import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.GroupOfStations;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.Pathway;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AddTransitModelEntitiesToGraph {
    private static final Logger LOG = LoggerFactory.getLogger(AddTransitModelEntitiesToGraph.class);


    private GtfsFeedId feedId;

    private final OtpTransitService transitService;

    // Map of stops and their vertices in the graph
    private Map<Stop, TransitStopVertex> stopNodes = new HashMap<>();

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

        // Although pathways are loaded from GTFS they are street data, so we will put them in the street graph.
        createPathwayEdgesAndAddThemToGraph(graph);
        addFeedInfoToGraph(graph);
        addAgenciesToGraph(graph);

        /* Interpret the transfers explicitly defined in transfers.txt. */
        addTransfersToGraph(graph);

    }

    private void addStopsToGraphAndGenerateStopVertexes(Graph graph) {
        // Compute the set of modes for each stop based on all the TripPatterns it is part of
        Map<Stop, TraverseModeSet> stopModeMap = new HashMap<>();

        for (TripPattern pattern : transitService.getTripPatterns()) {
            TraverseMode mode = pattern.mode;
            graph.addTransitMode(mode);
            for (Stop stop : pattern.getStops()) {
                TraverseModeSet set = stopModeMap.computeIfAbsent(stop, s -> new TraverseModeSet());
                set.setMode(mode, true);
            }
        }

        // Add a vertex representing the stop.
        // It is now possible for these vertices to not be connected to any edges.
        for (Stop stop : transitService.getAllStops()) {
            TraverseModeSet modes = stopModeMap.get(stop);
            TransitStopVertex stopVertex = new TransitStopVertex(graph, stop, modes);
            if (modes != null && modes.contains(TraverseMode.SUBWAY)) {
                stopVertex.setStreetToStopTime(subwayAccessTime);
            }

            // Add stops to internal index for Pathways to be created from this map
            // TODO rename
            stopNodes.put(stop, stopVertex);
        }
    }

    private void addStationsToGraph(Graph graph) {
        /* Store parent stops in graph, even if not linked*/
        for (Station station : transitService.getAllStations()) {
            graph.stationById.put(station.getId(), station);
        }
    }

    private void addMultiModalStationsToGraph(Graph graph) {
        /* Store parent stops in graph, even if not linked*/
        for (MultiModalStation multiModalStation : transitService.getAllMultiModalStations()) {
            graph.multiModalStationById.put(multiModalStation.getId(), multiModalStation);
        }
    }

    private void addGroupsOfStationsToGraph(Graph graph) {
        /* Store parent stops in graph, even if not linked*/
        for (GroupOfStations groupOfStations : transitService.getAllGroupsOfStations()) {
            graph.groupOfStationsById.put(groupOfStations.getId(), groupOfStations);
        }
    }

    private void createPathwayEdgesAndAddThemToGraph(Graph graph) {
        for (Pathway pathway : transitService.getAllPathways()) {
            Vertex fromVertex = stopNodes.get(pathway.getFromStop());
            Vertex toVertex = stopNodes.get(pathway.getToStop());

            if(fromVertex != null && toVertex != null) {
                if (pathway.isWheelchairTraversalTimeSet()) {
                    new PathwayEdge(fromVertex, toVertex, pathway.getTraversalTime(), pathway.getWheelchairTraversalTime());
                } else {
                    new PathwayEdge(fromVertex, toVertex, pathway.getTraversalTime());
                }
            }
            else {
                if(fromVertex == null) {
                    LOG.warn("The 'fromVertex' is missing for pathway from stop: " + pathway.getFromStop().getId());
                }
                if(toVertex == null) {
                    LOG.warn("The 'toVertex' is missing for pathway to stop: " + pathway.getToStop().getId());
                }
            }
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
