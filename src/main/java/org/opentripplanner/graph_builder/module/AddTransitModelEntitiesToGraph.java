package org.opentripplanner.graph_builder.module;

import static org.opentripplanner.common.geometry.SphericalDistanceLibrary.distance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.GroupOfStations;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.Pathway;
import org.opentripplanner.model.TripPattern;
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
import org.opentripplanner.routing.vertextype.TransitStopVertexBuilder;
import org.opentripplanner.transit.model.basic.WheelchairAccessibility;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.BoardingArea;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.PathwayNode;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StationElement;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddTransitModelEntitiesToGraph {

  private static final Logger LOG = LoggerFactory.getLogger(AddTransitModelEntitiesToGraph.class);

  private final GtfsFeedId feedId;

  private final OtpTransitService otpTransitService;

  // Map of all station elements and their vertices in the graph
  private final Map<StationElement, Vertex> stationElementNodes = new HashMap<>();

  private final int subwayAccessTime;

  /**
   * @param subwayAccessTime a positive integer for the extra time to access a subway platform, if
   *                         negative the default value of zero is used.
   */
  private AddTransitModelEntitiesToGraph(
    GtfsFeedId feedId,
    OtpTransitService otpTransitService,
    int subwayAccessTime
  ) {
    this.feedId = feedId;
    this.otpTransitService = otpTransitService;
    this.subwayAccessTime = Math.max(subwayAccessTime, 0);
  }

  public static void addToGraph(
    GtfsFeedId feedId,
    OtpTransitService otpTransitService,
    int subwayAccessTime,
    Graph graph,
    TransitModel transitModel
  ) {
    new AddTransitModelEntitiesToGraph(feedId, otpTransitService, subwayAccessTime)
      .applyToGraph(graph, transitModel);
  }

  private void applyToGraph(Graph graph, TransitModel transitModel) {
    addStopsToGraphAndGenerateStopVertexes(graph, transitModel);
    addStationsToGraph(transitModel);
    addMultiModalStationsToGraph(transitModel);
    addGroupsOfStationsToGraph(transitModel);
    addEntrancesToGraph(graph);
    addPathwayNodesToGraph(graph);
    addBoardingAreasToGraph(graph);

    // Although pathways are loaded from GTFS they are street data, so we will put them in the street graph.
    createPathwayEdgesAndAddThemToGraph(graph);
    if (OTPFeature.FlexRouting.isOn()) {
      addLocationsToGraph(transitModel);
      addLocationGroupsToGraph(transitModel);
    }
    addFeedInfoToGraph(transitModel);
    addAgenciesToGraph(transitModel);

    /* Interpret the transfers explicitly defined in transfers.txt. */
    addTransfersToGraph(transitModel);

    if (OTPFeature.FlexRouting.isOn()) {
      addFlexTripsToGraph(transitModel);
    }
  }

  private void addStopsToGraphAndGenerateStopVertexes(Graph graph, TransitModel transitModel) {
    // Compute the set of modes for each stop based on all the TripPatterns it is part of
    Map<StopLocation, Set<TransitMode>> stopModeMap = new HashMap<>();

    for (TripPattern pattern : otpTransitService.getTripPatterns()) {
      TransitMode mode = pattern.getMode();
      transitModel.addTransitMode(mode);
      for (var stop : pattern.getStops()) {
        Set<TransitMode> set = stopModeMap.computeIfAbsent(stop, s -> new HashSet<>());
        set.add(mode);
      }
    }

    // Add a vertex representing the stop.
    // It is now possible for these vertices to not be connected to any edges.
    for (Stop stop : otpTransitService.getAllStops()) {
      Set<TransitMode> modes = stopModeMap.get(stop);
      TransitStopVertex stopVertex = new TransitStopVertexBuilder()
        .withStop(stop)
        .withGraph(graph)
        .withTransitModel(transitModel)
        .withModes(modes)
        .build();
      if (modes != null && modes.contains(TransitMode.SUBWAY)) {
        stopVertex.setStreetToStopTime(subwayAccessTime);
      }

      // Add stops to internal index for Pathways to be created from this map
      stationElementNodes.put(stop, stopVertex);
    }
  }

  private void addStationsToGraph(TransitModel transitModel) {
    for (Station station : otpTransitService.getAllStations()) {
      transitModel.getStopModel().addStation(station);
    }
  }

  private void addMultiModalStationsToGraph(TransitModel transitModel) {
    for (MultiModalStation multiModalStation : otpTransitService.getAllMultiModalStations()) {
      transitModel.getStopModel().addMultiModalStation(multiModalStation);
    }
  }

  private void addGroupsOfStationsToGraph(TransitModel transitModel) {
    for (GroupOfStations groupOfStation : otpTransitService.getAllGroupsOfStations()) {
      transitModel.getStopModel().addGroupsOfStations(groupOfStation);
    }
  }

  private void addEntrancesToGraph(Graph graph) {
    for (Entrance entrance : otpTransitService.getAllEntrances()) {
      TransitEntranceVertex entranceVertex = new TransitEntranceVertex(graph, entrance);
      stationElementNodes.put(entrance, entranceVertex);
    }
  }

  private void addPathwayNodesToGraph(Graph graph) {
    for (PathwayNode node : otpTransitService.getAllPathwayNodes()) {
      TransitPathwayNodeVertex nodeVertex = new TransitPathwayNodeVertex(graph, node);
      stationElementNodes.put(node, nodeVertex);
    }
  }

  private void addBoardingAreasToGraph(Graph graph) {
    for (BoardingArea boardingArea : otpTransitService.getAllBoardingAreas()) {
      TransitBoardingAreaVertex boardingAreaVertex = new TransitBoardingAreaVertex(
        graph,
        boardingArea
      );
      stationElementNodes.put(boardingArea, boardingAreaVertex);
      if (boardingArea.getParentStop() != null) {
        var platformVertex = stationElementNodes.get(boardingArea.getParentStop());
        boolean wheelchair =
          boardingArea.getWheelchairAccessibility() == WheelchairAccessibility.POSSIBLE;

        PathwayEdge.lowCost(
          boardingAreaVertex,
          platformVertex,
          boardingArea.getId(),
          boardingArea.getName(),
          wheelchair
        );

        PathwayEdge.lowCost(
          platformVertex,
          boardingAreaVertex,
          boardingArea.getId(),
          boardingArea.getName(),
          wheelchair
        );
      }
    }
  }

  private void createPathwayEdgesAndAddThemToGraph(Graph graph) {
    for (Pathway pathway : otpTransitService.getAllPathways()) {
      Vertex fromVertex = stationElementNodes.get(pathway.getFromStop());
      Vertex toVertex = stationElementNodes.get(pathway.getToStop());

      if (fromVertex != null && toVertex != null) {
        // Elevator
        if (pathway.getPathwayMode() == 5) {
          createElevatorEdgesAndAddThemToGraph(graph, pathway, fromVertex, toVertex);
        } else {
          // the GTFS spec allows you to define a pathway which has neither traversal time, distance
          // nor steps. This would lead to traversal costs of 0, so we compute the distance from the
          // vertices as fallback.
          double distance = Optional
            .of(pathway.getLength())
            .filter(l -> l > 0)
            .orElseGet(() -> distance(fromVertex.getCoordinate(), toVertex.getCoordinate()));

          new PathwayEdge(
            fromVertex,
            toVertex,
            pathway.getId(),
            NonLocalizedString.ofNullable(pathway.getName()),
            pathway.getTraversalTime(),
            distance,
            pathway.getStairCount(),
            pathway.getSlope(),
            pathway.isPathwayModeWheelchairAccessible()
          );
          if (pathway.isBidirectional()) {
            new PathwayEdge(
              toVertex,
              fromVertex,
              pathway.getId(),
              NonLocalizedString.ofNullable(pathway.getReversedName()),
              pathway.getTraversalTime(),
              distance,
              -1 * pathway.getStairCount(),
              -1 * pathway.getSlope(),
              pathway.isPathwayModeWheelchairAccessible()
            );
          }
        }
      } else {
        if (fromVertex == null) {
          LOG.warn("The 'fromVertex' is missing for pathway from stop {}", pathway.getFromStop());
        }
        if (toVertex == null) {
          LOG.warn("The 'toVertex' is missing for pathway to stop {}", pathway.getToStop());
        }
      }
    }
  }

  /**
   * Create elevator edges from pathways. As pathway based elevators are not vertices, but edges in
   * the pathway model, we have to model each possible movement as an onboard-offboard pair, instead
   * of having only one set of vertices per level and edges between them.
   */
  private void createElevatorEdgesAndAddThemToGraph(
    Graph graph,
    Pathway pathway,
    Vertex fromVertex,
    Vertex toVertex
  ) {
    StationElement fromStation = fromVertex.getStationElement();
    I18NString fromVertexLevelName = fromStation == null || fromStation.getLevelName() == null
      ? fromVertex.getName()
      : new NonLocalizedString(fromStation.getLevelName());
    Double fromVertexLevelIndex = fromStation == null ? null : fromStation.getLevelIndex();

    StationElement toStation = toVertex.getStationElement();
    I18NString toVertexLevelName = toStation == null || toStation.getLevelName() == null
      ? toVertex.getName()
      : new NonLocalizedString(toStation.getLevelName());
    Double toVertexLevelIndex = toStation == null ? null : toStation.getLevelIndex();

    double levels = 1;
    if (
      fromVertexLevelIndex != null &&
      toVertexLevelIndex != null &&
      !fromVertexLevelIndex.equals(toVertexLevelIndex)
    ) {
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

    PathwayEdge.lowCost(fromVertex, fromOffboardVertex, fromVertex.getName());
    PathwayEdge.lowCost(toOffboardVertex, toVertex, toVertex.getName());

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
      WheelchairAccessibility.POSSIBLE,
      levels,
      pathway.getTraversalTime()
    );

    if (pathway.isBidirectional()) {
      PathwayEdge.lowCost(fromOffboardVertex, fromVertex, fromVertex.getName());
      PathwayEdge.lowCost(toVertex, toOffboardVertex, toVertex.getName());
      new ElevatorBoardEdge(toOffboardVertex, toOnboardVertex);
      new ElevatorAlightEdge(fromOnboardVertex, fromOffboardVertex, fromVertexLevelName);
      new ElevatorHopEdge(
        toOnboardVertex,
        fromOnboardVertex,
        permission,
        WheelchairAccessibility.POSSIBLE,
        levels,
        pathway.getTraversalTime()
      );
    }
  }

  private void addLocationsToGraph(TransitModel transitModel) {
    for (FlexStopLocation flexStopLocation : otpTransitService.getAllLocations()) {
      transitModel.getStopModel().locationsById.put(flexStopLocation.getId(), flexStopLocation);
    }
  }

  private void addLocationGroupsToGraph(TransitModel transitModel) {
    for (FlexLocationGroup flexLocationGroup : otpTransitService.getAllLocationGroups()) {
      transitModel
        .getStopModel()
        .locationGroupsById.put(flexLocationGroup.getId(), flexLocationGroup);
    }
  }

  private void addFeedInfoToGraph(TransitModel transitModel) {
    for (FeedInfo info : otpTransitService.getAllFeedInfos()) {
      transitModel.addFeedInfo(info);
    }
  }

  private void addAgenciesToGraph(TransitModel transitModel) {
    for (Agency agency : otpTransitService.getAllAgencies()) {
      transitModel.addAgency(feedId.getId(), agency);
    }
  }

  private void addTransfersToGraph(TransitModel transitModel) {
    transitModel.getTransferService().addAll(otpTransitService.getAllTransfers());
  }

  private void addFlexTripsToGraph(TransitModel transitModel) {
    for (FlexTrip flexTrip : otpTransitService.getAllFlexTrips()) transitModel.flexTripsById.put(
      flexTrip.getId(),
      flexTrip
    );
  }
}
