package org.opentripplanner.ext.gbfsgeofencing.internal.graphbuilder;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.ext.gbfsgeofencing.parameters.GbfsGeofencingFeedParameters;
import org.opentripplanner.ext.gbfsgeofencing.parameters.GbfsGeofencingParameters;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.gbfs.GbfsFeedLoaderAndMapper;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalRepository;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.street.geofencing.GeofencingZoneApplier;
import org.opentripplanner.street.Scope;
import org.opentripplanner.street.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graph builder module that loads GBFS geofencing zones at build time and applies them to street
 * edges. Uses the shared GBFS feed loading infrastructure (v2 and v3) and reuses
 * {@link GeofencingZoneApplier} from the street module to mark boundary vertices.
 *
 * <p>Computed zones and the spatial index are registered on
 * {@link DefaultVehicleRentalRepository} via its build-time setter — the raw zones are persisted
 * via {@code SerializedGraphObject} so the runtime application sees them after deserialization.
 */
public class GbfsGeofencingGraphBuilder implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsGeofencingGraphBuilder.class);

  private final GbfsGeofencingParameters parameters;
  private final Graph graph;
  private final DefaultVehicleRentalRepository rentalRepository;

  public GbfsGeofencingGraphBuilder(
    GbfsGeofencingParameters parameters,
    Graph graph,
    DefaultVehicleRentalRepository rentalRepository
  ) {
    this.parameters = parameters;
    this.graph = graph;
    this.rentalRepository = rentalRepository;
  }

  @Override
  public void buildGraph() {
    LOG.info(
      "Loading GBFS geofencing zones at build time from {} feed(s)",
      parameters.feeds().size()
    );

    List<GeofencingZone> allZones = new ArrayList<>();
    boolean anyFeedAppliesBusinessAreas = false;

    try (var httpClientFactory = new OtpHttpClientFactory()) {
      for (var feedParams : parameters.feeds()) {
        try {
          var zones = loadGeofencingZonesFromFeed(feedParams, httpClientFactory);
          if (!feedParams.applyBusinessAreas()) {
            zones = zones.stream().map(GeofencingZone::withoutBusinessArea).toList();
          } else {
            anyFeedAppliesBusinessAreas = true;
          }
          allZones.addAll(zones);
          LOG.info("Loaded {} geofencing zones from GBFS feed: {}", zones.size(), feedParams.url());
        } catch (Exception e) {
          LOG.error("Failed to load geofencing zones from GBFS feed: {}", feedParams.url(), e);
        }
      }
    }

    if (allZones.isEmpty()) {
      LOG.info("No geofencing zones loaded from any GBFS feeds");
      return;
    }

    var applier = new GeofencingZoneApplier(
      ls -> graph.findEdgesAlongLineStrings(ls, Scope.PERMANENT),
      graph::findEdges,
      anyFeedAppliesBusinessAreas
    );
    var result = applier.applyGeofencingZones(allZones);

    rentalRepository.setGeofencingZoneIndex("gbfs-build-time", result.zoneIndex(), allZones);

    LOG.info(
      "Applied {} geofencing zones with {} boundary vertices at build time",
      allZones.size(),
      result.boundaryVertices().size()
    );
  }

  private List<GeofencingZone> loadGeofencingZonesFromFeed(
    GbfsGeofencingFeedParameters feedParams,
    OtpHttpClientFactory httpClientFactory
  ) throws Exception {
    var loaderAndMapper = GbfsFeedLoaderAndMapper.create(feedParams, httpClientFactory);

    if (!loaderAndMapper.update()) {
      LOG.warn("Failed to update GBFS feed: {}", feedParams.url());
      return List.of();
    }

    // getUpdated() must be called to trigger geofencing zone mapping internally
    loaderAndMapper.getUpdated();

    return loaderAndMapper.getGeofencingZones();
  }
}
