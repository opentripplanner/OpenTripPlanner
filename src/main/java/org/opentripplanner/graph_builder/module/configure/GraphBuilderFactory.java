package org.opentripplanner.graph_builder.module.configure;

import static org.opentripplanner.datastore.api.FileType.DEM;
import static org.opentripplanner.datastore.api.FileType.GTFS;
import static org.opentripplanner.datastore.api.FileType.NETEX;
import static org.opentripplanner.datastore.api.FileType.OSM;

import com.google.common.collect.Lists;
import java.io.File;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.dataoverlay.configure.DataOverlayFactory;
import org.opentripplanner.ext.flex.FlexLocationsToStreetEdgesMapper;
import org.opentripplanner.ext.transferanalyzer.DirectTransferAnalyzer;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.DataImportIssuesToHTML;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.DirectTransferGenerator;
import org.opentripplanner.graph_builder.module.GraphCoherencyCheckerModule;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.graph_builder.module.OsmBoardingLocationsModule;
import org.opentripplanner.graph_builder.module.PruneNoThruIslands;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.TimeZoneAdjusterModule;
import org.opentripplanner.graph_builder.module.map.BusRouteStreetMatcher;
import org.opentripplanner.graph_builder.module.ned.DegreeGridNEDTileSource;
import org.opentripplanner.graph_builder.module.ned.ElevationModule;
import org.opentripplanner.graph_builder.module.ned.GeotiffGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.ned.NEDGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.netex.configure.NetexConfig;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.service.TransitModel;

public class GraphBuilderFactory {

  private final BuildConfig config;
  private final Graph graph;
  private final TransitModel transitModel;
  private final GraphBuilderDataSources dataSources;
  private final DataImportIssueStore issueStore;

  /**
   * The elevation data is created by the osmModule and consumed by the ElevationModule,
   * the wrapper ensure these modules are created in the right order.
   */
  private final WriteBeforeRead<Map<Vertex, Double>> elevationData = WriteBeforeRead.of(Map.of());

  GraphBuilderFactory(GraphBuilderFactoryBuilder builder) {
    this.config = builder.config();
    this.graph = builder.graph();
    this.transitModel = builder.transitModel();
    this.dataSources = builder.dataSources();
    this.issueStore = builder.issueStore();
  }

  public static GraphBuilderFactoryBuilder of() {
    return new GraphBuilderFactoryBuilder();
  }

  public TransitModel transitModel() {
    return transitModel;
  }

  public GraphBuilder createGraphBuilder() {
    return new GraphBuilder(graph, transitModel, issueStore);
  }

  public GraphBuilderModule createOpenStreetMapModule() {
    List<OpenStreetMapProvider> providers = Lists.newArrayList();
    for (DataSource osmFile : dataSources.get(OSM)) {
      providers.add(new OpenStreetMapProvider(osmFile, config.osmCacheDataInMem));
    }

    var osmModule = new OpenStreetMapModule(
      providers,
      config.boardingLocationTags,
      graph,
      zoneId(),
      issueStore
    );
    osmModule.customNamer = config.customNamer;
    osmModule.setDefaultWayPropertySetSource(config.osmWayPropertySet);
    osmModule.skipVisibility = !config.areaVisibility;
    osmModule.platformEntriesLinking = config.platformEntriesLinking;
    osmModule.staticBikeParkAndRide = config.staticBikeParkAndRide;
    osmModule.staticParkAndRide = config.staticParkAndRide;
    osmModule.banDiscouragedWalking = config.banDiscouragedWalking;
    osmModule.banDiscouragedBiking = config.banDiscouragedBiking;
    osmModule.maxAreaNodes = config.maxAreaNodes;

    // Fetch the output elevation data, this is feed into the NED graph builder
    this.elevationData.write(osmModule.elevationDataOutput());
    return osmModule;
  }

  public GraphBuilderModule createGtfsModule() {
    List<GtfsBundle> gtfsBundles = Lists.newArrayList();
    for (DataSource gtfsData : dataSources.get(GTFS)) {
      GtfsBundle gtfsBundle = new GtfsBundle((CompositeDataSource) gtfsData);

      if (config.parentStopLinking) {
        gtfsBundle.linkStopsToParentStations = true;
      }
      gtfsBundle.parentStationTransfers = config.stationTransfers;
      gtfsBundle.subwayAccessTime = config.getSubwayAccessTimeSeconds();
      gtfsBundle.setMaxStopToShapeSnapDistance(config.maxStopToShapeSnapDistance);
      gtfsBundles.add(gtfsBundle);
    }
    return new GtfsModule(
      gtfsBundles,
      transitModel,
      graph,
      issueStore,
      config.getTransitServicePeriod(),
      config.fareServiceFactory,
      config.discardMinTransferTimes,
      config.blockBasedInterlining,
      config.maxInterlineDistance
    );
  }

  public GraphBuilderModule createNetexModule() {
    return new NetexConfig(config)
      .createNetexModule(dataSources.get(NETEX), transitModel, graph, issueStore);
  }

  public GraphBuilderModule createTimeZoneAdjusterModule() {
    return new TimeZoneAdjusterModule(transitModel);
  }

