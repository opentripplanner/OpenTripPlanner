package org.opentripplanner.framework.json;

import java.util.Optional;
import tools.jackson.databind.JsonNode;

public class JsonUtils {

  /**
   * Return the value of the given field if it is a JSON string. An empty string is returned as
   * {@code Optional.of("")}. If the field is missing or not a string (number, boolean, object
   * etc.) an empty optional is returned.
   */
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
