package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;

import java.util.List;
import org.opentripplanner.graph_builder.model.DataSourceConfig;
import org.opentripplanner.gtfs.graphbuilder.GtfsFeedParameters;
import org.opentripplanner.gtfs.graphbuilder.GtfsFeedParametersBuilder;
import org.opentripplanner.netex.config.NetexFeedParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class TransitFeedConfig {

  public static TransitFeeds mapTransitFeeds(
    NodeAdapter root,
    String parameterName,
    NetexFeedParameters netexDefaults
  ) {
    List<DataSourceConfig> list = root
      .of(parameterName)
      .withDoc(NA, /*TODO DOC*/"TODO")
      .withExample(/*TODO DOC*/"TODO")
      .withDescription(/*TODO DOC*/"TODO")
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
      .withDoc(V2_2, "The feed input format.")
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
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asString(null)
      )
      .withSource(
        node.of("source").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asUri()
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
