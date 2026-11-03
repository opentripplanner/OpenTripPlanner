package org.opentripplanner.framework.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

class JsonUtilsTest {

  private static final ObjectMapper MAPPER = new JsonMapper();

  @Test
  void testAsText() throws JacksonException {
    assertTrue(JsonUtils.asText(MissingNode.getInstance(), "any").isEmpty());
    assertTrue(JsonUtils.asText(NullNode.getInstance(), "any").isEmpty());
    assertTrue(JsonUtils.asText(new StringNode("foo"), "bar").isEmpty());

    JsonNode node = MAPPER.readTree(
      """
      { "foo" : "bar", "array" : [] }
      """
    );

    Optional<String> result = JsonUtils.asText(node, "foo");
    assertTrue(result.isPresent());
    assertEquals("bar", result.get());

    assertTrue(JsonUtils.asText(node, "array").isEmpty());
  }
}
