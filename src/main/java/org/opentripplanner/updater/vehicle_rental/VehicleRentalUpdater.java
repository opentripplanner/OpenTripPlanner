package org.opentripplanner.updater.vehicle_rental;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.linking.LinkingDirection;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalService;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetVehicleRentalLink;
import org.opentripplanner.street.model.edge.VehicleRentalEdge;
import org.opentripplanner.street.model.vertex.RentalRestrictionExtension;
import org.opentripplanner.street.model.vertex.VehicleRentalPlaceVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.UpdaterConstructionException;
import org.opentripplanner.updater.WriteToGraphCallback;
import org.opentripplanner.updater.vehicle_rental.datasources.VehicleRentalDatasource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic vehicle-rental station updater which updates the Graph with vehicle rental stations from
 * one VehicleRentalDataSource.
 */
public class VehicleRentalUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(VehicleRentalUpdater.class);
  private final VehicleRentalDatasource source;
  private WriteToGraphCallback saveResultOnGraph;

  private Map<StreetEdge, RentalRestrictionExtension> latestModifiedEdges = Map.of();
  private Set<GeofencingZone> latestAppliedGeofencingZones = Set.of();
  Map<FeedScopedId, VehicleRentalPlaceVertex> verticesByStation = new HashMap<>();
  Map<FeedScopedId, DisposableEdgeCollection> tempEdgesByStation = new HashMap<>();
  private final VertexLinker linker;

  private final VehicleRentalService service;

  public VehicleRentalUpdater(
    VehicleRentalUpdaterParameters parameters,
    VehicleRentalDatasource source,
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
    var geofencingZones = source.getGeofencingZones();

    // Create graph writer runnable to apply these stations to the graph
    VehicleRentalGraphWriterRunnable graphWriterRunnable = new VehicleRentalGraphWriterRunnable(
      stations,
      geofencingZones
    );
    saveResultOnGraph.execute(graphWriterRunnable);
  }

  private class VehicleRentalGraphWriterRunnable implements GraphWriterRunnable {

    private final List<VehicleRentalPlace> stations;
    private final Set<GeofencingZone> geofencingZones;

    public VehicleRentalGraphWriterRunnable(
      List<VehicleRentalPlace> stations,
      List<GeofencingZone> geofencingZones
    ) {
      this.stations = stations;
      this.geofencingZones = Set.copyOf(geofencingZones);
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

      // this check relies on the generated equals for the record which also recursively checks that
      // the JTS geometries are equal
      if (!geofencingZones.isEmpty() && !geofencingZones.equals(latestAppliedGeofencingZones)) {
        LOG.info("Computing geofencing zones");
        var start = System.currentTimeMillis();

        latestModifiedEdges.forEach(StreetEdge::removeRentalExtension);

        var updater = new GeofencingVertexUpdater(graph.getStreetIndex()::getEdgesForEnvelope);
        latestModifiedEdges = updater.applyGeofencingZones(geofencingZones);
        latestAppliedGeofencingZones = geofencingZones;

        var end = System.currentTimeMillis();
        var millis = Duration.ofMillis(end - start);
        LOG.info(
          "Geofencing zones computation took {}. Added extension to {} edges.",
          TimeUtils.durationToStrCompact(millis),
          latestModifiedEdges.size()
        );
      }
    }
  }
}
