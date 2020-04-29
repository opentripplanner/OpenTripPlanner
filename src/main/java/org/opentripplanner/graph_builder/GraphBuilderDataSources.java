package org.opentripplanner.graph_builder;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.datastore.CompositeDataSource;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.FileType;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.util.OtpAppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

import static org.opentripplanner.datastore.FileType.DEM;
import static org.opentripplanner.datastore.FileType.GTFS;
import static org.opentripplanner.datastore.FileType.NETEX;
import static org.opentripplanner.datastore.FileType.OSM;


/**
 * This is a wrapper around an {@link OtpDataStore} adding the ability to filter
 * witch data source input files should be used and validate the available input files
 * against the command line parameters set.
 * <p/>
 * After this class is validated the {@link #has(FileType)} method can be used to
 * determine if a the build process should include a file in the build.
 * <p/>
 * By separating this from the builder, this class can be constructed early, causing
 * a validation of the available data-sources against the configuration - and then if
 * not valid - abort the entire OTP startup early, before spending time on loading any
 * data - like the streetGraph.
 */
public class GraphBuilderDataSources {
    private static final Logger LOG = LoggerFactory.getLogger(GraphBuilderDataSources.class);

    private final OtpDataStore store;
    private final Multimap<FileType, DataSource> inputData = ArrayListMultimap.create();
    private final Multimap<FileType, DataSource> skipData = ArrayListMultimap.create();
    private final Set<FileType> includeTypes = EnumSet.complementOf(EnumSet.of(FileType.UNKNOWN));
    private final File cacheDirectory;
    private final DataSource outputGraph;

    private GraphBuilderDataSources(
            CommandLineParameters cli,
            BuildConfig bc,
            OtpDataStore store
    ) {
        this.store = store;
        this.cacheDirectory = cli.cacheDirectory;
        this.outputGraph = getOutputGraph(cli);

        // Select witch files to import
        include(cli.doBuildStreet() && bc.streets, OSM);
        include(cli.doBuildStreet() && bc.streets, DEM);
        include(cli.doBuildTransit() && bc.transit, GTFS);
        include(cli.doBuildTransit() && bc.transit, NETEX);

        selectFilesToImport();

        // Log all files and expected action to take
        logSkippedAndSelectedFiles();

        // init input vil automatically validate it.
        // Check that the CLI is consistent with the input available
        validateCliMatchesInputData(cli);
    }

    /**
     * Create a wrapper around the data-store and resolve witch files to
     * import and export. Validate these files against the given command line
     * arguments and the graph build parameters.
     */
    public static GraphBuilderDataSources create(
            CommandLineParameters cli,
            BuildConfig bc,
            OtpDataStore store

    ) {
        return new GraphBuilderDataSources(cli, bc, store);
    }

    /**
     * @return {@code true} if and only if the data source exist, proper command line parameters is
     * set and not disabled by the loaded configuration files.
     */
    boolean has(FileType type) {
        return inputData.containsKey(type);
    }

    Iterable<DataSource> get(FileType type) {
        return inputData.get(type);
    }

    CompositeDataSource getBuildReportDir() {
        return store.getBuildReportDir();
    }

    File getCacheDirectory() {
        return cacheDirectory;
    }

    public DataSource getOutputGraph() {
        return outputGraph;
    }


    /* private methods */

    private boolean hasOneOf(FileType ... types) {
        for (FileType type : types) {
            if(has(type)) {
                return true;
            }
        }
        return false;
    }

    private void logSkippedAndSelectedFiles() {
        LOG.info("Loading files from: {}", String.join(", ", store.getRepositoryDescriptions()));

        // Sort data input files by type
        for (FileType type : FileType.values()) {
            for (DataSource source : inputData.get(type)) {
                if (type == FileType.CONFIG) {
                    log("%s loaded", source);
                }
                else {
                    log("Found %s", source);
                }
            }
        }
        for (FileType type : FileType.values()) {

            for (DataSource source : skipData.get(type)) {
                log("Skipping %s", source);
            }
        }
    }

    private void validateCliMatchesInputData(CommandLineParameters cli) {
        if (cli.build) {
            if (!hasOneOf(OSM, GTFS, NETEX)) {
                throw new OtpAppException("Unable to build graph, no transit data available.");
            }
        }
        else if (cli.buildStreet) {
            if (!has(OSM)) {
                throw new OtpAppException("Unable to build street graph, no OSM data available.");
            }
        }
        else if (cli.load) {
            if (!store.getGraph().exists()) {
                throw new OtpAppException(
                        "Unable to load graph, no graph file found: %s",
                        store.getGraph().path()
                );
            }
        }
        else if (cli.loadStreet) {
            if (!store.getStreetGraph().exists()) {
                throw new OtpAppException(
                        "Unable to load street graph, no street graph file found: %s",
                        store.getStreetGraph().path()
                );
            }
        }
    }

    private DataSource getOutputGraph(CommandLineParameters cli) {
        if(cli.doSaveGraph()) {
            return store.getGraph();
        }
        else if(cli.doSaveStreetGraph()) {
            return store.getStreetGraph();
        }
        return null;
    }

    private void log(String op, DataSource source) {
        String opTxt = String.format(op, source.type().text());
        LOG.info("- {} {}", opTxt, source.detailedInfo());
    }

    private void include(boolean include, FileType type) {
        // Add or remove type - we do not care if the element already exist or not
        if(include) {
            includeTypes.add(type);
        }
        else {
            includeTypes.remove(type);
        }
    }

    private void selectFilesToImport() {
        for (FileType type : FileType.values()) {
            if (includeTypes.contains(type)) {
                inputData.putAll(type, store.listExistingSourcesFor(type));
            }
            else {
                skipData.putAll(type, store.listExistingSourcesFor(type));
            }
        }
    }
}
