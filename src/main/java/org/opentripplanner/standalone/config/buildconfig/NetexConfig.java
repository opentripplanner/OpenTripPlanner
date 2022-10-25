package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;

import org.opentripplanner.netex.config.NetexFeedParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.ParameterBuilder;

/**
 * This class map Netex build configuration from JSON to Java objects.
 */
public class NetexConfig {

  public static NetexFeedParameters mapNetexDefaultParameters(
    NodeAdapter root,
    String parameterName
  ) {
    var node = root
      .of(parameterName)
      .withDoc(NA, /*TODO DOC*/"TODO")
      .withDescription(/*TODO DOC*/"TODO")
      .asObject();

    return mapDefaultParameters(node);
  }

  static NetexFeedParameters mapNetexFeed(NodeAdapter feedNode, NetexFeedParameters original) {
    return mapFilePatternParameters(feedNode, original)
      .withFeedId(readFeedId(feedNode).asString())
      .withSource(feedNode.of("source").withDoc(NA, /*TODO DOC*/"TODO").asUri())
      .build();
  }

  private static NetexFeedParameters mapDefaultParameters(NodeAdapter config) {
    // FeedId is optional for the default settings
    var feedId = readFeedId(config).asString(NetexFeedParameters.DEFAULT.feedId());

    return mapFilePatternParameters(config, NetexFeedParameters.DEFAULT).withFeedId(feedId).build();
  }

  private static NetexFeedParameters.Builder mapFilePatternParameters(
    NodeAdapter config,
    NetexFeedParameters original
  ) {
    return original
      .copyOf()
      .withSharedFilePattern(
        config
          .of("sharedFilePattern")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asPattern(original.sharedFilePattern().pattern())
      )
      .withSharedGroupFilePattern(
        config
          .of("sharedGroupFilePattern")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asPattern(original.sharedGroupFilePattern().pattern())
      )
      .withGroupFilePattern(
        config
          .of("groupFilePattern")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asPattern(original.groupFilePattern().pattern())
      )
      .withIgnoreFilePattern(
        config
          .of("ignoreFilePattern")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asPattern(original.ignoreFilePattern().pattern())
      )
      .withNoTransfersOnIsolatedStops(
        config
          .of("noTransfersOnIsolatedStops")
          .withDoc(
            V2_2,
            "Whether we should allow transfers to and from StopPlaces marked with LimitedUse.ISOLATED"
          )
          .asBoolean(original.noTransfersOnIsolatedStops())
      )
      .addFerryIdsNotAllowedForBicycle(
        config
          .of("ferryIdsNotAllowedForBicycle")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asStringSet(original.ferryIdsNotAllowedForBicycle())
      );
  }

  /** Provide common documentation for the default and feed specific 'feedId'. */
  private static ParameterBuilder readFeedId(NodeAdapter config) {
    return config
      .of("feedId")
      .withDoc(
        V2_2,
        "This field is used to identify the specific NeTEx feed. It is used instead of " +
        "the feed_id field in GTFS file feed_info.txt."
      );
  }
}
