package org.opentripplanner.ext.gbfsgeofencing.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_8;

import java.util.List;
import org.opentripplanner.ext.gbfsgeofencing.parameters.GbfsGeofencingFeedParameters;
import org.opentripplanner.ext.gbfsgeofencing.parameters.GbfsGeofencingParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.spi.HttpHeaders;

/**
 * This class is responsible for mapping GBFS geofencing configuration into parameters.
 */
public class GbfsGeofencingConfig {

  public static GbfsGeofencingParameters mapConfig(String parameterName, NodeAdapter root) {
    var c = root
      .of(parameterName)
      .since(V2_8)
      .summary("Load GBFS geofencing zones at graph build time.")
      .description(
        """
        This sandbox feature allows loading GBFS geofencing zones during graph build instead of
        at runtime. This is useful when geofencing zones are relatively static and you want to
        avoid external dependencies at runtime.

        Note: If both build-time and runtime geofencing are enabled for the same network,
        zones will be applied twice.
        """
      )
      .asObject();

    if (c.isEmpty()) {
      return new GbfsGeofencingParameters(List.of());
    }

    return new GbfsGeofencingParameters(mapFeeds(c));
  }

  private static List<GbfsGeofencingFeedParameters> mapFeeds(NodeAdapter config) {
    return config
      .of("feeds")
      .since(V2_8)
      .summary("List of GBFS feeds to load geofencing zones from.")
      .asObjects(List.of(), GbfsGeofencingConfig::mapFeed);
  }

  private static GbfsGeofencingFeedParameters mapFeed(NodeAdapter node) {
    return new GbfsGeofencingFeedParameters(
      node.of("url").since(V2_8).summary("URL of the GBFS feed (gbfs.json endpoint).").asString(),
      node
        .of("network")
        .since(V2_8)
        .summary("Network identifier. If not provided, extracted from GBFS system_id.")
        .asString(null),
      HttpHeaders.of(
        node
          .of("httpHeaders")
          .since(V2_8)
          .summary("HTTP headers to include in GBFS requests.")
          .asStringMap()
      )
    );
  }
}
