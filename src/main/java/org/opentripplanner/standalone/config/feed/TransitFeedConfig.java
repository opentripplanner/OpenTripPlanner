package org.opentripplanner.standalone.config.feed;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;

import java.util.List;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class TransitFeedConfig {

  public static TransitFeedParametersList mapTransitFeeds(NodeAdapter root, String parameterName) {
    List<TransitFeedParameters> list = root
      .of(parameterName)
      .withDoc(NA, /*TODO DOC*/"TODO")
      .withExample(/*TODO DOC*/"TODO")
      .withDescription(/*TODO DOC*/"TODO")
      .asObjects(TransitFeedConfig::mapTransitFeed);

    return new TransitFeedParametersList(
      filterListOnSubType(list, GtfsFeedParameters.class),
      filterListOnSubType(list, NetexFeedParameters.class)
    );
  }

  private static TransitFeedParameters mapTransitFeed(NodeAdapter feedNode) {
    var type = feedNode
      .of("type")
      .withDoc(V2_2, "The feed input format.")
      .asEnum(TransitFeedType.class);
    return switch (type) {
      case GTFS -> mapGtfsFeed(feedNode);
      case NETEX -> mapNetexFeed(feedNode);
    };
  }

  private static TransitFeedParameters mapGtfsFeed(NodeAdapter node) {
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

  private static TransitFeedParameters mapNetexFeed(NodeAdapter feedNode) {
    return new NetexFeedParametersBuilder()
      .withFeedId(
        feedNode
          .of("feedId")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asString(null)
      )
      .withSource(
        feedNode
          .of("source")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asUri()
      )
      .withSharedFilePattern(
        feedNode.of("sharedFilePattern").withDoc(NA, /*TODO DOC*/"TODO").asPattern(null)
      )
      .withSharedGroupFilePattern(
        feedNode.of("sharedGroupFilePattern").withDoc(NA, /*TODO DOC*/"TODO").asPattern(null)
      )
      .withIgnoreFilePattern(
        feedNode.of("ignoreFilePattern").withDoc(NA, /*TODO DOC*/"TODO").asPattern(null)
      )
      .withGroupFilePattern(
        feedNode.of("groupFilePattern").withDoc(NA, /*TODO DOC*/"TODO").asPattern(null)
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
