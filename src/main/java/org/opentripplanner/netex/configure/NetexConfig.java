package org.opentripplanner.netex.configure;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.ZipFileDataSource;
import org.opentripplanner.graph_builder.ConfiguredDataSource;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.netex.NetexBundle;
import org.opentripplanner.netex.NetexModule;
import org.opentripplanner.netex.loader.NetexDataSourceHierarchy;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.feed.NetexDefaultParameters;
import org.opentripplanner.standalone.config.feed.NetexFeedParameters;
import org.opentripplanner.standalone.config.feed.NetexFeedParametersBuilder;
import org.opentripplanner.transit.service.TransitModel;

/**
 * Responsible for dependency injection and creating main NeTEx module objects. This decouple the
 * main classes in the netex module, and serve as a single entry-point to create a {@link
 * NetexModule} which simplify the code({@link org.opentripplanner.graph_builder.GraphBuilder})
 * using it.
 * <p>
 * This class inject the build configuration. This way none of the other classes in the
 * `org.opentripplanner.netex` have dependencies to the {@link BuildConfig}.
 * <p>
 * The naming convention used is close to the defacto standard used by Spring.
 */
public class NetexConfig {

  private final BuildConfig buildParams;

  public NetexConfig(BuildConfig builderParams) {
    this.buildParams = builderParams;
  }

  public static NetexBundle netexBundleForTest(BuildConfig builderParams, File netexZipFile) {
    ZipFileDataSource dataSource = new ZipFileDataSource(netexZipFile, FileType.NETEX);
    ConfiguredDataSource<NetexFeedParameters> netexConfiguredDataSource = new ConfiguredDataSource<>(
      dataSource,
      new NetexFeedParametersBuilder().withSource(dataSource.uri()).build()
    );
    return new NetexConfig(builderParams).netexBundle(netexConfiguredDataSource);
  }

  public NetexModule createNetexModule(
    Iterable<ConfiguredDataSource<NetexFeedParameters>> netexSources,
    TransitModel transitModel,
    Graph graph,
    DataImportIssueStore issueStore
  ) {
    List<NetexBundle> netexBundles = new ArrayList<>();

    for (ConfiguredDataSource<NetexFeedParameters> netexConfiguredDataSource : netexSources) {
      netexBundles.add(netexBundle(netexConfiguredDataSource));
    }

    return new NetexModule(
      buildParams.netexDefaults.feedId(),
      graph,
      transitModel,
      issueStore,
      buildParams.getSubwayAccessTimeSeconds(),
      buildParams.getTransitServicePeriod(),
      netexBundles
    );
  }

  /** public to enable testing */
  private NetexBundle netexBundle(
    ConfiguredDataSource<NetexFeedParameters> netexConfiguredDataSource
  ) {
    String configuredFeedId = netexConfiguredDataSource
      .config()
      .feedId()
      .orElse(buildParams.netexDefaults.feedId());

    return new NetexBundle(
      configuredFeedId,
      (CompositeDataSource) netexConfiguredDataSource.dataSource(),
      hierarchy(netexConfiguredDataSource),
      buildParams.netexDefaults.ferryIdsNotAllowedForBicycle(),
      buildParams.maxStopToShapeSnapDistance
    );
  }

  private NetexDataSourceHierarchy hierarchy(
    ConfiguredDataSource<NetexFeedParameters> netexConfiguredDataSource
  ) {
    NetexFeedParameters netexFeedConfig = netexConfiguredDataSource.config();
    NetexDefaultParameters defaults = buildParams.netexDefaults;
    Pattern ignoreFilePattern = netexFeedConfig
      .ignoreFilePattern()
      .orElse(defaults.ignoreFilePattern());
    Pattern sharedFilePattern = netexFeedConfig
      .sharedFilePattern()
      .orElse(defaults.sharedFilePattern());
    Pattern sharedGroupFilePattern = netexFeedConfig
      .sharedGroupFilePattern()
      .orElse(defaults.sharedGroupFilePattern());
    Pattern groupFilePattern = netexFeedConfig
      .groupFilePattern()
      .orElse(defaults.groupFilePattern());

    return new NetexDataSourceHierarchy(
      (CompositeDataSource) netexConfiguredDataSource.dataSource()
    )
      .prepare(ignoreFilePattern, sharedFilePattern, sharedGroupFilePattern, groupFilePattern);
  }
}
