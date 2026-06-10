package org.opentripplanner.framework.json;

import java.util.Optional;
import tools.jackson.databind.JsonNode;

public class JsonUtils {

  public static Optional<String> asText(JsonNode node, String field) {
    JsonNode valueNode = node.get(field);
    if (valueNode == null) {
      return Optional.empty();
    } else if (valueNode.isString()) {
      return Optional.of(valueNode.asString());
    } else {
      return Optional.empty();
    }
  }
}
