package org.opentripplanner.modularization.harness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/** JSON helpers: the shared mapper, normalization of responses, and path construction. */
public final class Json {

  public static final ObjectMapper MAPPER = new ObjectMapper().enable(
    SerializationFeature.INDENT_OUTPUT
  );

  private Json() {}

  /**
   * Normalize a response so two equal answers give identical text:
   * <ul>
   *   <li>object keys are sorted</li>
   *   <li>nodes with a path matching one of the ignore globs are removed</li>
   *   <li>if {@code sortArraysBy} is set, arrays of objects that all have that text field are
   *   sorted by it (used for introspection results where the order has no meaning)</li>
   *   <li>arrays with a path matching one of the {@code unordered} globs are sorted by the text
   *   of their normalized elements. Use it where the API gives no ordering guarantee.</li>
   * </ul>
   */
  public static JsonNode normalize(
    JsonNode node,
    List<Glob> ignore,
    String sortArraysBy,
    List<Glob> unordered
  ) {
    return normalize(node, "", ignore, sortArraysBy, unordered);
  }

  private static JsonNode normalize(
    JsonNode node,
    String path,
    List<Glob> ignore,
    String sortArraysBy,
    List<Glob> unordered
  ) {
    if (node.isObject()) {
      var out = JsonNodeFactory.instance.objectNode();
      var keys = new TreeSet<String>();
      node.fieldNames().forEachRemaining(keys::add);
      for (String key : keys) {
        String childPath = childPath(path, key);
        if (isIgnored(childPath, ignore)) {
          continue;
        }
        out.set(key, normalize(node.get(key), childPath, ignore, sortArraysBy, unordered));
      }
      return out;
    }
    if (node.isArray()) {
      List<JsonNode> items = new ArrayList<>();
      for (JsonNode item : node) {
        items.add(item);
      }
      if (sortArraysBy != null && !items.isEmpty() && allHaveTextField(items, sortArraysBy)) {
        items.sort(Comparator.comparing(i -> i.get(sortArraysBy).asText()));
      }
      boolean isUnordered = unordered.stream().anyMatch(g -> g.matches(path));
      List<JsonNode> normalized = new ArrayList<>();
      for (int i = 0; i < items.size(); i++) {
        // Elements of unordered arrays have no stable index, so a '*' stands in for it
        String childPath = path + (isUnordered ? "[*]" : "[" + i + "]");
        if (!isIgnored(childPath, ignore)) {
          normalized.add(normalize(items.get(i), childPath, ignore, sortArraysBy, unordered));
        }
      }
      if (isUnordered) {
        normalized.sort(Comparator.comparing(JsonNode::toString));
      }
      ArrayNode out = JsonNodeFactory.instance.arrayNode();
      normalized.forEach(out::add);
      return out;
    }
    return node;
  }

  static String childPath(String parent, String key) {
    return parent.isEmpty() ? key : parent + "." + key;
  }

  private static boolean isIgnored(String path, List<Glob> ignore) {
    return ignore.stream().anyMatch(g -> g.matches(path));
  }

  private static boolean allHaveTextField(List<JsonNode> items, String field) {
    return items
      .stream()
      .allMatch(i -> i.isObject() && i.has(field) && i.get(field).isTextual());
  }

  static ObjectNode object() {
    return JsonNodeFactory.instance.objectNode();
  }
}
