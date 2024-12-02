package org.opentripplanner.standalone.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_7;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.io.Serializable;
import java.util.List;
import org.opentripplanner.standalone.config.debuguiconfig.BackgroundTileLayer;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an object representation of the 'debug-ui-config.json'.
 */
public class DebugUiConfig implements Serializable {

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

    this.additionalBackgroundLayers =
      root
        .of("additionalBackgroundLayers")
        .asObjects(
          List.of(),
          node ->
            new BackgroundTileLayer(
              node
                .of("name")
                .since(V2_7)
                .summary(
                  "Used in the url to fetch tiles, and as the layer name in the vector tiles."
                )
                .asString(),
              node
                .of("templateUrl")
                .since(V2_7)
                .summary("Maximum zoom levels the layer is active for.")
                .asString(),
              node
                .of("tileSize")
                .since(V2_7)
                .summary("Minimum zoom levels the layer is active for.")
                .asInt(256),
              node
                .of("attribution")
                .since(V2_7)
                .summary("Minimum zoom levels the layer is active for.")
                .asString("© OpenTripPlanner")
            )
        );

    if (logUnusedParams && LOG.isWarnEnabled()) {
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
