package org.opentripplanner.framework.json;

import java.util.Optional;
import tools.jackson.databind.JsonNode;

public class JsonUtils {

  public static Optional<String> asText(JsonNode node, String field) {
    JsonNode valueNode = node.get(field);
    if (valueNode == null) {
      return Optional.empty();
    }
    String value = valueNode.asString();
    return value.isEmpty() ? Optional.empty() : Optional.of(value);
  }
}
