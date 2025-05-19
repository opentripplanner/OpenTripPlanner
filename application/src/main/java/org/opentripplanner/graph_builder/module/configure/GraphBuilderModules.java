package org.opentripplanner.graph_builder.module.configure;

import static org.opentripplanner.datastore.api.FileType.DEM;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.dataoverlay.EdgeUpdaterModule;
import org.opentripplanner.ext.dataoverlay.configure.DataOverlayFactory;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationModule;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.ext.transferanalyzer.DirectTransferAnalyzer;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;
import org.opentripplanner.graph_builder.issue.report.DataImportIssueReporter;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.model.ConfiguredDataSource;
import org.opentripplanner.graph_builder.module.DirectTransferGenerator;
import org.opentripplanner.graph_builder.module.RouteToCentroidStationIdsValidator;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.TurnRestrictionModule;
import org.opentripplanner.graph_builder.module.islandpruning.PruneIslands;
import org.opentripplanner.graph_builder.module.ned.DegreeGridNEDTileSource;
import org.opentripplanner.graph_builder.module.ned.ElevationModule;
import org.opentripplanner.graph_builder.module.ned.GeotiffGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.ned.NEDGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.ned.parameter.DemExtractParameters;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractParameters;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.netex.NetexModule;
import org.opentripplanner.netex.configure.NetexConfigure;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.osm.OsmProvider;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.street.model.StreetLimitationParameters;
import org.opentripplanner.transit.service.TimetableRepository;

/**
 * Configure all modules that are not simple enough to be injected.
 */
@Module
public class GraphBuilderModules {

  @Provides
  @Singleton
  static OsmModule provideOsmModule(
    GraphBuilderDataSources dataSources,
    BuildConfig config,
    Graph graph,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    VehicleParkingRepository vehicleParkingRepository,
    DataImportIssueStore issueStore,
    StreetLimitationParameters streetLimitationParameters
  ) {
    List<OsmProvider> providers = new ArrayList<>();
    for (ConfiguredDataSource<
      OsmExtractParameters
    > osmConfiguredDataSource : dataSources.getOsmConfiguredDatasource()) {
      providers.add(
        new DefaultOsmProvider(
          osmConfiguredDataSource.dataSource(),
          osmConfiguredDataSource.config().osmTagMapper(),
          osmConfiguredDataSource.config().timeZone(),
          osmConfiguredDataSource.config().includeOsmSubwayEntrances(),
          config.osmCacheDataInMem,
          issueStore
        )
      );
    }

    return OsmModule.of(providers, graph, osmInfoGraphBuildRepository, vehicleParkingRepository)
      .withEdgeNamer(config.edgeNamer)
      .withAreaVisibility(config.areaVisibility)
      .withPlatformEntriesLinking(config.platformEntriesLinking)
      .withStaticParkAndRide(config.staticParkAndRide)
      .withStaticBikeParkAndRide(config.staticBikeParkAndRide)
      .withMaxAreaNodes(config.maxAreaNodes)
      .withBoardingAreaRefTags(config.boardingLocationTags)
      .withIncludeOsmSubwayEntrances(config.osmDefaults.includeOsmSubwayEntrances())
      .withIssueStore(issueStore)
      .withStreetLimitationParameters(streetLimitationParameters)
      .build();
  }

  @Provides
  @Singleton
  static GtfsModule provideGtfsModule(
    GraphBuilderDataSources dataSources,
    BuildConfig config,
    Graph graph,
    TimetableRepository timetableRepository,
    DataImportIssueStore issueStore,
    FareServiceFactory fareServiceFactory
  ) {
    List<GtfsBundle> gtfsBundles = new ArrayList<>();
    for (var gtfsData : dataSources.getGtfsConfiguredDatasource()) {
      gtfsBundles.add(new GtfsBundle(gtfsData.dataSource(), gtfsData.config()));
    }
    return new GtfsModule(
      gtfsBundles,
      timetableRepository,
      graph,
      issueStore,
      config.getTransitServicePeriod(),
      fareServiceFactory,
      config.maxStopToShapeSnapDistance,
      config.getSubwayAccessTimeSeconds()
    );
  }

  @Provides
  @Singleton
  static NetexModule provideNetexModule(
    GraphBuilderDataSources dataSources,
    BuildConfig config,
    Graph graph,
    TimetableRepository timetableRepository,
    VehicleParkingRepository parkingService,
    DataImportIssueStore issueStore
  ) {
    return new NetexConfigure(config).createNetexModule(
      dataSources.getNetexConfiguredDatasource(),
      timetableRepository,
      parkingService,
      graph,
      issueStore
    );
  }

  @Provides
  @Singleton
  static StreetLinkerModule provideStreetLinkerModule(
    BuildConfig config,
    Graph graph,
    VehicleParkingRepository parkingRepository,
    TimetableRepository timetableRepository,
    DataImportIssueStore issueStore
  ) {
    return new StreetLinkerModule(
      graph,
      parkingRepository,
      timetableRepository,
      issueStore,
      config.areaVisibility,
      config.maxAreaNodes
    );
  }

