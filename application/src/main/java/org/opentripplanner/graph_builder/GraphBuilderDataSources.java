package org.opentripplanner.graph_builder;

import static org.opentripplanner.datastore.api.FileType.DEM;
import static org.opentripplanner.datastore.api.FileType.GTFS;
import static org.opentripplanner.datastore.api.FileType.NETEX;
import static org.opentripplanner.datastore.api.FileType.OSM;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.File;
import java.net.URI;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.api.OtpBaseDirectory;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.graph_builder.module.ned.parameter.DemExtractParameters;
import org.opentripplanner.graph_builder.module.ned.parameter.DemExtractParametersBuilder;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractParameters;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractParametersBuilder;
import org.opentripplanner.gtfs.graphbuilder.GtfsFeedParameters;
import org.opentripplanner.netex.config.NetexFeedParameters;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a wrapper around an {@link OtpDataStore} adding the ability to filter which data source
 * input files should be used and validate the available input files against the command line
 * parameters set.
 * <p/>
 * After this class is validated the {@link #has(FileType)} method can be used to determine if the
 * build process should include a file in the build.
 * <p/>
 * By separating this from the builder, this class can be constructed early, causing a validation of
 * the available data-sources against the configuration - and then if not valid - abort the entire
 * OTP startup early, before spending time on loading any data - like the streetGraph.
 */
@Singleton
public class GraphBuilderDataSources {

  private static final Logger LOG = LoggerFactory.getLogger(GraphBuilderDataSources.class);
  private static final String BULLET_POINT = "- ";

  private final OtpDataStore store;
  private final Multimap<FileType, DataSource> inputData = ArrayListMultimap.create();
  private final Multimap<FileType, DataSource> skipData = ArrayListMultimap.create();
  private final Set<FileType> includeTypes = EnumSet.complementOf(EnumSet.of(FileType.UNKNOWN));
  private final File cacheDirectory;
  private final DataSource outputGraph;
  private final BuildConfig buildConfig;
  private final File baseDirectory;

  /**
   * Create a wrapper around the data-store and resolve which files to import and export. Validate
   * these files against the given command line arguments and the graph build parameters.
   */
  @Inject
  public GraphBuilderDataSources(
    CommandLineParameters cli,
    BuildConfig bc,
    OtpDataStore store,
    @OtpBaseDirectory File baseDirectory
  ) {
    this.store = store;
    this.buildConfig = bc;
    this.cacheDirectory = cli.cacheDirectory;
    this.outputGraph = getOutputGraph(cli);
    this.baseDirectory = baseDirectory;

    // Select which files to import
    include(cli.doBuildStreet(), OSM);
    include(cli.doBuildStreet(), DEM);
    include(cli.doBuildTransit(), GTFS);
    include(cli.doBuildTransit(), NETEX);

    selectFilesToImport();

    // Log all files and expected action to take
    logSkippedAndSelectedFiles();

    // init input vil automatically validate it.
    // Check that the CLI is consistent with the input available
    validateCliMatchesInputData(cli);
  }

  public DataSource getOutputGraph() {
    return outputGraph;
  }

  /**
   * @return {@code true} if and only if the data source exist, proper command line parameters is
   * set and not disabled by the loaded configuration files.
   */
  public boolean has(FileType type) {
    return inputData.containsKey(type);
  }

  public Iterable<DataSource> get(FileType type) {
    return inputData.get(type);
  }

  public Iterable<ConfiguredDataSource<OsmExtractParameters>> getOsmConfiguredDatasource() {
    return inputData
      .get(OSM)
      .stream()
      .map(it -> new ConfiguredDataSource<>(it, getOsmExtractConfig(it)))
      .toList();
  }

  private OsmExtractParameters getOsmExtractConfig(DataSource dataSource) {
    return buildConfig.osm.parameters
      .stream()
      .filter(osmExtractConfig -> uriMatch(osmExtractConfig.source(), dataSource.uri()))
      .findFirst()
      .orElse(
        new OsmExtractParametersBuilder(buildConfig.osmDefaults)
          .withSource(dataSource.uri())
          .build()
      );
  }

  public Iterable<ConfiguredDataSource<DemExtractParameters>> getDemConfiguredDatasource() {
    return inputData
      .get(DEM)
      .stream()
      .map(it -> new ConfiguredDataSource<>(it, getDemExtractConfig(it)))
      .toList();
  }

  private DemExtractParameters getDemExtractConfig(DataSource dataSource) {
    return buildConfig.dem
      .demExtracts()
      .stream()
      .filter(demExtractConfig -> uriMatch(demExtractConfig.source(), dataSource.uri()))
      .findFirst()
      .orElse(
        new DemExtractParametersBuilder(buildConfig.demDefaults)
          .withSource(dataSource.uri())
          .build()
      );
  }

  public Iterable<ConfiguredDataSource<GtfsFeedParameters>> getGtfsConfiguredDatasource() {
    return inputData
      .get(GTFS)
      .stream()
      .map(it -> new ConfiguredDataSource<>(it, getGtfsFeedConfig(it)))
      .toList();
  }

  private GtfsFeedParameters getGtfsFeedConfig(DataSource dataSource) {
    return buildConfig.transitFeeds
      .gtfsFeeds()
      .stream()
      .filter(gtfsFeedConfig -> uriMatch(gtfsFeedConfig.source(), dataSource.uri()))
      .findFirst()
      .orElse(buildConfig.gtfsDefaults.copyOf().withSource(dataSource.uri()).build());
  }

  public Iterable<ConfiguredDataSource<NetexFeedParameters>> getNetexConfiguredDatasource() {
    return inputData
      .get(NETEX)
      .stream()
      .map(it -> new ConfiguredDataSource<>(it, getNetexConfig(it)))
      .toList();
  }

  public NetexFeedParameters getNetexConfig(DataSource dataSource) {
    return buildConfig.transitFeeds
      .netexFeeds()
      .stream()
      .filter(netexFeedConfig -> uriMatch(netexFeedConfig.source(), dataSource.uri()))
      .findFirst()
      .orElse(buildConfig.netexDefaults.copyOf().withSource(dataSource.uri()).build());
  }

  /**
   * Returns the optional data source for the stop consolidation configuration.
   */
  public Optional<DataSource> stopConsolidation() {
    return store.stopConsolidation();
  }

  /**
   * Match the URI provided in the configuration with the URI of a datasource,
   * either by comparing directly the two URIs or by first prepending the OTP base directory
   * to the URI provided in the configuration.
   * This covers the case where the source parameter provided in the configuration is relative to
   * the base directory.
   */
  private boolean uriMatch(URI configURI, URI datasourceURI) {
    return (
      configURI.equals(datasourceURI) ||
      (
        !configURI.isAbsolute() &&
        baseDirectory.toPath().resolve(configURI.toString()).toUri().equals(datasourceURI)
      )
    );
  }

  public CompositeDataSource getBuildReportDir() {
    return store.getBuildReportDir();
  }

  public File getCacheDirectory() {
    return cacheDirectory;
  }

  /* private methods */

  private boolean hasOneOf(FileType... types) {
    for (FileType type : types) {
      if (has(type)) {
        return true;
      }
    }
    return false;
  }

  private void logSkippedAndSelectedFiles() {
    LOG.info("Data source location(s): {}", String.join(", ", store.getRepositoryDescriptions()));

    // Sort data input files by type
    LOG.info("Existing files expected to be read or written:");
    for (FileType type : FileType.values()) {
      for (DataSource source : inputData.get(type)) {
        LOG.info(BULLET_POINT + "{}", source.detailedInfo());
      }
    }

    if (!skipData.values().isEmpty()) {
      LOG.info("Files excluded due to command line switches or unknown type:");
      for (FileType type : FileType.values()) {
        for (DataSource source : skipData.get(type)) {
          LOG.info(BULLET_POINT + "{}", source.detailedInfo());
        }
      }
    }
  }

  private void validateCliMatchesInputData(CommandLineParameters cli) {
    if (cli.build) {
      if (!hasOneOf(OSM, GTFS, NETEX)) {
        throw new OtpAppException("Unable to build graph, no transit nor OSM data available.");
      }
    } else if (cli.buildStreet) {
      if (!has(OSM)) {
        throw new OtpAppException("Unable to build street graph, no OSM data available.");
      }
    } else if (cli.load) {
      if (!store.getGraph().exists()) {
        throw new OtpAppException(
          "Unable to load graph, no graph file found: %s",
          store.getGraph().path()
        );
      }
    } else if (cli.loadStreet) {
      if (!store.getStreetGraph().exists()) {
        throw new OtpAppException(
          "Unable to load street graph, no street graph file found: %s",
          store.getStreetGraph().path()
        );
      }
      if (!hasOneOf(GTFS, NETEX)) {
        throw new OtpAppException("Unable to build transit graph, no transit data available.");
      }
    }
  }

  private DataSource getOutputGraph(CommandLineParameters cli) {
    if (cli.doSaveGraph()) {
      return store.getGraph();
    } else if (cli.doSaveStreetGraph()) {
      return store.getStreetGraph();
    }
    return null;
  }

  private void include(boolean include, FileType type) {
    // Add or remove type - we do not care if the element already exist or not
    if (include) {
      includeTypes.add(type);
    } else {
      includeTypes.remove(type);
    }
  }

  private void selectFilesToImport() {
    for (FileType type : FileType.values()) {
      if (includeTypes.contains(type)) {
        inputData.putAll(type, store.listExistingSourcesFor(type));
      } else {
        skipData.putAll(type, store.listExistingSourcesFor(type));
      }
    }
  }
}
