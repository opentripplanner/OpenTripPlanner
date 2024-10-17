package org.opentripplanner.framework.json;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

public class JsonUtils {

  public static Optional<String> asText(JsonNode node, String field) {
    JsonNode valueNode = node.get(field);
    if (valueNode == null) {
      return Optional.empty();
    }
    String value = valueNode.asText();
    return value.isEmpty() ? Optional.empty() : Optional.of(value);
  }
}
