package org.opentripplanner.graph_builder.module.configure;

import static org.opentripplanner.datastore.api.FileType.DEM;
import static org.opentripplanner.datastore.api.FileType.GTFS;
import static org.opentripplanner.datastore.api.FileType.NETEX;
import static org.opentripplanner.datastore.api.FileType.OSM;

import dagger.Module;
import dagger.Provides;
import java.io.File;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.dataoverlay.EdgeUpdaterModule;
import org.opentripplanner.ext.dataoverlay.configure.DataOverlayFactory;
import org.opentripplanner.ext.transferanalyzer.DirectTransferAnalyzer;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.DataImportIssuesToHTML;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.DirectTransferGenerator;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.graph_builder.module.PruneNoThruIslands;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.ned.DegreeGridNEDTileSource;
import org.opentripplanner.graph_builder.module.ned.ElevationModule;
import org.opentripplanner.graph_builder.module.ned.GeotiffGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.ned.NEDGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.netex.NetexModule;
import org.opentripplanner.netex.configure.NetexConfig;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.service.TransitModel;

/**
 * Configure all modules witch is not simple enough to be injected.
 */
@Module
public class GraphBuilderModules {

  @Provides
  @Singleton
  static OpenStreetMapModule provideOpenStreetMapModule(
    GraphBuilderDataSources dataSources,
    BuildConfig config,
    Graph graph,
    @Nullable ZoneId zoneId,
    DataImportIssueStore issueStore
  ) {
    List<OpenStreetMapProvider> providers = new ArrayList<>();
    for (DataSource osmFile : dataSources.get(OSM)) {
      providers.add(new OpenStreetMapProvider(osmFile, config.osmCacheDataInMem));
    }

    return new OpenStreetMapModule(
      config,
      providers,
      config.boardingLocationTags,
      graph,
      zoneId,
      issueStore
    );
  }

