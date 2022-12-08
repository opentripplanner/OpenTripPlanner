package org.opentripplanner.graph_builder.module;

import static org.opentripplanner.framework.geometry.SphericalDistanceLibrary.distance;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.edge.PathwayEdge;
import org.opentripplanner.street.model.vertex.ElevatorOffboardVertex;
import org.opentripplanner.street.model.vertex.ElevatorOnboardVertex;
import org.opentripplanner.street.model.vertex.StationElementVertex;
import org.opentripplanner.street.model.vertex.TransitBoardingAreaVertex;
import org.opentripplanner.street.model.vertex.TransitEntranceVertex;
import org.opentripplanner.street.model.vertex.TransitPathwayNodeVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertexBuilder;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.BoardingArea;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.Pathway;
import org.opentripplanner.transit.model.site.PathwayMode;
import org.opentripplanner.transit.model.site.PathwayNode;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StationElement;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddTransitModelEntitiesToGraph {

  private static final Logger LOG = LoggerFactory.getLogger(AddTransitModelEntitiesToGraph.class);

  private final OtpTransitService otpTransitService;

  // Map of all station elements and their vertices in the graph
  private final Map<StationElement<?, ?>, StationElementVertex> stationElementNodes = new HashMap<>();

  private final int subwayAccessTime;

  /**
   * @param subwayAccessTime a positive integer for the extra time to access a subway platform, if
   *                         negative the default value of zero is used.
   */
  private AddTransitModelEntitiesToGraph(
    OtpTransitService otpTransitService,
    int subwayAccessTime
  ) {
    this.otpTransitService = otpTransitService;
    this.subwayAccessTime = Math.max(subwayAccessTime, 0);
  }

  public static void addToGraph(
    OtpTransitService otpTransitService,
    int subwayAccessTime,
    Graph graph,
    TransitModel transitModel
  ) {
    new AddTransitModelEntitiesToGraph(otpTransitService, subwayAccessTime)
      .applyToGraph(graph, transitModel);
  }

  private void applyToGraph(Graph graph, TransitModel transitModel) {
    transitModel.mergeStopModels(otpTransitService.stopModel());

    addStopsToGraphAndGenerateStopVertexes(graph, transitModel);
    addEntrancesToGraph(graph);
    addPathwayNodesToGraph(graph);
    addBoardingAreasToGraph(graph);

    // Although pathways are loaded from GTFS they are street data, so we will put them in the street graph.
    createPathwayEdgesAndAddThemToGraph(graph);
    addFeedInfoToGraph(transitModel);
    addAgenciesToGraph(transitModel);
    addServicesToTransitModel(transitModel);
    addTripPatternsToTransitModel(transitModel);

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
    for (RegularStop stop : otpTransitService.stopModel().listRegularStops()) {
      Set<TransitMode> modes = stopModeMap.get(stop);
      TransitStopVertex stopVertex = new TransitStopVertexBuilder()
        .withStop(stop)
        .withGraph(graph)
        .withModes(modes)
        .build();

      if (modes != null && modes.contains(TransitMode.SUBWAY)) {
        stopVertex.setStreetToStopTime(subwayAccessTime);
      }

      // Add stops to internal index for Pathways to be created from this map
      stationElementNodes.put(stop, stopVertex);
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
        boolean wheelchair = boardingArea.getWheelchairAccessibility() == Accessibility.POSSIBLE;

        PathwayEdge.lowCost(
          boardingAreaVertex,
          platformVertex,
          boardingArea.getId(),
          boardingArea.getName(),
          wheelchair,
          PathwayMode.WALKWAY
        );

        PathwayEdge.lowCost(
          platformVertex,
          boardingAreaVertex,
          boardingArea.getId(),
          boardingArea.getName(),
          wheelchair,
          PathwayMode.WALKWAY
        );
      }
    }
  }

  private void createPathwayEdgesAndAddThemToGraph(Graph graph) {
    for (Pathway pathway : otpTransitService.getAllPathways()) {
      StationElementVertex fromVertex = stationElementNodes.get(pathway.getFromStop());
      StationElementVertex toVertex = stationElementNodes.get(pathway.getToStop());

      if (fromVertex != null && toVertex != null) {
        // Elevator
        if (pathway.getPathwayMode() == PathwayMode.ELEVATOR) {
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
            pathway.isPathwayModeWheelchairAccessible(),
            pathway.getPathwayMode()
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
              pathway.isPathwayModeWheelchairAccessible(),
              pathway.getPathwayMode()
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
    StationElementVertex fromVertex,
    StationElementVertex toVertex
  ) {
    StopLevel fromLevel = getStopLevel(fromVertex);
    StopLevel toLevel = getStopLevel(toVertex);

    double levels = 1;
    if (
      fromLevel.index() != null &&
      toLevel.index() != null &&
      !fromLevel.index().equals(toLevel.index())
    ) {
      levels = Math.abs(fromLevel.index() - toLevel.index());
    }

    ElevatorOffboardVertex fromOffboardVertex = new ElevatorOffboardVertex(
      graph,
      fromVertex.getLabel() + "_" + pathway.getId() + "_offboard",
      fromVertex.getX(),
      fromVertex.getY(),
      fromLevel.name()
    );
    ElevatorOffboardVertex toOffboardVertex = new ElevatorOffboardVertex(
      graph,
      toVertex.getLabel() + "_" + pathway.getId() + "_offboard",
      toVertex.getX(),
      toVertex.getY(),
      toLevel.name()
    );

    PathwayEdge.lowCost(fromVertex, fromOffboardVertex, fromVertex.getName(), PathwayMode.ELEVATOR);
    PathwayEdge.lowCost(toOffboardVertex, toVertex, toVertex.getName(), PathwayMode.ELEVATOR);

    ElevatorOnboardVertex fromOnboardVertex = new ElevatorOnboardVertex(
      graph,
      fromVertex.getLabel() + "_" + pathway.getId() + "_onboard",
      fromVertex.getX(),
      fromVertex.getY(),
      fromLevel.name()
    );
    ElevatorOnboardVertex toOnboardVertex = new ElevatorOnboardVertex(
      graph,
      toVertex.getLabel() + "_" + pathway.getId() + "_onboard",
      toVertex.getX(),
      toVertex.getY(),
      toLevel.name()
    );

    new ElevatorBoardEdge(fromOffboardVertex, fromOnboardVertex);
    new ElevatorAlightEdge(toOnboardVertex, toOffboardVertex, toLevel.name());

    StreetTraversalPermission permission = StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;
    new ElevatorHopEdge(
      fromOnboardVertex,
      toOnboardVertex,
      permission,
      Accessibility.POSSIBLE,
      levels,
      pathway.getTraversalTime()
    );

    if (pathway.isBidirectional()) {
      PathwayEdge.lowCost(
        fromOffboardVertex,
        fromVertex,
        fromVertex.getName(),
        PathwayMode.ELEVATOR
      );
      PathwayEdge.lowCost(toVertex, toOffboardVertex, toVertex.getName(), PathwayMode.ELEVATOR);
      new ElevatorBoardEdge(toOffboardVertex, toOnboardVertex);
      new ElevatorAlightEdge(fromOnboardVertex, fromOffboardVertex, fromLevel.name());
      new ElevatorHopEdge(
        toOnboardVertex,
        fromOnboardVertex,
        permission,
        Accessibility.POSSIBLE,
        levels,
        pathway.getTraversalTime()
      );
    }
  }

  private StopLevel getStopLevel(StationElementVertex vertex) {
    StationElement<?, ?> fromStation = vertex.getStationElement();
    var level = fromStation.level();
    return level != null
      ? new StopLevel(
        NonLocalizedString.ofNullableOrElse(level.name(), fromStation.getName()),
        level.index()
      )
      : new StopLevel(fromStation.getName(), null);
  }

  private void addFeedInfoToGraph(TransitModel transitModel) {
    for (FeedInfo info : otpTransitService.getAllFeedInfos()) {
      transitModel.addFeedInfo(info);
    }
  }

  private void addAgenciesToGraph(TransitModel transitModel) {
    for (Agency agency : otpTransitService.getAllAgencies()) {
      transitModel.addAgency(agency);
    }
  }

  private void addTransfersToGraph(TransitModel transitModel) {
    transitModel.getTransferService().addAll(otpTransitService.getAllTransfers());
  }

  private void addServicesToTransitModel(TransitModel transitModel) {
    /* Assign 0-based numeric codes to all GTFS service IDs. */
    for (FeedScopedId serviceId : otpTransitService.getAllServiceIds()) {
      transitModel.getServiceCodes().put(serviceId, transitModel.getServiceCodes().size());
    }
  }

  private void addTripPatternsToTransitModel(TransitModel transitModel) {
    Collection<TripPattern> tripPatterns = otpTransitService.getTripPatterns();

    /* Loop over all new TripPatterns setting the service codes. */
    for (TripPattern tripPattern : tripPatterns) {
      // TODO this could be more elegant
      tripPattern.getScheduledTimetable().setServiceCodes(transitModel.getServiceCodes());

      // Store the tripPattern in the Graph so it will be serialized and usable in routing.
      transitModel.addTripPattern(tripPattern.getId(), tripPattern);
    }
  }

  private void addFlexTripsToGraph(TransitModel transitModel) {
    for (FlexTrip<?, ?> flexTrip : otpTransitService.getAllFlexTrips()) {
      transitModel.addFlexTrip(flexTrip.getId(), flexTrip);
    }
  }

  private record StopLevel(I18NString name, Double index) {}
}
