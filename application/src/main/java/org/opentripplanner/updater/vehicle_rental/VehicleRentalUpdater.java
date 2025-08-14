package org.opentripplanner.updater.vehicle_rental;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.RentalRestrictionExtension;
import org.opentripplanner.street.model.edge.LinkingDirection;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.VertexFactory;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.RealTimeUpdateContext;
import org.opentripplanner.updater.spi.PollingGraphUpdater;
import org.opentripplanner.updater.spi.UpdaterConstructionException;
import org.opentripplanner.updater.vehicle_rental.datasources.VehicleRentalDataSource;
import org.opentripplanner.utils.lang.ObjectUtils;
import org.opentripplanner.utils.logging.Throttle;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.time.TimeUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic vehicle-rental station updater which updates the Graph with vehicle rental stations from
 * one VehicleRentalDataSource.
 */
public class VehicleRentalUpdater extends PollingGraphUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(VehicleRentalUpdater.class);

  private final Throttle unlinkedPlaceThrottle;

  private final VehicleRentalDataSource source;
  private final String nameForLogging;

  private Map<StreetEdge, RentalRestrictionExtension> latestModifiedEdges = Map.of();
  private Set<GeofencingZone> latestAppliedGeofencingZones = Set.of();
  private final Map<FeedScopedId, VehicleRentalPlaceVertex> verticesByStation = new HashMap<>();
  private final Map<FeedScopedId, DisposableEdgeCollection> tempEdgesByStation = new HashMap<>();
  private final VertexLinker linker;

  private final VehicleRentalRepository service;

  public VehicleRentalUpdater(
    VehicleRentalUpdaterParameters parameters,
    VehicleRentalDataSource source,
    VertexLinker vertexLinker,
    VehicleRentalRepository repository
  ) throws IllegalArgumentException {
    super(parameters);
    // Configure updater
    LOG.info("Setting up vehicle rental updater for {}.", source);

    this.source = source;
    this.nameForLogging = ObjectUtils.ifNotNull(
      parameters.sourceParameters().network(),
      parameters.sourceParameters().url()
    );
    this.unlinkedPlaceThrottle = Throttle.ofOneSecond();

    // Creation of network linker library will not modify the graph
    this.linker = vertexLinker;

    // Adding a vehicle rental station service needs a graph writer runnable
    this.service = repository;

    try {
      // Do any setup if needed
      source.setup();
    } catch (UpdaterConstructionException e) {
      LOG.warn("Unable to setup updater: {}", nameForLogging, e);
    }

    if (runOnlyOnce()) {
      LOG.info(
        "Creating vehicle-rental updater running once only (non-polling): {}",
        nameForLogging
      );
    } else {
      LOG.info(
        "Creating vehicle-rental updater running every {}: {}",
        DurationUtils.durationToStr(pollingPeriod()),
        nameForLogging
      );
    }
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
  protected void runPolling() throws InterruptedException, ExecutionException {
    LOG.debug("Updating vehicle rental stations from {}", nameForLogging);
    if (!source.update()) {
      LOG.debug("No updates from {}", nameForLogging);
      return;
    }
    List<VehicleRentalPlace> stations = source.getUpdates();
    var geofencingZones = source.getGeofencingZones();

    // Create graph writer runnable to apply these stations to the graph
    VehicleRentalGraphWriterRunnable graphWriterRunnable = new VehicleRentalGraphWriterRunnable(
      stations,
      geofencingZones
    );
    updateGraph(graphWriterRunnable);
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
    public void run(RealTimeUpdateContext context) {
      // Apply stations to graph
      Set<FeedScopedId> stationSet = new HashSet<>();
      var vertexFactory = new VertexFactory(context.graph());

      /* add any new stations and update vehicle counts for existing stations */
      for (VehicleRentalPlace station : stations) {
        service.addVehicleRentalStation(station);
        stationSet.add(station.id());
        VehicleRentalPlaceVertex vehicleRentalVertex = verticesByStation.get(station.id());

        if (vehicleRentalVertex == null) {
          vehicleRentalVertex = vertexFactory.vehicleRentalPlace(station);
          DisposableEdgeCollection tempEdges = linker.linkVertexForRealTime(
            vehicleRentalVertex,
            new TraverseModeSet(TraverseMode.WALK),
            LinkingDirection.BIDIRECTIONAL,
            (vertex, streetVertex) ->
              List.of(
                StreetVehicleRentalLink.createStreetVehicleRentalLink(
                  (VehicleRentalPlaceVertex) vertex,
                  streetVertex
                ),
                StreetVehicleRentalLink.createStreetVehicleRentalLink(
                  streetVertex,
                  (VehicleRentalPlaceVertex) vertex
                )
              )
          );
          if (vehicleRentalVertex.getOutgoing().isEmpty()) {
            // Copy reference to pass into lambda
            var vrv = vehicleRentalVertex;
            unlinkedPlaceThrottle.throttle(() ->
              // the toString includes the text "Bike rental station"
              LOG.warn(
                "VehicleRentalPlace is unlinked for {}: {}  {}",
                nameForLogging,
                vrv,
                unlinkedPlaceThrottle.setupInfo()
              )
            );
          }
          Set<RentalFormFactor> formFactors = Stream.concat(
            station.availablePickupFormFactors(false).stream(),
            station.availableDropoffFormFactors(false).stream()
          ).collect(Collectors.toSet());
          for (RentalFormFactor formFactor : formFactors) {
            tempEdges.addEdge(
              VehicleRentalEdge.createVehicleRentalEdge(vehicleRentalVertex, formFactor)
            );
          }
          verticesByStation.put(station.id(), vehicleRentalVertex);
          tempEdgesByStation.put(station.id(), tempEdges);
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
        LOG.info("Computing geofencing zones for {}", nameForLogging);
        var start = System.currentTimeMillis();

        latestModifiedEdges.forEach(StreetEdge::removeRentalExtension);

        var updater = new GeofencingVertexUpdater(context.graph()::findEdges);
        latestModifiedEdges = updater.applyGeofencingZones(geofencingZones);
        latestAppliedGeofencingZones = geofencingZones;

        var end = System.currentTimeMillis();
        var millis = Duration.ofMillis(end - start);
        LOG.info(
          "Geofencing zones computation took {}. Added extension to {} edges. For {}",
          TimeUtils.durationToStrCompact(millis),
          latestModifiedEdges.size(),
          nameForLogging
        );
      }
    }
  }
}
