package org.opentripplanner.updater.vehicle_rental;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.linking.LinkingDirection;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalService;
import org.opentripplanner.street.model.edge.StreetVehicleRentalLink;
import org.opentripplanner.street.model.edge.VehicleRentalEdge;
import org.opentripplanner.street.model.vertex.VehicleRentalPlaceVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.DataSource;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.UpdaterConstructionException;
import org.opentripplanner.updater.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic vehicle-rental station updater which updates the Graph with vehicle rental stations from
 * one VehicleRentalDataSource.
 */
public class VehicleRentalUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(VehicleRentalUpdater.class);
  private final DataSource<VehicleRentalPlace> source;
  private WriteToGraphCallback saveResultOnGraph;
  Map<FeedScopedId, VehicleRentalPlaceVertex> verticesByStation = new HashMap<>();
  Map<FeedScopedId, DisposableEdgeCollection> tempEdgesByStation = new HashMap<>();
  private VertexLinker linker;

  private VehicleRentalService service;

  public VehicleRentalUpdater(
    VehicleRentalUpdaterParameters parameters,
    DataSource<VehicleRentalPlace> source,
    VertexLinker vertexLinker,
    VehicleRentalService vehicleRentalStationService
  ) throws IllegalArgumentException {
    super(parameters);
    // Configure updater
    LOG.info("Setting up vehicle rental updater.");

    this.source = source;

    // Creation of network linker library will not modify the graph
    this.linker = vertexLinker;

    // Adding a vehicle rental station service needs a graph writer runnable
    this.service = vehicleRentalStationService;

    try {
      // Do any setup if needed
      source.setup();
    } catch (UpdaterConstructionException e) {
      LOG.warn("Unable to setup updater: {}", parameters.configRef(), e);
    }

    if (pollingPeriodSeconds() <= 0) {
      LOG.info("Creating vehicle-rental updater running once only (non-polling): {}", source);
    } else {
      LOG.info(
        "Creating vehicle-rental updater running every {} seconds: {}",
        pollingPeriodSeconds(),
        source
      );
    }
  }

  @Override
  public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
    this.saveResultOnGraph = saveResultOnGraph;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(VehicleRentalUpdater.class).addObj("source", source).toString();
  }

  @Override
  public String getConfigRef() {
    return toString();
  }

  @Override
  protected void runPolling() {
    LOG.debug("Updating vehicle rental stations from {}", source);
    if (!source.update()) {
      LOG.debug("No updates");
      return;
    }
    List<VehicleRentalPlace> stations = source.getUpdates();

    // Create graph writer runnable to apply these stations to the graph
    VehicleRentalGraphWriterRunnable graphWriterRunnable = new VehicleRentalGraphWriterRunnable(
      stations
    );
    saveResultOnGraph.execute(graphWriterRunnable);
  }

  private class VehicleRentalGraphWriterRunnable implements GraphWriterRunnable {

    private final List<VehicleRentalPlace> stations;

    public VehicleRentalGraphWriterRunnable(List<VehicleRentalPlace> stations) {
      this.stations = stations;
    }

    @Override
    public void run(Graph graph, TransitModel transitModel) {
      // Apply stations to graph
      Set<FeedScopedId> stationSet = new HashSet<>();

      /* add any new stations and update vehicle counts for existing stations */
      for (VehicleRentalPlace station : stations) {
        service.addVehicleRentalStation(station);
        stationSet.add(station.getId());
        VehicleRentalPlaceVertex vehicleRentalVertex = verticesByStation.get(station.getId());
        if (vehicleRentalVertex == null) {
          vehicleRentalVertex = new VehicleRentalPlaceVertex(graph, station);
          DisposableEdgeCollection tempEdges = linker.linkVertexForRealTime(
            vehicleRentalVertex,
            new TraverseModeSet(TraverseMode.WALK),
            LinkingDirection.BOTH_WAYS,
            (vertex, streetVertex) ->
              List.of(
                new StreetVehicleRentalLink((VehicleRentalPlaceVertex) vertex, streetVertex),
                new StreetVehicleRentalLink(streetVertex, (VehicleRentalPlaceVertex) vertex)
              )
          );
          if (vehicleRentalVertex.getOutgoing().isEmpty()) {
            // the toString includes the text "Bike rental station"
            LOG.info("VehicleRentalPlace {} is unlinked", vehicleRentalVertex);
          }
          Set<FormFactor> formFactors = Stream
            .concat(
              station.getAvailablePickupFormFactors(false).stream(),
              station.getAvailableDropoffFormFactors(false).stream()
            )
            .collect(Collectors.toSet());
          for (FormFactor formFactor : formFactors) {
            tempEdges.addEdge(new VehicleRentalEdge(vehicleRentalVertex, formFactor));
          }
          verticesByStation.put(station.getId(), vehicleRentalVertex);
          tempEdgesByStation.put(station.getId(), tempEdges);
        } else {
          vehicleRentalVertex.setStation(station);
        }
      }
      /* remove existing stations that were not present in the update */
      List<FeedScopedId> toRemove = new ArrayList<>();
      for (Entry<FeedScopedId, VehicleRentalPlaceVertex> entry : verticesByStation.entrySet()) {
        FeedScopedId station = entry.getKey();
        if (stationSet.contains(station)) continue;
        toRemove.add(station);
        service.removeVehicleRentalStation(station);
      }
      for (FeedScopedId station : toRemove) {
        // post-iteration removal to avoid concurrent modification
        verticesByStation.remove(station);
        tempEdgesByStation.get(station).disposeEdges();
        tempEdgesByStation.remove(station);
      }
    }
  }
}
