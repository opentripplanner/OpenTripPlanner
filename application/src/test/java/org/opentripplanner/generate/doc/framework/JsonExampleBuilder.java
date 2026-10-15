package org.opentripplanner.generate.doc.framework;

import java.util.Objects;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Helper class to build up JSON nodes that can be pretty-printed and inserted into the documentation.
 *
 */
public class JsonExampleBuilder {

  private static final ObjectMapper MAPPER = new JsonMapper();
  private JsonNode node;

  JsonExampleBuilder(JsonNode node) {
    Objects.requireNonNull(node);
    this.node = node;
  }

  public JsonExampleBuilder wrapInObject(String propName) {
    var obj = MAPPER.createObjectNode();
    obj.set(propName, node);
    node = obj;
    return this;
  }

  public JsonExampleBuilder wrapInArray() {
    var array = MAPPER.createArrayNode();
    array.add(node);
    node = array;
    return this;
  }

  public JsonNode build() {
    return node;
  }
}
