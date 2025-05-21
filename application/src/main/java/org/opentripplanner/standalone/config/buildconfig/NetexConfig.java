package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_6;

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
      .since(V2_2)
      .summary(
        "The netexDefaults section allows you to specify default properties for NeTEx files."
      )
      .asObject();

    return mapDefaultParameters(node);
  }

  static NetexFeedParameters mapNetexFeed(NodeAdapter feedNode, NetexFeedParameters original) {
    return mapFilePatternParameters(feedNode, original)
      .withFeedId(readFeedId(feedNode).asString())
      .withSource(
        feedNode
          .of("source")
          .since(V2_2)
          .summary("The unique URI pointing to the data file.")
          .asUri()
      )
      .build();
  }

  private static NetexFeedParameters mapDefaultParameters(NodeAdapter config) {
    // FeedId is optional for the default settings
    var feedId = readFeedId(config).asString(NetexFeedParameters.DEFAULT.feedId());

    return mapFilePatternParameters(config, NetexFeedParameters.DEFAULT).withFeedId(feedId).build();
  }

  private static NetexFeedParameters.Builder mapFilePatternParameters(
    NodeAdapter config,
    NetexFeedParameters base
  ) {
    var dft = NetexFeedParameters.DEFAULT;
    return base
      .copyOf()
      .withSharedFilePattern(
        config
          .of("sharedFilePattern")
          .since(V2_0)
          .summary("Pattern for matching shared NeTEx files in a NeTEx bundle.")
          .description(
            """
            This field is used to match *shared files*(zip file entries) in the module file. Shared
            files are loaded first. Then the rest of the files are grouped and loaded.

            The pattern `"shared-data.xml"` matches `"shared-data.xml"`

            File names are matched in the following order - and treated accordingly to the first match:

             - `ignoreFilePattern`
             - `sharedFilePattern`
             - `sharedGroupFilePattern`
             - `groupFilePattern`
            """
          )
          .docDefaultValue(dft.sharedFilePattern().pattern())
          .asPattern(base.sharedFilePattern().pattern())
      )
      .withSharedGroupFilePattern(
        config
          .of("sharedGroupFilePattern")
          .since(V2_0)
          .summary("Pattern for matching shared group NeTEx files in a NeTEx bundle.")
          .description(
            """
            This field is used to match *shared group files* in the module file (zip file entries).
            Typically this is used to group all files from one agency together.

            *Shared group files* are loaded after shared files, but before the matching group
            files. Each *group* of files are loaded as a unit, followed by next group.

            Files are grouped together by the first group pattern in the regular expression.

            The pattern `"(\\w{3})-.*-shared\\.xml"` matches `"RUT-shared.xml"` with group `"RUT"`.
            """
          )
          .docDefaultValue(dft.sharedGroupFilePattern().pattern())
          .asPattern(base.sharedGroupFilePattern().pattern())
      )
      .withGroupFilePattern(
        config
          .of("groupFilePattern")
          .since(V2_0)
          .summary("Pattern for matching group NeTEx files.")
          .description(
            """
            This field is used to match *group files* in the module file (zip file entries).
            *group files* are loaded right the after *shared group files* are loaded.
            Files are grouped together by the first group pattern in the regular expression.
            The pattern `"(\\w{3})-.*\\.xml"` matches `"RUT-Line-208-Hagalia-Nevlunghavn.xml"`
            with group `"RUT"`.
            """
          )
          .docDefaultValue(dft.groupFilePattern().pattern())
          .asPattern(base.groupFilePattern().pattern())
      )
      .withIgnoreFilePattern(
        config
          .of("ignoreFilePattern")
          .since(V2_0)
          .summary("Pattern for matching ignored files in a NeTEx bundle.")
          .description(
            """
            This field is used to exclude matching *files* in the module file (zip file entries).
            The *ignored* files are *not* loaded.
            """
          )
          .docDefaultValue(dft.ignoreFilePattern().pattern())
          .asPattern(base.ignoreFilePattern().pattern())
      )
      .withNoTransfersOnIsolatedStops(
        config
          .of("noTransfersOnIsolatedStops")
          .since(V2_2)
          .summary(
            "Whether we should allow transfers to and from StopPlaces marked with LimitedUse.ISOLATED"
          )
          .docDefaultValue(dft.noTransfersOnIsolatedStops())
          .asBoolean(base.noTransfersOnIsolatedStops())
      )
      .addFerryIdsNotAllowedForBicycle(
        config
          .of("ferryIdsNotAllowedForBicycle")
          .since(V2_0)
          .summary("List ferries which do not allow bikes.")
          .description(
            """
            Bicycles are allowed on most ferries however the Nordic profile doesn't contain a place
            where bicycle conveyance can be defined.

            For this reason we allow bicycles on ferries by default and allow to override the rare
            case where this is not the case.
            """
          )
          .docDefaultValue(dft.ferryIdsNotAllowedForBicycle())
          .asStringSet(base.ferryIdsNotAllowedForBicycle())
      )
      .withIgnoreFareFrame(
        config
          .of("ignoreFareFrame")
          .since(V2_3)
          .summary("Ignore contents of the FareFrame")
          .docDefaultValue(base.ignoreFareFrame())
          .asBoolean(base.ignoreFareFrame())
      )
      .withIgnoreParking(
        config
          .of("ignoreParking")
          .since(V2_6)
          .summary("Ignore Parking elements.")
          .docDefaultValue(base.ignoreParking())
          .asBoolean(base.ignoreParking())
      );
  }

  /** Provide common documentation for the default and feed specific 'feedId'. */
  private static ParameterBuilder readFeedId(NodeAdapter config) {
    return config
      .of("feedId")
      .since(V2_2)
      .summary(
        "This field is used to identify the specific NeTEx feed. It is used instead of " +
        "the feed_id field in GTFS file feed_info.txt."
      );
  }
}
