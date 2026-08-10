package org.opentripplanner.graph_builder;

import static org.opentripplanner.datastore.api.FileType.GTFS;
import static org.opentripplanner.datastore.api.FileType.NETEX;
import static org.opentripplanner.datastore.api.FileType.OSM;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import javax.annotation.Nullable;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayRepository;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.cache.GraphBuildCacheManager;
import org.opentripplanner.graph_builder.module.configure.DaggerGraphBuilderFactory;
import org.opentripplanner.graph_builder.module.configure.GraphBuilderFactory;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.streetdetails.StreetDetailsRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeRepository;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.street.StreetRepository;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.utils.lang.OtpNumberFormat;
import org.opentripplanner.utils.time.DurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This makes a Graph out of various inputs like GTFS and OSM. It is modular: GraphBuilderModules
 * are placed in a list and run in sequence.
 */
public class GraphBuilder implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(GraphBuilder.class);

  private final Queue<GraphBuilderModule> graphBuilderModules = new LinkedList<>();
  private final Graph graph;
  private final TransitRepository transitRepository;
  private final DataImportIssueStore issueStore;
  private final Closeable closeDataSourcesHandle;
  private final DeduplicatorService deduplicator;
  private final GraphBuildCacheManager cacheManager;

  private boolean hasTransitData = false;

  public GraphBuilder(
    Graph baseGraph,
    DeduplicatorService deduplicator,
    TransitRepository transitRepository,
    DataImportIssueStore issueStore,
    Closeable closeDataSourcesHandle,
    GraphBuildCacheManager cacheManager
  ) {
    this.graph = baseGraph;
    this.deduplicator = deduplicator;
    this.transitRepository = transitRepository;
    this.issueStore = issueStore;
    this.closeDataSourcesHandle = closeDataSourcesHandle;
    this.cacheManager = cacheManager;
  }

  /**
   * Factory method to create and configure a GraphBuilder with all the appropriate modules to build
   * a graph from the given data source and configuration directory.
   */
  public static GraphBuilder create(
    BuildConfig config,
    GraphBuilderDataSources dataSources,
    Graph graph,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    StreetDetailsRepository streetDetailsRepository,
    FareServiceFactory fareServiceFactory,
    StreetRepository streetRepository,
    TransitRepository transitRepository,
    TransferRepository transferRepository,
    WorldEnvelopeRepository worldEnvelopeRepository,
    VehicleParkingRepository vehicleParkingService,
    @Nullable EmissionRepository emissionRepository,
    @Nullable EmpiricalDelayRepository empiricalDelayRepository,
    @Nullable StopConsolidationRepository stopConsolidationRepository,
    boolean loadStreetGraph,
    boolean saveStreetGraph
  ) {
    boolean hasOsm = dataSources.has(OSM);
    boolean hasGtfs = dataSources.has(GTFS);
    boolean hasNetex = dataSources.has(NETEX);
    boolean hasTransitData = hasGtfs || hasNetex;

    transitRepository.initTimeZone(config.transitModelTimeZone);

    GraphBuilderFactory.Builder builder = DaggerGraphBuilderFactory.builder();
    builder
      .config(config)
      .graph(graph)
      .osmInfoGraphBuildRepository(osmInfoGraphBuildRepository)
      .streetDetailsRepository(streetDetailsRepository)
      .streetRepository(streetRepository)
      .transitRepository(transitRepository)
      .transferRepository(transferRepository)
      .worldEnvelopeRepository(worldEnvelopeRepository)
      .vehicleParkingRepository(vehicleParkingService)
      .stopConsolidationRepository(stopConsolidationRepository)
      .emissionRepository(emissionRepository)
      .empiricalDelayRepository(empiricalDelayRepository)
      .fareServiceFactory(fareServiceFactory)
      .dataSources(dataSources)
      .timeZoneId(transitRepository.getTimeZone());

    var factory = builder.build();

    var graphBuilder = factory.graphBuilder();

    graphBuilder.hasTransitData = hasTransitData;

    if (hasOsm) {
      graphBuilder.addModule(factory.osmModule());
    }

    if (hasGtfs) {
      graphBuilder.addModule(factory.gtfsModule());
    }

    if (hasNetex) {
      graphBuilder.addModule(factory.netexModule());
    }

    // Consolidate stops only if a stop consolidation repo has been provided
    if (hasTransitData) {
      graphBuilder.addModuleOptional(factory.stopConsolidationModule());
      graphBuilder.addModule(factory.tripPatternNamer());
      graphBuilder.addModuleOptional(
        factory.timeZoneAdjusterModule(),
        transitRepository.getAgencyTimeZones().size() > 1
      );

      if (hasOsm || graphBuilder.graph.hasStreets) {
        graphBuilder.addModule(factory.osmBoardingLocationsModule());
      }
    }

    // This module is outside the hasGTFS conditional block because it also links things like parking
    // which need to be handled even when there's no transit.
    graphBuilder.addModule(factory.streetLinkerModule());

    // Avoid applying turn restrictions twice if doing separate street graph and graph builds.
    if (hasOsm) {
      graphBuilder.addModule(factory.turnRestrictionModule());
    }

    // Prune graph connectivity islands after transit stop linking, so that pruning can take into account
    // existence of stops in islands. If an island has a stop, it actually may be a real island and should
    // not be removed quite as easily
    if ((hasOsm && !saveStreetGraph) || loadStreetGraph) {
      graphBuilder.addModule(factory.pruneIslands());
    }

    // Load elevation data and apply it to the streets.
    // We want to do run this module after loading the OSM street network but before finding transfers.
    for (GraphBuilderModule it : factory.elevationModules()) {
      graphBuilder.addModule(it);
    }

    if (hasTransitData) {
      // Add links to flex areas after the streets has been split, so that also the split edges are connected
      graphBuilder.addModuleOptional(factory.areaStopsToVerticesMapper(), OTPFeature.FlexRouting);

      // This module will use streets or straight line distance depending on whether OSM data is found in the graph.
      graphBuilder.addModule(factory.directTransferGenerator());

      // Analyze routing between stops to generate report
      graphBuilder.addModuleOptional(factory.directTransferAnalyzer(), OTPFeature.TransferAnalyzer);

      graphBuilder.addModuleOptional(factory.emissionGraphBuilder(), OTPFeature.Emission);

      graphBuilder.addModuleOptional(
        factory.empiricalDelayGraphBuilder(),
        OTPFeature.EmpiricalDelay
      );
    }

    if (loadStreetGraph || hasOsm) {
      graphBuilder.addModule(factory.graphCoherencyCheckerModule());
    }

    graphBuilder.addModule(factory.stopConnectivityModule());

    graphBuilder.addModuleOptional(factory.routeToCentroidStationIdValidator());

    graphBuilder.addModuleOptional(factory.dataImportIssueReporter(), config.dataImportReport);

    graphBuilder.addModuleOptional(factory.dataOverlayFactory(), OTPFeature.DataOverlay);

    graphBuilder.addModule(factory.calculateWorldEnvelopeModule());

    return graphBuilder;
  }

  public void run() {
    try {
      // Record how long it takes to build the graph, purely for informational purposes.
      long startTime = System.currentTimeMillis();

      // Check all graph builder inputs, and fail fast to avoid waiting until the build process
      // advances.
      for (GraphBuilderModule builder : graphBuilderModules) {
        builder.checkInputs();
      }

      // because we want to garbage-collect the modules as soon as they are finished
      // we remove them from the queue during the build process
      while (!graphBuilderModules.isEmpty()) {
        var builder = graphBuilderModules.poll();
        builder.buildGraph();
      }

      new DataImportIssueSummary(issueStore.listIssues()).logSummary();

      // Log before we validate, this way we have more information if the validation fails
      logGraphBuilderCompleteStatus(startTime, graph, transitRepository, deduplicator);

      validate();
    } finally {
      try {
        cacheManager.close();
      } finally {
        closeDataSources();
      }
    }
  }

  private void addModuleOptional(@Nullable GraphBuilderModule module, OTPFeature feature) {
    addModuleOptional(module, feature.isOn());
  }

  private void addModuleOptional(@Nullable GraphBuilderModule module, boolean enabled) {
    if (enabled) {
      addModuleOptional(module);
    }
  }

  private void addModuleOptional(@Nullable GraphBuilderModule module) {
    if (module != null) {
      addModule(module);
    }
  }

  private void addModule(GraphBuilderModule module) {
    graphBuilderModules.add(Objects.requireNonNull(module));
  }

  private boolean hasTransitData() {
    return hasTransitData;
  }

  public DataImportIssueSummary issueSummary() {
    return new DataImportIssueSummary(issueStore.listIssues());
  }

  /**
   * Validates the build. Currently, only checks if the graph has transit data if any transit data
   * sets were included in the build. If all transit data gets filtered out due to transit period
   * configuration, for example, then this function will throw a {@link OtpAppException}.
   */
  private void validate() {
    if (hasTransitData() && !transitRepository.hasTransit()) {
      throw new OtpAppException(
        "The provided transit data have no trips within the configured transit service period. " +
          "There is something wrong with your data - see the log above. Another possibility is that the " +
          "'transitServiceStart' and 'transitServiceEnd' are not correctly configured."
      );
    }
  }

  private void closeDataSources() {
    try {
      closeDataSourcesHandle.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void logGraphBuilderCompleteStatus(
    long startTime,
    Graph graph,
    TransitRepository transitRepository,
    DeduplicatorService deduplicator
  ) {
    long endTime = System.currentTimeMillis();
    String time = DurationUtils.durationToStr(Duration.ofMillis(endTime - startTime));
    var f = new OtpNumberFormat();
    var nStops = f.formatNumber(transitRepository.getSiteRepository().stopIndexSize());
    var nPatterns = f.formatNumber(transitRepository.getAllTripPatterns().size());
    var nTransfers = f.formatNumber(
      transitRepository.getConstrainedTransferService().listAll().size()
    );
    var nVertices = f.formatNumber(graph.countVertices());
    var nEdges = f.formatNumber(graph.countEdges());

    LOG.info("Graph building took {}.", time);
    LOG.info("Graph built.   |V|={} |E|={}", nVertices, nEdges);
    LOG.info(
      "Transit built. |Stops|={} |Patterns|={} |ConstrainedTransfers|={}",
      nStops,
      nPatterns,
      nTransfers
    );
    // Log size info for the deduplicator
    LOG.info("Memory optimized {}", deduplicator.toString());
  }
}
