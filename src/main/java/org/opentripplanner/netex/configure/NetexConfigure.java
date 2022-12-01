package org.opentripplanner.netex.configure;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.ZipFileDataSource;
import org.opentripplanner.graph_builder.ConfiguredDataSource;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.NetexBundle;
import org.opentripplanner.netex.NetexModule;
import org.opentripplanner.netex.config.NetexFeedParameters;
import org.opentripplanner.netex.loader.NetexDataSourceHierarchy;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.BuildConfig;
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
public class NetexConfigure {

  private final BuildConfig buildParams;

  public NetexConfigure(BuildConfig builderParams) {
    this.buildParams = builderParams;
  }

  public static NetexBundle netexBundleForTest(BuildConfig builderParams, File netexZipFile) {
    ZipFileDataSource dataSource = new ZipFileDataSource(netexZipFile, FileType.NETEX);
    var configuredDataSource = new ConfiguredDataSource<>(dataSource, builderParams.netexDefaults);
    return new NetexConfigure(builderParams).netexBundle(configuredDataSource);
  }

  public NetexModule createNetexModule(
    Iterable<ConfiguredDataSource<NetexFeedParameters>> netexSources,
    TransitModel transitModel,
    Graph graph,
    DataImportIssueStore issueStore
  ) {
    List<NetexBundle> netexBundles = new ArrayList<>();

    for (ConfiguredDataSource<NetexFeedParameters> it : netexSources) {
      netexBundles.add(netexBundle(it));
    }

    return new NetexModule(
      graph,
      transitModel,
      issueStore,
      buildParams.getSubwayAccessTimeSeconds(),
      buildParams.getTransitServicePeriod(),
      netexBundles
    );
  }

  /** public to enable testing */
  private NetexBundle netexBundle(ConfiguredDataSource<NetexFeedParameters> configuredDataSource) {
    var source = (CompositeDataSource) configuredDataSource.dataSource();
    var config = configuredDataSource.config();

    return new NetexBundle(
      config.feedId(),
      source,
      hierarchy(source, config),
      config.ferryIdsNotAllowedForBicycle(),
      buildParams.maxStopToShapeSnapDistance,
      config.noTransfersOnIsolatedStops(),
      config.ignoreFareFrame()
    );
  }

  private NetexDataSourceHierarchy hierarchy(
    CompositeDataSource source,
    NetexFeedParameters params
  ) {
    return new NetexDataSourceHierarchy(source)
      .prepare(
        params.ignoreFilePattern(),
        params.sharedFilePattern(),
        params.sharedGroupFilePattern(),
        params.groupFilePattern()
      );
  }
}
