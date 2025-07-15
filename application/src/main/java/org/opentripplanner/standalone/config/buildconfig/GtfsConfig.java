package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import org.opentripplanner.gtfs.config.GtfsDefaultParameters;
import org.opentripplanner.gtfs.config.GtfsFeedParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class map GTFS build configuration from JSON to Java objects.
 */
public class GtfsConfig {

  public static GtfsDefaultParameters mapGtfsDefaultParameters(
    NodeAdapter root,
    String parameterName
  ) {
    var baseDefaults = GtfsFeedParameters.DEFAULT;
    var node = root
      .of(parameterName)
      .since(V2_3)
      .summary("The gtfsDefaults section allows you to specify default properties for GTFS files.")
      .asObject();

    return mapGenericParameters(node, baseDefaults, "");
  }

  public static GtfsFeedParameters mapGtfsFeed(NodeAdapter node, GtfsDefaultParameters defaults) {
    String documentationAddition = " Overrides the value specified in `gtfsDefaults`.";
    var genericParameters = mapGenericParameters(node, defaults, documentationAddition);
    return genericParameters
      .withFeedInfo()
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
      .build();
  }

  private static GtfsDefaultParameters mapGenericParameters(
    NodeAdapter node,
    GtfsDefaultParameters defaults,
    String documentationAddition
  ) {
    var docDefaults = GtfsFeedParameters.DEFAULT;
    return defaults
      .copyOf()
      .withRemoveRepeatedStops(
        node
          .of("removeRepeatedStops")
          .since(V2_3)
          .summary(
            "Should consecutive identical stops be merged into one stop time entry." +
            documentationAddition
          )
          .docDefaultValue(docDefaults.removeRepeatedStops())
          .asBoolean(defaults.removeRepeatedStops())
      )
      .withStationTransferPreference(
        node
          .of("stationTransferPreference")
          .since(V2_3)
          .summary(
            "Should there be some preference or aversion for transfers at stops that are part of a station." +
            documentationAddition
          )
          .description(
            """
            This parameter sets the generic level of preference. What is the actual cost can be changed
            with the `stopBoardAlightDuringTransferCost` parameter in the router configuration.
            """
          )
          .docDefaultValue(docDefaults.stationTransferPreference())
          .asEnum(defaults.stationTransferPreference())
      )
      .withDiscardMinTransferTimes(
        node
          .of("discardMinTransferTimes")
          .since(V2_3)
          .summary(
            "Should minimum transfer times in GTFS files be discarded." + documentationAddition
          )
          .description(
            """
            This is useful eg. when the minimum transfer time is only set for ticketing purposes,
            but we want to calculate the transfers always from OSM data.
            """
          )
          .docDefaultValue(docDefaults.discardMinTransferTimes())
          .asBoolean(defaults.discardMinTransferTimes())
      )
      .withBlockBasedInterlining(
        node
          .of("blockBasedInterlining")
          .since(V2_3)
          .summary(
            "Whether to create stay-seated transfers in between two trips with the same block id." +
            documentationAddition
          )
          .docDefaultValue(docDefaults.blockBasedInterlining())
          .asBoolean(defaults.blockBasedInterlining())
      )
      .withMaxInterlineDistance(
        node
          .of("maxInterlineDistance")
          .since(V2_3)
          .summary(
            "Maximal distance between stops in meters that will connect consecutive trips that are made with same vehicle." +
            documentationAddition
          )
          .docDefaultValue(docDefaults.maxInterlineDistance())
          .asInt(defaults.maxInterlineDistance())
      )
      .build();
  }
}
