package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;

import java.util.List;
import org.opentripplanner.graph_builder.model.DataSourceConfig;
import org.opentripplanner.gtfs.config.GtfsDefaultParameters;
import org.opentripplanner.gtfs.config.GtfsFeedParameters;
import org.opentripplanner.netex.config.NetexFeedParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class TransitFeedConfig {

  public static TransitFeeds mapTransitFeeds(
    NodeAdapter root,
    String parameterName,
    NetexFeedParameters netexDefaults,
    GtfsDefaultParameters gtfsDefaults
  ) {
    List<DataSourceConfig> list = root
      .of(parameterName)
      .since(V2_2)
      .summary("Scan for transit data files")
      .description(
        """
        The transitFeeds section of `build-config.json` allows you to override the default behavior
        of scanning for transit data files in the [base directory](Configuration.md#Base-Directory).
        You can specify data located outside the local filesystem (including cloud storage services)
        or at various different locations around the local filesystem.

        When a feed of a particular type (`netex` or `gtfs`) is specified in the transitFeeds
        section, auto-scanning in the base directory for this feed type will be disabled.
        """
      )
      .asObjects(node -> TransitFeedConfig.mapTransitFeed(node, netexDefaults, gtfsDefaults));

    return new TransitFeeds(
      filterListOnSubType(list, GtfsFeedParameters.class),
      filterListOnSubType(list, NetexFeedParameters.class)
    );
  }

  private static DataSourceConfig mapTransitFeed(
    NodeAdapter feedNode,
    NetexFeedParameters netexDefaults,
    GtfsDefaultParameters gtfsDefaults
  ) {
    var type = feedNode
      .of("type")
      .since(V2_2)
      .summary("The feed input format.")
      .asEnum(TransitFeedType.class);
    return switch (type) {
      case GTFS -> GtfsConfig.mapGtfsFeed(feedNode, gtfsDefaults);
      case NETEX -> NetexConfig.mapNetexFeed(feedNode, netexDefaults);
    };
  }

  @SuppressWarnings("unchecked")
  private static <T> List<T> filterListOnSubType(List<? super T> list, Class<T> type) {
    return list
      .stream()
      .filter(it -> type.isAssignableFrom(it.getClass()))
      .map(it -> (T) it)
      .toList();
  }

  enum TransitFeedType {
    GTFS,
    NETEX,
  }
}
