package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import java.util.List;
import org.opentripplanner.graph_builder.model.DataSourceConfig;
import org.opentripplanner.gtfs.graphbuilder.GtfsFeedParameters;
import org.opentripplanner.gtfs.graphbuilder.GtfsFeedParametersBuilder;
import org.opentripplanner.netex.config.NetexFeedParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.transit.model.site.StopTransferPriority;

public class TransitFeedConfig {

  public static TransitFeeds mapTransitFeeds(
    NodeAdapter root,
    String parameterName,
    NetexFeedParameters netexDefaults
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
      .asObjects(node -> TransitFeedConfig.mapTransitFeed(node, netexDefaults));

    return new TransitFeeds(
      filterListOnSubType(list, GtfsFeedParameters.class),
      filterListOnSubType(list, NetexFeedParameters.class)
    );
  }

  private static DataSourceConfig mapTransitFeed(
    NodeAdapter feedNode,
    NetexFeedParameters netexDefaults
  ) {
    var type = feedNode
      .of("type")
      .since(V2_2)
      .summary("The feed input format.")
      .asEnum(TransitFeedType.class);
    return switch (type) {
      case GTFS -> mapGtfsFeed(feedNode);
      case NETEX -> NetexConfig.mapNetexFeed(feedNode, netexDefaults);
    };
  }

  private static DataSourceConfig mapGtfsFeed(NodeAdapter node) {
    return new GtfsFeedParametersBuilder()
      .withFeedId(
        node
          .of("feedId")
          .since(NA)
          .summary(
            "The unique ID for this feed. This overrides any feed ID defined within the feed itself."
          )
          .asString(null)
      )
      .withSource(
        node.of("source").since(NA).summary("The unique URI pointing to the data file.").asUri()
      )
      .withRemoveRepeatedStops(
        node
          .of("removeRepeatedStops")
          .since(V2_3)
          .summary("Should consecutive identical stops be merged into one stop time entry")
          .asBoolean(true)
      )
      .withStationTransferPreference(
        node
          .of("stationTransferPreference")
          .since(V2_3)
          .summary(
            "Should there be some preference or aversion for transfers at stops that are part of a station."
          )
          .description(
            """
            This parameter sets the generic level of preference. What is the actual cost can be changed
            with the `stopTransferCost` parameter in the router configuration.
            """
          )
          .asEnum(StopTransferPriority.ALLOWED)
      )
      .build();
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
