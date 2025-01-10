package org.opentripplanner.updater.vehicle_parking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.linking.LinkingDirection;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingHelper;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingState;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.RealTimeUpdateContext;
import org.opentripplanner.updater.spi.DataSource;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graph updater that dynamically sets availability information on vehicle parking lots. This
 * updater fetches data from a single {@link DataSource<VehicleParking>}.
 */
public class VehicleParkingUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(VehicleParkingUpdater.class);
  private final Map<VehicleParking, List<VehicleParkingEntranceVertex>> verticesByPark = new HashMap<>();
  private final Map<VehicleParking, List<DisposableEdgeCollection>> tempEdgesByPark = new HashMap<>();
  private final DataSource<VehicleParking> source;
  private final List<VehicleParking> oldVehicleParkings = new ArrayList<>();
  private final VertexLinker linker;

  private final VehicleParkingRepository parkingRepository;

  public VehicleParkingUpdater(
    VehicleParkingUpdaterParameters parameters,
    DataSource<VehicleParking> source,
    VertexLinker vertexLinker,
    VehicleParkingRepository parkingRepository
  ) {
    super(parameters);
    this.source = source;
    // Creation of network linker library will not modify the graph
    this.linker = vertexLinker;
    // Adding a vehicle parking station service needs a graph writer runnable
    this.parkingRepository = parkingRepository;

    LOG.info("Creating vehicle-parking updater running every {}: {}", pollingPeriod(), source);
  }

  @Override
  protected void runPolling() throws InterruptedException, ExecutionException {
    LOG.debug("Updating vehicle parkings from {}", source);
    if (!source.update()) {
      LOG.debug("No updates");
      return;
    }
    List<VehicleParking> vehicleParkings = source.getUpdates();

    // Create graph writer runnable to apply these stations to the graph
    VehicleParkingGraphWriterRunnable graphWriterRunnable = new VehicleParkingGraphWriterRunnable(
      vehicleParkings
    );
    updateGraph(graphWriterRunnable);
  }

  private class VehicleParkingGraphWriterRunnable implements GraphWriterRunnable {

    private final Map<FeedScopedId, VehicleParking> oldVehicleParkingsById;
    private final Set<VehicleParking> updatedVehicleParkings;

    private VehicleParkingGraphWriterRunnable(List<VehicleParking> updatedVehicleParkings) {
      this.oldVehicleParkingsById =
        oldVehicleParkings
          .stream()
          .collect(Collectors.toMap(VehicleParking::getId, Function.identity()));
      this.updatedVehicleParkings = new HashSet<>(updatedVehicleParkings);
    }

    @Override
    public void run(RealTimeUpdateContext context) {
      // Apply stations to graph
      /* Add any new park and update space available for existing parks */
      Set<VehicleParking> toAdd = new HashSet<>();
      Set<VehicleParking> toLink = new HashSet<>();
      Set<VehicleParking> toRemove = new HashSet<>();

      var vehicleParkingHelper = new VehicleParkingHelper(context.graph());

      for (VehicleParking updatedVehicleParking : updatedVehicleParkings) {
        var operational = updatedVehicleParking.getState().equals(VehicleParkingState.OPERATIONAL);
        var alreadyExists = oldVehicleParkings.contains(updatedVehicleParking);

        if (alreadyExists) {
          oldVehicleParkingsById
            .get(updatedVehicleParking.getId())
            .updateAvailability(updatedVehicleParking.getAvailability());
        } else {
          toAdd.add(updatedVehicleParking);
          if (operational) {
            toLink.add(updatedVehicleParking);
          }
        }
      }

      /* Remove existing parks that were not present in the update */
      for (var oldVehicleParking : oldVehicleParkings) {
        if (updatedVehicleParkings.contains(oldVehicleParking)) {
          continue;
        }

        if (verticesByPark.containsKey(oldVehicleParking)) {
          tempEdgesByPark.get(oldVehicleParking).forEach(DisposableEdgeCollection::disposeEdges);
          verticesByPark
            .get(oldVehicleParking)
            .forEach(v -> removeVehicleParkingEdgesFromGraph(v, context.graph()));
          verticesByPark.remove(oldVehicleParking);
        }

        toRemove.add(oldVehicleParking);
      }

      /* Add new parks, after removing, so that there are no duplicate vertices for removed and re-added parks.*/
      for (final VehicleParking updatedVehicleParking : toLink) {
        var vehicleParkingVertices = vehicleParkingHelper.createVehicleParkingVertices(
          updatedVehicleParking
        );
        var disposableEdgeCollectionsForVertex = linkVehicleParkingVertexToStreets(
          vehicleParkingVertices
        );

        VehicleParkingHelper.linkVehicleParkingEntrances(vehicleParkingVertices);

        verticesByPark.put(updatedVehicleParking, vehicleParkingVertices);
        tempEdgesByPark.put(updatedVehicleParking, disposableEdgeCollectionsForVertex);
      }

      parkingRepository.updateVehicleParking(toAdd, toRemove);

      oldVehicleParkings.removeAll(toRemove);
      oldVehicleParkings.addAll(toAdd);
    }

    private List<DisposableEdgeCollection> linkVehicleParkingVertexToStreets(
      List<VehicleParkingEntranceVertex> vehicleParkingVertices
    ) {
      List<DisposableEdgeCollection> disposableEdgeCollectionsForVertex = new ArrayList<>();
      for (var vehicleParkingVertex : vehicleParkingVertices) {
        var disposableEdges = linkVehicleParkingForRealtime(vehicleParkingVertex);
        disposableEdgeCollectionsForVertex.addAll(disposableEdges);

        if (vehicleParkingVertex.getOutgoing().isEmpty()) {
          LOG.info("Vehicle parking {} unlinked", vehicleParkingVertex);
        }
      }
      return disposableEdgeCollectionsForVertex;
    }

    private List<DisposableEdgeCollection> linkVehicleParkingForRealtime(
      VehicleParkingEntranceVertex vehicleParkingEntranceVertex
    ) {
      List<DisposableEdgeCollection> disposableEdgeCollections = new ArrayList<>();
      if (vehicleParkingEntranceVertex.isWalkAccessible()) {
        var disposableWalkEdges = linker.linkVertexForRealTime(
          vehicleParkingEntranceVertex,
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
        disposableEdgeCollections.add(disposableWalkEdges);
      }

      if (vehicleParkingEntranceVertex.isCarAccessible()) {
        var disposableCarEdges = linker.linkVertexForRealTime(
          vehicleParkingEntranceVertex,
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
        disposableEdgeCollections.add(disposableCarEdges);
      }

      return disposableEdgeCollections;
    }

    private void removeVehicleParkingEdgesFromGraph(
      VehicleParkingEntranceVertex entranceVertex,
      Graph graph
    ) {
      entranceVertex
        .getIncoming()
        .stream()
        .filter(VehicleParkingEdge.class::isInstance)
        .forEach(graph::removeEdge);
      entranceVertex
        .getOutgoing()
        .stream()
        .filter(VehicleParkingEdge.class::isInstance)
        .forEach(graph::removeEdge);
      graph.remove(entranceVertex);
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("source", source).toString();
  }
}
