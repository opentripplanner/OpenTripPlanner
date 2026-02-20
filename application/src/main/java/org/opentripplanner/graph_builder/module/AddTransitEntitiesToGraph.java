package org.opentripplanner.graph_builder.module;

import static org.opentripplanner.street.geometry.SphericalDistanceLibrary.distance;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.model.TransitDataImport;
import org.opentripplanner.service.streetdetails.StreetDetailsRepository;
import org.opentripplanner.service.streetdetails.model.Level;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.edge.PathwayEdge;
import org.opentripplanner.street.model.vertex.ElevatorHopVertex;
import org.opentripplanner.street.model.vertex.StationElementVertex;
import org.opentripplanner.street.model.vertex.TransitBoardingAreaVertex;
import org.opentripplanner.street.model.vertex.TransitEntranceVertex;
import org.opentripplanner.street.model.vertex.TransitPathwayNodeVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.streetadapter.VertexFactory;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.BoardingArea;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.Pathway;
import org.opentripplanner.transit.model.site.PathwayMode;
import org.opentripplanner.transit.model.site.PathwayNode;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StationElement;
import org.opentripplanner.transit.model.site.StopLevel;
import org.opentripplanner.transit.model.site.StopLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddTransitEntitiesToGraph {

  private static final Logger LOG = LoggerFactory.getLogger(AddTransitEntitiesToGraph.class);

  private final TransitDataImport dataImport;

  private final StreetDetailsRepository streetDetailsRepository;

  // Map of all station elements and their vertices in the graph
  private final Map<StationElement<?, ?>, StationElementVertex> stationElementNodes =
    new HashMap<>();

  private final int subwayAccessTime;
  private final VertexFactory vertexFactory;

  /**
   * @param subwayAccessTime a positive integer for the extra time to access a subway platform, if
   *                         negative the default value of zero is used.
   */
  private AddTransitEntitiesToGraph(
    TransitDataImport dataImport,
    int subwayAccessTime,
    Graph graph,
    StreetDetailsRepository streetDetailsRepository
  ) {
    this.dataImport = dataImport;
    this.subwayAccessTime = Math.max(subwayAccessTime, 0);
    this.vertexFactory = new VertexFactory(graph);
    this.streetDetailsRepository = streetDetailsRepository;
  }

  public static void addToGraph(
    TransitDataImport dataImport,
    int subwayAccessTime,
    Graph graph,
    StreetDetailsRepository streetDetailsRepository
  ) {
    var adder = new AddTransitEntitiesToGraph(
      dataImport,
      subwayAccessTime,
      graph,
      streetDetailsRepository
    );
    adder.applyToGraph();
  }

  private void applyToGraph() {
    addStopsToGraphAndGenerateStopVertexes();
    addEntrancesToGraph();
    addStationCentroidsToGraph();
    addPathwayNodesToGraph();
    addBoardingAreasToGraph();

    // Although pathways are loaded from GTFS they are street data, so we will put them in the
    // street graph.
    createPathwayEdgesAndAddThemToGraph();
  }

  private void addStopsToGraphAndGenerateStopVertexes() {
    // Compute the set of modes for each stop based on all the TripPatterns it is part of
    SetMultimap<StopLocation, TransitMode> stopModeMap = HashMultimap.create();

    for (TripPattern pattern : dataImport.getTripPatterns()) {
      TransitMode mode = pattern.getMode();
      for (var stop : pattern.getStops()) {
        stopModeMap.put(stop, mode);
      }
    }

    // Add a vertex representing the stop.
    // It is now possible for these vertices to not be connected to any edges.
    for (RegularStop stop : dataImport.siteRepository().listRegularStops()) {
      Set<TransitMode> modes = stopModeMap.get(stop);
      var b = TransitStopVertex.of()
        .withId(stop.getId())
        .withPoint(stop.getGeometry())
        .withWheelchairAccessiblity(stop.getWheelchairAccessibility())
        .withIsFerry(modes.contains(TransitMode.FERRY));
      TransitStopVertex stopVertex = vertexFactory.transitStop(b);

      if (modes.contains(TransitMode.SUBWAY)) {
        stopVertex.setStreetToStopTime(subwayAccessTime);
      }

      // Add stops to internal index for Pathways to be created from this map
      stationElementNodes.put(stop, stopVertex);
    }
  }

  private void addEntrancesToGraph() {
    for (Entrance entrance : dataImport.siteRepository().listEntrances()) {
      TransitEntranceVertex entranceVertex = vertexFactory.transitEntrance(entrance);
      stationElementNodes.put(entrance, entranceVertex);
    }
  }

  private void addStationCentroidsToGraph() {
    for (Station station : dataImport.siteRepository().listStations()) {
      if (station.shouldRouteToCentroid()) {
        vertexFactory.stationCentroid(station);
      }
    }
  }

  private void addPathwayNodesToGraph() {
    for (PathwayNode node : dataImport.getAllPathwayNodes()) {
      TransitPathwayNodeVertex nodeVertex = vertexFactory.transitPathwayNode(node);
      stationElementNodes.put(node, nodeVertex);
    }
  }

  private void addBoardingAreasToGraph() {
    for (BoardingArea boardingArea : dataImport.getAllBoardingAreas()) {
      TransitBoardingAreaVertex boardingAreaVertex = vertexFactory.transitBoardingArea(
        boardingArea
      );
      stationElementNodes.put(boardingArea, boardingAreaVertex);
      if (boardingArea.getParentStop() != null) {
        var platformVertex = stationElementNodes.get(boardingArea.getParentStop());
        boolean wheelchair = boardingArea.getWheelchairAccessibility() == Accessibility.POSSIBLE;

        PathwayEdge.createLowCostPathwayEdge(boardingAreaVertex, platformVertex, wheelchair);

        PathwayEdge.createLowCostPathwayEdge(platformVertex, boardingAreaVertex, wheelchair);
      }
    }
  }

  private void createPathwayEdgesAndAddThemToGraph() {
    for (Pathway pathway : dataImport.getAllPathways()) {
      StationElementVertex fromVertex = stationElementNodes.get(pathway.getFromStop());
      StationElementVertex toVertex = stationElementNodes.get(pathway.getToStop());

      if (fromVertex != null && toVertex != null) {
        // Elevator
        if (pathway.getPathwayMode() == PathwayMode.ELEVATOR) {
          createElevatorEdgesAndAddThemToGraph(pathway, fromVertex, toVertex);
        } else {
          // the GTFS spec allows you to define a pathway which has neither traversal time, distance
          // nor steps. This would lead to traversal costs of 0, so we compute the distance from the
          // vertices as fallback.
          double distance = Optional.of(pathway.getLength())
            .filter(l -> l > 0)
            .orElseGet(() -> distance(fromVertex.getCoordinate(), toVertex.getCoordinate()));

          PathwayEdge.createPathwayEdge(
            fromVertex,
            toVertex,
            NonLocalizedString.ofNullable(pathway.getSignpostedAs()),
            pathway.getTraversalTime(),
            distance,
            pathway.getStairCount(),
            pathway.getSlope(),
            pathway.isPathwayModeWheelchairAccessible()
          );
          if (pathway.isBidirectional()) {
            PathwayEdge.createPathwayEdge(
              toVertex,
              fromVertex,
              NonLocalizedString.ofNullable(pathway.getReverseSignpostedAs()),
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
   * the pathway model, we have to model each possible movement as an ElevatorHopVertex-StationElementVertex pair,
   * instead of having only one set of vertices per level and edges between them.
   */
  private void createElevatorEdgesAndAddThemToGraph(
    Pathway pathway,
    StationElementVertex fromVertex,
    StationElementVertex toVertex
  ) {
    StreetTraversalPermission permission = StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;
    int traversalTime = pathway.getTraversalTime();
    StopLevel fromLevel = findStopLevel(fromVertex);
    StopLevel toLevel = findStopLevel(toVertex);
    double levels = 1;
    if (fromLevel != null && toLevel != null && fromLevel.index() != toLevel.index()) {
      levels = Math.abs(fromLevel.index() - toLevel.index());
    }

    ElevatorHopVertex fromOnboardVertex = vertexFactory.elevator(
      fromVertex,
      getElevatorLabel(fromVertex, pathway)
    );
    ElevatorHopVertex toOnboardVertex = vertexFactory.elevator(
      toVertex,
      getElevatorLabel(toVertex, pathway)
    );

    createOneWayElevatorEdges(
      fromVertex,
      toVertex,
      fromOnboardVertex,
      toOnboardVertex,
      fromLevel,
      toLevel,
      permission,
      levels,
      traversalTime
    );
    if (pathway.isBidirectional()) {
      createOneWayElevatorEdges(
        toVertex,
        fromVertex,
        toOnboardVertex,
        fromOnboardVertex,
        toLevel,
        fromLevel,
        permission,
        levels,
        traversalTime
      );
    }
  }

  private void createOneWayElevatorEdges(
    StationElementVertex fromVertex,
    StationElementVertex toVertex,
    ElevatorHopVertex fromOnboardVertex,
    ElevatorHopVertex toOnboardVertex,
    @Nullable StopLevel fromLevel,
    @Nullable StopLevel toLevel,
    StreetTraversalPermission permission,
    double levels,
    int traversalTime
  ) {
    ElevatorBoardEdge elevatorBoardEdge = ElevatorBoardEdge.createElevatorBoardEdge(
      fromVertex,
      fromOnboardVertex
    );
    ElevatorAlightEdge elevatorAlightEdge = ElevatorAlightEdge.createElevatorAlightEdge(
      toOnboardVertex,
      toVertex
    );

    if (fromLevel != null) {
      streetDetailsRepository.addHorizontalEdgeLevelInfo(
        elevatorBoardEdge,
        new Level(fromLevel.index(), fromLevel.name())
      );
    }
    if (toLevel != null) {
      streetDetailsRepository.addHorizontalEdgeLevelInfo(
        elevatorAlightEdge,
        new Level(toLevel.index(), toLevel.name())
      );
    }

    ElevatorHopEdge.createElevatorHopEdge(
      fromOnboardVertex,
      toOnboardVertex,
      permission,
      Accessibility.POSSIBLE,
      levels,
      traversalTime
    );
  }

  private static String getElevatorLabel(StationElementVertex vertex, Pathway pathway) {
    return "%s_%s".formatted(vertex.getLabel(), pathway.getId());
  }

  /**
   * Try to find a stop level. If one can not be found, return null.
   * If a name is not present, default to the index as the name.
   *
   * @return null or StopLevel without any null fields
   */
  @Nullable
  public StopLevel findStopLevel(StationElementVertex vertex) {
    var stop = dataImport.siteRepository().getRegularStop(vertex.getId());
    if (stop == null || stop.level() == null) {
      return null;
    } else {
      var level = stop.level();
      return new StopLevel(
        Objects.requireNonNullElse(level.name(), String.valueOf(level.index())),
        level.index()
      );
    }
  }
}
