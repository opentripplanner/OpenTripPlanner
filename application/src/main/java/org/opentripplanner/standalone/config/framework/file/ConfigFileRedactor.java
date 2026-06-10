package org.opentripplanner.standalone.config.framework.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Set;

/**
 * Redacts secret values from JSON configuration nodes before they are logged. This prevents
 * sensitive information like passwords and API keys from appearing in log output.
 */
public class ConfigFileRedactor {

  /** When echoing config files to logs, values for these keys will be hidden. */
  private static final Set<String> REDACT_KEYS = Set.of(
    "secretKey",
    "accessKey",
    "gsCredentials",
    "password"
  );

  private ConfigFileRedactor() {}

  /**
   * Convert the JsonNode to a pretty-printed string with secrets hidden, operating on a protective
   * copy of the node to avoid losing information.
   */
  public static String toRedactedString(JsonNode node) {
    JsonNode redactedNode = node.deepCopy();
    redactSecretsRecursive(redactedNode);
    return redactedNode.toPrettyString();
  }

  /** Note that this method destructively modifies the node and its children in place. */
  private static void redactSecretsRecursive(JsonNode node) {
    if (node.isObject()) {
      node
        .properties()
        .forEach(entry -> {
          if (entry.getValue().isObject() || entry.getValue().isArray()) {
            redactSecretsRecursive(entry.getValue());
          } else if (REDACT_KEYS.contains(entry.getKey())) {
            entry.setValue(new TextNode("********"));
          }
        });
    } else if (node.isArray()) {
      for (JsonNode element : node) {
        redactSecretsRecursive(element);
      }
    }
  }
}