  @Provides
  @Singleton
  static PruneIslands providePruneIslands(
    BuildConfig config,
    Graph graph,
    VehicleParkingRepository parkingRepository,
    TimetableRepository timetableRepository,
    DataImportIssueStore issueStore
  ) {
    PruneIslands pruneIslands = new PruneIslands(
      graph,
      timetableRepository,
      issueStore,
      new StreetLinkerModule(
        graph,
        parkingRepository,
        timetableRepository,
        issueStore,
        config.areaVisibility,
        config.maxAreaNodes
      )
    );
    pruneIslands.setPruningThresholdIslandWithoutStops(
      config.islandPruning.pruningThresholdIslandWithoutStops
    );
    pruneIslands.setPruningThresholdIslandWithStops(
      config.islandPruning.pruningThresholdIslandWithStops
    );
    pruneIslands.setAdaptivePruningFactor(config.islandPruning.adaptivePruningFactor);
    pruneIslands.setAdaptivePruningDistance(config.islandPruning.adaptivePruningDistance);
    return pruneIslands;
  }

  @Provides
  @Singleton
  static List<ElevationModule> provideElevationModules(
    BuildConfig config,
    GraphBuilderDataSources dataSources,
    Graph graph,
    OsmModule osmModule,
    DataImportIssueStore issueStore
  ) {
    List<ElevationModule> result = new ArrayList<>();
    List<ElevationGridCoverageFactory> gridCoverageFactories = new ArrayList<>();
    if (config.elevationBucket != null) {
      gridCoverageFactories.add(
        createNedElevationFactory(new File(dataSources.getCacheDirectory(), "ned"), config)
      );
    } else if (dataSources.has(DEM)) {
      gridCoverageFactories.addAll(
        createDemGeotiffGridCoverageFactories(dataSources.getDemConfiguredDatasource())
      );
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
    TimetableRepository timetableRepository,
    DataImportIssueStore issueStore
  ) {
    return new DirectTransferGenerator(
      graph,
      timetableRepository,
      issueStore,
      config.maxTransferDuration,
      config.transferRequests,
      config.transferParametersForMode
    );
  }

  @Provides
  @Singleton
  static DirectTransferAnalyzer provideDirectTransferAnalyzer(
    BuildConfig config,
    Graph graph,
    TimetableRepository timetableRepository,
    DataImportIssueStore issueStore
  ) {
    return new DirectTransferAnalyzer(
      graph,
      timetableRepository,
      issueStore,
      config.maxTransferDuration.toSeconds() * WalkPreferences.DEFAULT.speed()
    );
  }

  @Provides
  @Singleton
  @Nullable
  static EdgeUpdaterModule provideDataOverlayFactory(BuildConfig config, Graph graph) {
    return DataOverlayFactory.create(graph, config.dataOverlay);
  }

  @Provides
  @Singleton
  static DataImportIssueStore provideDataImportIssuesStore() {
    return new DefaultDataImportIssueStore();
  }

  @Provides
  @Singleton
  static DataImportIssueReporter provideDataImportIssuesToHTML(
    GraphBuilderDataSources dataSources,
    BuildConfig config,
    DataImportIssueStore issueStore
  ) {
    return new DataImportIssueReporter(
      issueStore,
      dataSources.getBuildReportDir(),
      config.maxDataImportIssuesPerFile
    );
  }

  @Provides
  @Singleton
  static DataImportIssueSummary providesDataImportIssueSummary(DataImportIssueStore issueStore) {
    return new DataImportIssueSummary(issueStore.listIssues());
  }

  @Provides
  @Singleton
  static TurnRestrictionModule provideTurnRestrictionModule(
    Graph graph,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository
  ) {
    return new TurnRestrictionModule(graph, osmInfoGraphBuildRepository);
  }

  @Provides
  @Singleton
  @Nullable
  static StopConsolidationModule providesStopConsolidationModule(
    TimetableRepository timetableRepository,
    @Nullable StopConsolidationRepository repo,
    GraphBuilderDataSources dataSources
  ) {
    return dataSources
      .stopConsolidation()
      .map(ds -> StopConsolidationModule.of(timetableRepository, repo, ds))
      .orElse(null);
  }

  @Provides
  @Singleton
  @Nullable
  static RouteToCentroidStationIdsValidator routeToCentroidStationIdValidator(
    DataImportIssueStore issueStore,
    BuildConfig config,
    TimetableRepository timetableRepository
  ) {
    var ids = config.transitRouteToStationCentroid();
    return ids.isEmpty()
      ? null
      : new RouteToCentroidStationIdsValidator(issueStore, ids, timetableRepository);
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
    Iterable<ConfiguredDataSource<DemExtractParameters>> dataSources
  ) {
    List<ElevationGridCoverageFactory> elevationGridCoverageFactories = new ArrayList<>();
    for (ConfiguredDataSource<DemExtractParameters> demSource : dataSources) {
      double elevationUnitMultiplier = demSource.config().elevationUnitMultiplier();
      elevationGridCoverageFactories.add(
        createGeotiffGridCoverageFactoryImpl(demSource.dataSource(), elevationUnitMultiplier)
      );
    }
    return elevationGridCoverageFactories;
  }

  private static ElevationModule createElevationModule(
    BuildConfig config,
    Graph graph,
    DataImportIssueStore issueStore,
    ElevationGridCoverageFactory it,
    OsmModule osmModule,
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
      config.distanceBetweenElevationSamples,
      config.maxElevationPropagationMeters,
      config.includeEllipsoidToGeoidDifference,
      config.multiThreadElevationCalculations
    );
  }

  private static ElevationGridCoverageFactory createGeotiffGridCoverageFactoryImpl(
    DataSource demSource,
    double elevationUnitMultiplier
  ) {
    return new GeotiffGridCoverageFactoryImpl(demSource, elevationUnitMultiplier);
  }
}