  @Provides
  @Singleton
  static GtfsModule provideGtfsModule(
    GraphBuilderDataSources dataSources,
    BuildConfig config,
    Graph graph,
    TransitModel transitModel,
    DataImportIssueStore issueStore
  ) {
    List<GtfsBundle> gtfsBundles = new ArrayList<>();
    for (DataSource gtfsData : dataSources.get(GTFS)) {
      GtfsBundle gtfsBundle = new GtfsBundle((CompositeDataSource) gtfsData);

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

  @Provides
  @Singleton
  static NetexModule provideNetexModule(
    GraphBuilderDataSources dataSources,
    BuildConfig config,
    Graph graph,
    TransitModel transitModel,
    DataImportIssueStore issueStore
  ) {
    return new NetexConfig(config)
      .createNetexModule(dataSources.get(NETEX), transitModel, graph, issueStore);
  }

  @Provides
  @Singleton
  static StreetLinkerModule provideStreetLinkerModule(
    BuildConfig config,
    Graph graph,
    TransitModel transitModel,
    DataImportIssueStore issueStore
  ) {
    return new StreetLinkerModule(graph, transitModel, issueStore, config.areaVisibility);
  }

  @Provides
  @Singleton
  static PruneNoThruIslands providePruneNoThruIslands(
    BuildConfig config,
    Graph graph,
    TransitModel transitModel,
    DataImportIssueStore issueStore
  ) {
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

  @Provides
  @Singleton
  static List<ElevationModule> provideElevationModules(
    BuildConfig config,
    GraphBuilderDataSources dataSources,
    Graph graph,
    OpenStreetMapModule osmModule,
    DataImportIssueStore issueStore
  ) {
    List<ElevationModule> result = new ArrayList<>();
    List<ElevationGridCoverageFactory> gridCoverageFactories = new ArrayList<>();
    if (config.elevationBucket != null) {
      gridCoverageFactories.add(
        createNedElevationFactory(new File(dataSources.getCacheDirectory(), "ned"), config)
      );
    } else if (dataSources.has(DEM)) {
      gridCoverageFactories.addAll(createDemGeotiffGridCoverageFactories(dataSources.get(DEM)));
    }
    // Refactoring this class, it was made clear that this allows for adding multiple elevation
    // modules to the same graph builder. We do not actually know if this is supported by the
    // ElevationModule class.
    for (ElevationGridCoverageFactory it : gridCoverageFactories) {
      result.add(
        createElevationModule(
          config,
          graph,
          issueStore,
          it,
          osmModule,
          dataSources.getCacheDirectory()
        )
      );
    }
    return result;
  }

  @Provides
  @Singleton
  static DirectTransferGenerator provideDirectTransferGenerator(
    BuildConfig config,
    Graph graph,
    TransitModel transitModel,
    DataImportIssueStore issueStore
  ) {
    var maxTransferDuration = Duration.ofSeconds((long) config.maxTransferDurationSeconds);
    return new DirectTransferGenerator(
      graph,
      transitModel,
      issueStore,
      maxTransferDuration,
      config.transferRequests
    );
  }

  @Provides
  @Singleton
  static DirectTransferAnalyzer provideDirectTransferAnalyzer(
    BuildConfig config,
    Graph graph,
    TransitModel transitModel,
    DataImportIssueStore issueStore
  ) {
    return new DirectTransferAnalyzer(
      graph,
      transitModel,
      issueStore,
      config.maxTransferDurationSeconds * new RoutingRequest().walkSpeed
    );
  }

  @Provides
  @Singleton
  static EdgeUpdaterModule provideDataOverlayFactory(BuildConfig config, Graph graph) {
    return DataOverlayFactory.create(graph, config.dataOverlay);
  }

  @Provides
  @Singleton
  static DataImportIssuesToHTML provideDataImportIssuesToHTML(
    GraphBuilderDataSources dataSources,
    BuildConfig config,
    DataImportIssueStore issueStore
  ) {
    return new DataImportIssuesToHTML(
      issueStore,
      dataSources.getBuildReportDir(),
      config.maxDataImportIssuesPerFile
    );
  }

  /* private methods */

  private static ElevationGridCoverageFactory createNedElevationFactory(
    File nedCacheDirectory,
    BuildConfig config
  ) {
    // Download the elevation tiles from an Amazon S3 bucket
    DegreeGridNEDTileSource awsTileSource = new DegreeGridNEDTileSource();
    awsTileSource.awsAccessKey = config.elevationBucket.accessKey;
    awsTileSource.awsSecretKey = config.elevationBucket.secretKey;
    awsTileSource.awsBucketName = config.elevationBucket.bucketName;

    return new NEDGridCoverageFactoryImpl(nedCacheDirectory, awsTileSource);
  }

  private static List<ElevationGridCoverageFactory> createDemGeotiffGridCoverageFactories(
    Iterable<DataSource> dataSources
  ) {
    List<ElevationGridCoverageFactory> elevationGridCoverageFactories = new ArrayList<>();
    for (DataSource demSource : dataSources) {
      elevationGridCoverageFactories.add(createGeotiffGridCoverageFactoryImpl(demSource));
    }
    return elevationGridCoverageFactories;
  }

  private static ElevationModule createElevationModule(
    BuildConfig config,
    Graph graph,
    DataImportIssueStore issueStore,
    ElevationGridCoverageFactory it,
    OpenStreetMapModule osmModule,
    File cacheDirectory
  ) {
    var cachedElevationsFile = new File(cacheDirectory, "cached_elevations.obj");

    return new ElevationModule(
      it,
      graph,
      issueStore,
      cachedElevationsFile,
      osmModule.elevationDataOutput(),
      config.readCachedElevations,
      config.writeCachedElevations,
      config.elevationUnitMultiplier,
      config.distanceBetweenElevationSamples,
      config.maxElevationPropagationMeters,
      config.includeEllipsoidToGeoidDifference,
      config.multiThreadElevationCalculations
    );
  }

  private static ElevationGridCoverageFactory createGeotiffGridCoverageFactoryImpl(
    DataSource demSource
  ) {
    return new GeotiffGridCoverageFactoryImpl(demSource);
  }
}
