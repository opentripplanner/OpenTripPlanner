package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import org.opentripplanner.gtfs.graphbuilder.GtfsFeedParameters;
import org.opentripplanner.gtfs.graphbuilder.GtfsFeedParametersBuilder;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class map GTFS build configuration from JSON to Java objects.
 */
public class GtfsConfig {

  public static GtfsFeedParameters mapGtfsDefaultParameters(
    NodeAdapter root,
    String parameterName
  ) {
    var node = root
      .of(parameterName)
      .since(V2_3)
      .summary("The gtfsDefaults section allows you to specify default properties for GTFS files.")
      .asObject();

    return mapGenericParameters(node);
  }

  public static GtfsFeedParameters mapGtfsFeed(NodeAdapter node, GtfsFeedParameters defaults) {
    return defaults
      .copyOf()
      .withFeedId(
        node
          .of("feedId")
          .since(V2_2)
          .summary(
            "The unique ID for this feed. This overrides any feed ID defined within the feed itself."
          )
          .asString(null)
      )
      .withSource(
        node.of("source").since(V2_2).summary("The unique URI pointing to the data file.").asUri()
      )
      .withRemoveRepeatedStops(
        node
          .of("removeRepeatedStops")
          .since(V2_3)
          .summary("Should consecutive identical stops be merged into one stop time entry")
          .asBoolean(GtfsFeedParameters.DEFAULT_REMOVE_REPEATED_STOPS)
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
          .asEnum(GtfsFeedParameters.DEFAULT_STATION_TRANSFER_PREFERENCE)
      )
      .build();
  }

  private static GtfsFeedParameters mapGenericParameters(NodeAdapter node) {
    return new GtfsFeedParametersBuilder()
      .withRemoveRepeatedStops(
        node
          .of("removeRepeatedStops")
          .since(V2_3)
          .summary("Should consecutive identical stops be merged into one stop time entry")
          .asBoolean(GtfsFeedParameters.DEFAULT_REMOVE_REPEATED_STOPS)
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
          .asEnum(GtfsFeedParameters.DEFAULT_STATION_TRANSFER_PREFERENCE)
      )
      .build();
  }
}
