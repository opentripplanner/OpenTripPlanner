package org.opentripplanner.graph_builder;

import static org.opentripplanner.datastore.FileType.DEM;
import static org.opentripplanner.datastore.FileType.GTFS;
import static org.opentripplanner.datastore.FileType.NETEX;
import static org.opentripplanner.datastore.FileType.OSM;
import static org.opentripplanner.netex.configure.NetexConfig.netexModule;

import com.google.common.collect.Lists;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.opentripplanner.datastore.CompositeDataSource;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.ext.dataoverlay.configure.DataOverlayFactory;
import org.opentripplanner.ext.flex.FlexLocationsToStreetEdgesMapper;
import org.opentripplanner.ext.transferanalyzer.DirectTransferAnalyzer;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.DirectTransferGenerator;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.graph_builder.module.PruneNoThruIslands;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.TransitToTaggedStopsModule;
import org.opentripplanner.graph_builder.module.map.BusRouteStreetMatcher;
import org.opentripplanner.graph_builder.module.ned.DegreeGridNEDTileSource;
import org.opentripplanner.graph_builder.module.ned.ElevationModule;
import org.opentripplanner.graph_builder.module.ned.GeotiffGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.ned.NEDGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.openstreetmap.BinaryOpenStreetMapProvider;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.S3BucketConfig;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This makes a Graph out of various inputs like GTFS and OSM.
 * It is modular: GraphBuilderModules are placed in a list and run in sequence.
 */