  public GraphBuilderModule createBusRouteStreetMatcher() {
    return new BusRouteStreetMatcher(graph, transitModel);
  }

  public GraphBuilderModule createOsmBoardingLocationsModule() {
    return new OsmBoardingLocationsModule(graph);
  }

  public GraphBuilderModule createStreetLinkerModule() {
    return new StreetLinkerModule(graph, transitModel, issueStore, config.areaVisibility);
  }

  public GraphBuilderModule createPruneNoThruIslands() {
    PruneNoThruIslands pruneNoThruIslands = new PruneNoThruIslands(
      graph,
      transitModel,
      issueStore,
      new StreetLinkerModule(graph, transitModel, issueStore, config.areaVisibility)
    );
    pruneNoThruIslands.setPruningThresholdIslandWithoutStops(
      config.pruningThresholdIslandWithoutStops
    );
    pruneNoThruIslands.setPruningThresholdIslandWithStops(config.pruningThresholdIslandWithStops);
    return pruneNoThruIslands;
  }

  public List<GraphBuilderModule> createElevationModules() {
    List<GraphBuilderModule> result = new ArrayList<>();
    List<ElevationGridCoverageFactory> gridCoverageFactories = new ArrayList<>();
    if (config.elevationBucket != null) {
      gridCoverageFactories.add(createNedElevationFactory());
    } else if (dataSources.has(DEM)) {
      gridCoverageFactories.addAll(createDemGeotiffGridCoverageFactories());
    }
    // Refactoring this class, it was made clear that this allows for adding multiple elevation
    // modules to the same graph builder. We do not actually know if this is supported by the
    // ElevationModule class.
    for (ElevationGridCoverageFactory it : gridCoverageFactories) {
      result.add(createElevationModule(it, dataSources.getCacheDirectory()));
    }
    return result;
  }

  public GraphBuilderModule createFlexLocationsToStreetEdgesMapper() {
    return new FlexLocationsToStreetEdgesMapper(graph, transitModel);
  }

  public GraphBuilderModule createDirectTransferGenerator() {
    var maxTransferDuration = Duration.ofSeconds((long) config.maxTransferDurationSeconds);
    return new DirectTransferGenerator(
      graph,
      transitModel,
      issueStore,
      maxTransferDuration,
      config.transferRequests
    );
  }

  public GraphBuilderModule createDirectTransferAnalyzer() {
    return new DirectTransferAnalyzer(
      graph,
      transitModel,
      issueStore,
      config.maxTransferDurationSeconds * new RoutingRequest().walkSpeed
    );
  }

  public GraphBuilderModule createGraphCoherencyCheckerModule() {
    return new GraphCoherencyCheckerModule(graph, issueStore);
  }

  public GraphBuilderModule createDataOverlayFactory() {
    return DataOverlayFactory.create(graph, config.dataOverlay);
  }

  public GraphBuilderModule createDataImportIssuesToHTML() {
    return new DataImportIssuesToHTML(
      issueStore,
      dataSources.getBuildReportDir(),
      config.maxDataImportIssuesPerFile
    );
  }

  /* private methods */

  private ZoneId zoneId() {
    // TODO OTP2: This is probably wrong, fetch it from config instead?
    return transitModel.getTimeZone();
  }

  private ElevationGridCoverageFactory createNedElevationFactory() {
    var nedCacheDirectory = new File(dataSources.getCacheDirectory(), "ned");

    // Download the elevation tiles from an Amazon S3 bucket
    DegreeGridNEDTileSource awsTileSource = new DegreeGridNEDTileSource();
    awsTileSource.awsAccessKey = config.elevationBucket.accessKey;
    awsTileSource.awsSecretKey = config.elevationBucket.secretKey;
    awsTileSource.awsBucketName = config.elevationBucket.bucketName;

    return new NEDGridCoverageFactoryImpl(nedCacheDirectory, awsTileSource);
  }

  private List<ElevationGridCoverageFactory> createDemGeotiffGridCoverageFactories() {
    List<ElevationGridCoverageFactory> elevationGridCoverageFactories = new ArrayList<>();
    for (DataSource demSource : dataSources.get(DEM)) {
      elevationGridCoverageFactories.add(createGeotiffGridCoverageFactoryImpl(demSource));
    }
    return elevationGridCoverageFactories;
  }

  private GraphBuilderModule createElevationModule(
    ElevationGridCoverageFactory it,
    File cacheDirectory
  ) {
    var cachedElevationsFile = new File(cacheDirectory, "cached_elevations.obj");

    return new ElevationModule(
      it,
      graph,
      issueStore,
      cachedElevationsFile,
      elevationData.read(),
      config.readCachedElevations,
      config.writeCachedElevations,
      config.elevationUnitMultiplier,
      config.distanceBetweenElevationSamples,
      config.maxElevationPropagationMeters,
      config.includeEllipsoidToGeoidDifference,
      config.multiThreadElevationCalculations
    );
  }

  private ElevationGridCoverageFactory createGeotiffGridCoverageFactoryImpl(DataSource demSource) {
    return new GeotiffGridCoverageFactoryImpl(demSource);
  }
}
