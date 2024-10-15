package org.opentripplanner.generate.doc.framework;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;

/**
 * Helper class to build up JSON nodes that can be pretty-printed and inserted into the documentation.
 *
 */
public class JsonExampleBuilder {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private JsonNode node;

  JsonExampleBuilder(JsonNode node) {
    Objects.requireNonNull(node);
    this.node = node;
  }

  public JsonExampleBuilder wrapInObject(String propName) {
    var obj = MAPPER.createObjectNode();
    obj.put(propName, node);
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