public class GraphBuilder implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(GraphBuilder.class);

    private final List<GraphBuilderModule> graphBuilderModules = new ArrayList<>();

    private final Graph graph;

    private GraphBuilder(Graph baseGraph) {
        this.graph = baseGraph == null ? new Graph() : baseGraph;
    }

    private void addModule(GraphBuilderModule loader) {
        graphBuilderModules.add(loader);
    }

    public Graph getGraph() {
        return graph;
    }

    public void run() {
         // Record how long it takes to build the graph, purely for informational purposes.
        long startTime = System.currentTimeMillis();

        // Check all graph builder inputs, and fail fast to avoid waiting until the build process
        // advances.
        for (GraphBuilderModule builder : graphBuilderModules) {
            builder.checkInputs();
        }

        DataImportIssueStore issueStore = new DataImportIssueStore(true);
        HashMap<Class<?>, Object> extra = new HashMap<Class<?>, Object>();

        for (GraphBuilderModule load : graphBuilderModules) {
            load.buildGraph(graph, extra, issueStore);
        }
        issueStore.summarize();

        long endTime = System.currentTimeMillis();
        LOG.info(String.format("Graph building took %.1f minutes.", (endTime - startTime) / 1000 / 60.0));
        LOG.info("Main graph size: |V|={} |E|={}", graph.countVertices(), graph.countEdges());
    }

    /**
     * Factory method to create and configure a GraphBuilder with all the appropriate modules to
     * build a graph from the given data source and configuration directory.
     */
    public static GraphBuilder create(
            BuildConfig config,
            GraphBuilderDataSources dataSources,
            Graph baseGraph,
            boolean loadStreetGraph,
            boolean saveStreetGraph
    ) {

        boolean hasOsm  = dataSources.has(OSM);
        boolean hasDem  = dataSources.has(DEM);
        boolean hasGtfs = dataSources.has(GTFS);
        boolean hasNetex = dataSources.has(NETEX);
        boolean hasTransitData = hasGtfs || hasNetex;

        GraphBuilder graphBuilder = new GraphBuilder(baseGraph);

        if ( hasOsm ) {
            List<BinaryOpenStreetMapProvider> osmProviders = Lists.newArrayList();
            for (DataSource osmFile : dataSources.get(OSM)) {
                osmProviders.add(
                        new BinaryOpenStreetMapProvider(osmFile, config.osmCacheDataInMem)
                );
            }
            OpenStreetMapModule osmModule = new OpenStreetMapModule(osmProviders);
            DefaultStreetEdgeFactory streetEdgeFactory = new DefaultStreetEdgeFactory();
            streetEdgeFactory.useElevationData = hasDem;
            osmModule.edgeFactory = streetEdgeFactory;
            osmModule.customNamer = config.customNamer;
            osmModule.setDefaultWayPropertySetSource(config.osmWayPropertySet);
            osmModule.skipVisibility = !config.areaVisibility;
            osmModule.platformEntriesLinking = config.platformEntriesLinking;
            osmModule.staticBikeParkAndRide = config.staticBikeParkAndRide;
            osmModule.staticParkAndRide = config.staticParkAndRide;
            osmModule.banDiscouragedWalking = config.banDiscouragedWalking;
            osmModule.banDiscouragedBiking = config.banDiscouragedBiking;
            osmModule.maxAreaNodes = config.maxAreaNodes;
            graphBuilder.addModule(osmModule);
        }
        if ( hasGtfs ) {
            List<GtfsBundle> gtfsBundles = Lists.newArrayList();
            for (DataSource gtfsData : dataSources.get(GTFS)) {

                GtfsBundle gtfsBundle = new GtfsBundle((CompositeDataSource)gtfsData);

                if (config.parentStopLinking) {
                    gtfsBundle.linkStopsToParentStations = true;
                }
                gtfsBundle.parentStationTransfers = config.stationTransfers;
                gtfsBundle.subwayAccessTime = config.getSubwayAccessTimeSeconds();
                gtfsBundle.maxInterlineDistance = config.maxInterlineDistance;
                gtfsBundles.add(gtfsBundle);
            }
            GtfsModule gtfsModule = new GtfsModule(gtfsBundles, config.getTransitServicePeriod());
            gtfsModule.setFareServiceFactory(config.fareServiceFactory);
            graphBuilder.addModule(gtfsModule);
        }

        if( hasNetex ) {
            graphBuilder.addModule(netexModule(config, dataSources.get(NETEX)));
        }

        if(hasTransitData && (hasOsm || graphBuilder.graph.hasStreets)) {
            if (config.matchBusRoutesToStreets) {
                graphBuilder.addModule(new BusRouteStreetMatcher());
            }
            graphBuilder.addModule(new TransitToTaggedStopsModule());
        }

        // This module is outside the hasGTFS conditional block because it also links things like bike rental
        // which need to be handled even when there's no transit.
        StreetLinkerModule streetLinkerModule = new StreetLinkerModule();
        streetLinkerModule.setAddExtraEdgesToAreas(config.areaVisibility);
        graphBuilder.addModule(streetLinkerModule);

        // Prune graph connectivity islands after transit stop linking, so that pruning can take into account
        // existence of stops in islands. If an island has a stop, it actually may be a real island and should
        // not be removed quite as easily
        if ((hasOsm && !saveStreetGraph) || loadStreetGraph) {
            PruneNoThruIslands pruneNoThruIslands = new PruneNoThruIslands(streetLinkerModule);
            pruneNoThruIslands.setPruningThresholdIslandWithoutStops(config.pruningThresholdIslandWithoutStops);
            pruneNoThruIslands.setPruningThresholdIslandWithStops(config.pruningThresholdIslandWithStops);
            graphBuilder.addModule(pruneNoThruIslands);
        }

        // Load elevation data and apply it to the streets.
        // We want to do run this module after loading the OSM street network but before finding transfers.
        List<ElevationGridCoverageFactory> elevationGridCoverageFactories = new ArrayList<>();
        if (config.elevationBucket != null) {
            // Download the elevation tiles from an Amazon S3 bucket
            S3BucketConfig bucketConfig = config.elevationBucket;
            File cacheDirectory = new File(dataSources.getCacheDirectory(), "ned");
            DegreeGridNEDTileSource awsTileSource = new DegreeGridNEDTileSource();
            awsTileSource.awsAccessKey = bucketConfig.accessKey;
            awsTileSource.awsSecretKey = bucketConfig.secretKey;
            awsTileSource.awsBucketName = bucketConfig.bucketName;
            elevationGridCoverageFactories.add(
                new NEDGridCoverageFactoryImpl(cacheDirectory, awsTileSource));
        } else if (dataSources.has(DEM)) {
            // Load the elevation from a file in the graph inputs directory
            for (DataSource demSource : dataSources.get(DEM)) {
                elevationGridCoverageFactories.add(new GeotiffGridCoverageFactoryImpl(demSource));
            }
        }
        // Refactoring this class, it was made clear that this allows for adding multiple elevation
        // modules to the same graph builder. We do not actually know if this is supported by the
        // ElevationModule class.
        for (ElevationGridCoverageFactory factory : elevationGridCoverageFactories) {
            graphBuilder.addModule(
                new ElevationModule(
                    factory,
                    new File(dataSources.getCacheDirectory(), "cached_elevations.obj"),
                    config.readCachedElevations,
                    config.writeCachedElevations,
                    config.elevationUnitMultiplier,
                    config.distanceBetweenElevationSamples,
                    config.includeEllipsoidToGeoidDifference,
                    config.multiThreadElevationCalculations
                )
            );
        }
        if ( hasTransitData ) {
            // Add links to flex areas after the streets has been split, so that also the split edges are connected
            if (OTPFeature.FlexRouting.isOn()) {
                graphBuilder.addModule(new FlexLocationsToStreetEdgesMapper());
            }

            // This module will use streets or straight line distance depending on whether OSM data is found in the graph.
            graphBuilder.addModule(new DirectTransferGenerator(config.maxTransferDurationSeconds, config.transferRequests));

            // Analyze routing between stops to generate report
            if (OTPFeature.TransferAnalyzer.isOn()) {
                graphBuilder.addModule(new DirectTransferAnalyzer(
                    config.maxTransferDurationSeconds * new RoutingRequest().walkSpeed)
                );
            }
        }

        if (config.dataImportReport) {
            graphBuilder.addModule(
                    new DataImportIssuesToHTML(
                            dataSources.getBuildReportDir(),
                            config.maxDataImportIssuesPerFile
                    )
            );
        }

        if (OTPFeature.DataOverlay.isOn()) {
            var module = DataOverlayFactory.create(config.dataOverlay);
            if(module != null) {
                graphBuilder.addModule(module);
            }
        }

        return graphBuilder;
    }
}
