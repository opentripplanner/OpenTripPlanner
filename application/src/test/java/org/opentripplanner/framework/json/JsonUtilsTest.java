package org.opentripplanner.framework.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JsonUtilsTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void testAsText() throws JsonProcessingException {
    assertTrue(JsonUtils.asText(MissingNode.getInstance(), "any").isEmpty());
    assertTrue(JsonUtils.asText(NullNode.getInstance(), "any").isEmpty());
    assertTrue(JsonUtils.asText(new TextNode("foo"), "bar").isEmpty());

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
