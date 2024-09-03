package org.opentripplanner.graph_builder.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.logging.ProgressTracker;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.ParkAndRideEntranceRemoved;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.LinkingDirection;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingHelper;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetTransitEntranceLink;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitEntranceVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.transit.model.network.CarAccess;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link GraphBuilderModule} plugin that links various
 * objects in the graph to the street network. It should be run after both the transit network and
 * street network are loaded. It links four things: transit stops, transit entrances, bike rental
 * stations, and bike parks. Therefore, it should be run even when there's no GTFS data present to
 * make bike rental services and bike parks usable.
 */
public class StreetLinkerModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(StreetLinkerModule.class);
  private static final TraverseModeSet CAR_ONLY = new TraverseModeSet(TraverseMode.CAR);
  private static final TraverseModeSet WALK_ONLY = new TraverseModeSet(TraverseMode.WALK);
  private final Graph graph;
  private final TransitModel transitModel;
  private final DataImportIssueStore issueStore;
  private final Boolean addExtraEdgesToAreas;

  public StreetLinkerModule(
    Graph graph,
    TransitModel transitModel,
    DataImportIssueStore issueStore,
    boolean addExtraEdgesToAreas
  ) {
    this.graph = graph;
    this.transitModel = transitModel;
    this.issueStore = issueStore;
    this.addExtraEdgesToAreas = addExtraEdgesToAreas;
  }

  @Override
  public void buildGraph() {
    transitModel.index();
    graph.index(transitModel.getStopModel());
    graph.getLinker().setAddExtraEdgesToAreas(this.addExtraEdgesToAreas);

    if (graph.hasStreets) {
      linkTransitStops(graph, transitModel);
      linkTransitEntrances(graph);
      linkVehicleParks(graph, issueStore);
    }

    // Calculates convex hull of a graph which is shown in routerInfo API point
    graph.calculateConvexHull();
  }

  public void linkTransitStops(Graph graph, TransitModel transitModel) {
    List<TransitStopVertex> vertices = graph.getVerticesOfType(TransitStopVertex.class);
    var progress = ProgressTracker.track("Linking transit stops to graph", 5000, vertices.size());
    LOG.info(progress.startMessage());

    Set<StopLocation> stopLocationsUsedForFlexTrips = Set.of();

    if (OTPFeature.FlexRouting.isOn()) {
      stopLocationsUsedForFlexTrips =
        transitModel
          .getAllFlexTrips()
          .stream()
          .flatMap(t -> t.getStops().stream())
          .collect(Collectors.toSet());

      stopLocationsUsedForFlexTrips.addAll(
        stopLocationsUsedForFlexTrips
          .stream()
          .filter(GroupStop.class::isInstance)
          .map(GroupStop.class::cast)
          .flatMap(g -> g.getChildLocations().stream().filter(RegularStop.class::isInstance))
          .toList()
      );
    }

    // The stops that are used by transit capable of transporting cars need to be connected to the road network (e.g. car ferries).
    Set<StopLocation> stopLocationsUsedForCarsAllowedTrips = Set.of();
    stopLocationsUsedForCarsAllowedTrips =
      transitModel
        .getAllTripPatterns()
        .stream()
        .filter(t ->
          t
            .getScheduledTimetable()
            .getTripTimes()
            .stream()
            .anyMatch(tt -> tt.getTrip().getCarsAllowed() == CarAccess.ALLOWED)
        )
        .flatMap(t -> t.getStops().stream())
        .collect(Collectors.toSet());

    stopLocationsUsedForCarsAllowedTrips.addAll(
      stopLocationsUsedForCarsAllowedTrips
        .stream()
        .filter(GroupStop.class::isInstance)
        .map(GroupStop.class::cast)
        .flatMap(g -> g.getChildLocations().stream().filter(RegularStop.class::isInstance))
        .toList()
    );

    for (TransitStopVertex tStop : vertices) {
      // Stops with pathways do not need to be connected to the street network, since there are explicit entrances defined for that
      if (tStop.hasPathways()) {
        continue;
      }
      // check if stop is already linked, to allow multiple idempotent linking cycles
      if (tStop.isConnectedToGraph()) {
        continue;
      }

      // ordinarily stops only need to be accessible by foot
      StopLinkType linkType = StopLinkType.WALK_ONLY;

      if (
        (
          OTPFeature.FlexRouting.isOn() && stopLocationsUsedForFlexTrips.contains(tStop.getStop())
        ) ||
        stopLocationsUsedForCarsAllowedTrips.contains(tStop.getStop())
      ) {
        linkType = StopLinkType.WALK_AND_CAR;
      }

      linkStopToStreetNetwork(tStop, linkType);

      //noinspection Convert2MethodRef
      progress.step(m -> LOG.info(m));
    }
    LOG.info(progress.completeMessage());
  }

  /**
   * Link a stop to the nearest "relevant" edges.
   * <p>
   * These are mostly walk edges but if a stop is used by a flex pattern it also needs to be
   * car-accessible. Therefore, flex stops are ensured to be connected to the car-accessible
   * edge. This may lead to several links being created.
   */
  private void linkStopToStreetNetwork(TransitStopVertex tStop, StopLinkType linkType) {
    graph
      .getLinker()
      .linkVertexPermanently(
        tStop,
        WALK_ONLY,
        LinkingDirection.BOTH_WAYS,
        (transitVertex, streetVertex) -> {
          var linkEdges = createStopLinkEdges((TransitStopVertex) transitVertex, streetVertex);

          if (linkType == StopLinkType.WALK_AND_CAR && !streetVertex.isConnectedToDriveableEdge()) {
            linkToDriveableEdge(tStop);
          }

          return linkEdges;
        }
      );
  }

  /**
   * If regular stops or group stops are used for flex trips, they also need to be connected to car
   * routable street edges.
   * <p>
   * This does not apply to zones as street vertices store which zones they are part of.
   *
   * @see https://github.com/opentripplanner/OpenTripPlanner/issues/5498
   */
  private void linkToDriveableEdge(TransitStopVertex tStop) {
    graph
      .getLinker()
      .linkVertexPermanently(
        tStop,
        CAR_ONLY,
        LinkingDirection.BOTH_WAYS,
        (transitVertex, streetVertex) ->
          createStopLinkEdges((TransitStopVertex) transitVertex, streetVertex)
      );
  }

  @Nonnull
  private static List<Edge> createStopLinkEdges(
    TransitStopVertex vertex,
    StreetVertex streetVertex
  ) {
    return List.of(
      StreetTransitStopLink.createStreetTransitStopLink(vertex, streetVertex),
      StreetTransitStopLink.createStreetTransitStopLink(streetVertex, vertex)
    );
  }

  private static void linkVehicleParkingWithLinker(
    Graph graph,
    VehicleParkingEntranceVertex vehicleParkingVertex
  ) {
    if (vehicleParkingVertex.isWalkAccessible()) {
      graph
        .getLinker()
        .linkVertexPermanently(
          vehicleParkingVertex,
          new TraverseModeSet(TraverseMode.WALK),
          LinkingDirection.BOTH_WAYS,
          (vertex, streetVertex) ->
            List.of(
              StreetVehicleParkingLink.createStreetVehicleParkingLink(
                (VehicleParkingEntranceVertex) vertex,
                streetVertex
              ),
              StreetVehicleParkingLink.createStreetVehicleParkingLink(
                streetVertex,
                (VehicleParkingEntranceVertex) vertex
              )
            )
        );
    }

    if (vehicleParkingVertex.isCarAccessible()) {
      graph
        .getLinker()
        .linkVertexPermanently(
          vehicleParkingVertex,
          new TraverseModeSet(TraverseMode.CAR),
          LinkingDirection.BOTH_WAYS,
          (vertex, streetVertex) ->
            List.of(
              StreetVehicleParkingLink.createStreetVehicleParkingLink(
                (VehicleParkingEntranceVertex) vertex,
                streetVertex
              ),
              StreetVehicleParkingLink.createStreetVehicleParkingLink(
                streetVertex,
                (VehicleParkingEntranceVertex) vertex
              )
            )
        );
    }
  }

  private void linkTransitEntrances(Graph graph) {
    LOG.info("Linking transit entrances to graph...");
    for (TransitEntranceVertex tEntrance : graph.getVerticesOfType(TransitEntranceVertex.class)) {
      graph
        .getLinker()
        .linkVertexPermanently(
          tEntrance,
          new TraverseModeSet(TraverseMode.WALK),
          LinkingDirection.BOTH_WAYS,
          (vertex, streetVertex) ->
            List.of(
              StreetTransitEntranceLink.createStreetTransitEntranceLink(
                (TransitEntranceVertex) vertex,
                streetVertex
              ),
              StreetTransitEntranceLink.createStreetTransitEntranceLink(
                streetVertex,
                (TransitEntranceVertex) vertex
              )
            )
        );
    }
  }

  private void linkVehicleParks(Graph graph, DataImportIssueStore issueStore) {
    LOG.info("Linking vehicle parks to graph...");
    List<VehicleParking> vehicleParkingToRemove = new ArrayList<>();
    for (VehicleParkingEntranceVertex vehicleParkingEntranceVertex : graph.getVerticesOfType(
      VehicleParkingEntranceVertex.class
    )) {
      if (vehicleParkingEntranceVertex.isLinkedToGraph()) {
        continue;
      }

      if (vehicleParkingEntranceVertex.getParkingEntrance().getVertex() == null) {
        linkVehicleParkingWithLinker(graph, vehicleParkingEntranceVertex);
        continue;
      }

      if (graph.containsVertex(vehicleParkingEntranceVertex.getParkingEntrance().getVertex())) {
        VehicleParkingHelper.linkToGraph(vehicleParkingEntranceVertex);
        continue;
      }

      issueStore.add(
        new ParkAndRideEntranceRemoved(vehicleParkingEntranceVertex.getParkingEntrance())
      );
      var vehicleParking = removeVehicleParkingEntranceVertexFromGraph(
        vehicleParkingEntranceVertex,
        graph
      );
      if (vehicleParking != null) {
        vehicleParkingToRemove.add(vehicleParking);
      }
    }
    if (!vehicleParkingToRemove.isEmpty()) {
      var vehicleParkingService = graph.getVehicleParkingService();
      vehicleParkingService.updateVehicleParking(List.of(), vehicleParkingToRemove);
    }
  }

  /**
   * Removes vehicle parking entrance vertex from graph.
   *
   * @return vehicle parking for removal if the removed entrance was its only entrance.
   */
  private VehicleParking removeVehicleParkingEntranceVertexFromGraph(
    VehicleParkingEntranceVertex vehicleParkingEntranceVertex,
    Graph graph
  ) {
    var vehicleParkingEdge = vehicleParkingEntranceVertex
      .getOutgoing()
      .stream()
      .filter(VehicleParkingEdge.class::isInstance)
      .map(VehicleParkingEdge.class::cast)
      .findFirst()
      .orElseThrow(() ->
        new IllegalStateException(
          "VehicleParkingEdge missing from vertex: " + vehicleParkingEntranceVertex
        )
      );

    var entrance = vehicleParkingEntranceVertex.getParkingEntrance();

    var vehicleParking = vehicleParkingEdge.getVehicleParking();

    boolean removeVehicleParking =
      vehicleParking.getEntrances().size() == 1 &&
      vehicleParking.getEntrances().get(0).equals(entrance);

    vehicleParkingEntranceVertex.getIncoming().forEach(graph::removeEdge);
    vehicleParkingEntranceVertex.getOutgoing().forEach(graph::removeEdge);
    graph.remove(vehicleParkingEntranceVertex);

    if (removeVehicleParking) {
      return vehicleParking;
    } else {
      vehicleParking.getEntrances().remove(entrance);
      return null;
    }
  }

  private enum StopLinkType {
    /**
     * Only ensure that the link leads to a walkable edge.
     * (The same edge may also be drivable but this is not guaranteed.)
     */
    WALK_ONLY,
    /**
     * Make sure that the stop is linked to an edge each that is walkable and drivable.
     * This may lead to several links being created.
     */
    WALK_AND_CAR,
  }
}
