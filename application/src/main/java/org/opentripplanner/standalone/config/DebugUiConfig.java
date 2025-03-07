package org.opentripplanner.standalone.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_7;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.util.List;
import org.opentripplanner.standalone.config.debuguiconfig.BackgroundTileLayer;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an object representation of the 'debug-ui-config.json'.
 */
public class DebugUiConfig {

  private static final Logger LOG = LoggerFactory.getLogger(DebugUiConfig.class);

  public static final DebugUiConfig DEFAULT = new DebugUiConfig(
    MissingNode.getInstance(),
    "DEFAULT",
    false
  );

  /**
   * The node adaptor kept for reference and (de)serialization.
   */
  private final NodeAdapter root;
  private final List<BackgroundTileLayer> additionalBackgroundLayers;

  public DebugUiConfig(JsonNode node, String source, boolean logUnusedParams) {
    this(new NodeAdapter(node, source), logUnusedParams);
  }

  /** protected to give unit-test access */
  DebugUiConfig(NodeAdapter root, boolean logUnusedParams) {
    this.root = root;

    this.additionalBackgroundLayers = root
      .of("additionalBackgroundLayers")
      .since(V2_7)
      .summary("Additional background raster map layers.")
      .description(
        """
        Add additional background layers that will appear in the Debug UI as one of the choices.

        Currently only raster tile layers are supported.
        """
      )
      .asObjects(List.of(), node ->
        new BackgroundTileLayer(
          node.of("name").since(V2_7).summary("Name to appear in the layer selector.").asString(),
          node
            .of("templateUrl")
            .since(V2_7)
            .summary(
              """
              The [Maplibre-compatible template URL](https://maplibre.org/maplibre-native/ios/api/tile-url-templates.html)
              for the raster layer, for example `https://examples.com/tiles/{z}/{x}/{y}.png`.
              """
            )
            .asString(),
          node.of("tileSize").since(V2_7).summary("Size of the tile in pixels.").asInt(256),
          node
            .of("attribution")
            .since(V2_7)
            .summary("Attribution for the map data.")
            .asString("© OpenTripPlanner")
        )
      );

    if (logUnusedParams) {
      root.logAllWarnings(LOG::warn);
    }
  }

  public NodeAdapter asNodeAdapter() {
    return root;
  }

  public List<BackgroundTileLayer> additionalBackgroundLayers() {
    return additionalBackgroundLayers;
  }

  /**
   * If {@code true} the config is loaded from file, in not the DEFAULT config is used.
   */
  public boolean isDefault() {
    return root.isEmpty();
  }

  /**
   * Checks if any unknown or invalid parameters were encountered while loading the configuration.
   */
  public boolean hasUnknownParameters() {
    return root.hasUnknownParameters();
  }
}
