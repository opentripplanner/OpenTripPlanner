package org.opentripplanner.modularization.harness;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

/** Finds the differences between two captures, value by value. */
public final class Differ {

  /** Marker used when a value exists on one side only. */
  public static final String ABSENT = "<absent>";

  private Differ() {}

  /**
   * Compare all captured scenario files in two directories. Files whose name starts with an
   * underscore (logs, metadata) are not compared.
   */
  public static List<Difference> compareDirectories(Path baseline, Path candidate) {
    var names = new TreeSet<String>();
    names.addAll(scenarioNames(baseline));
    names.addAll(scenarioNames(candidate));

    var result = new ArrayList<Difference>();
    for (String name : names) {
      Path b = baseline.resolve(name + ".json");
      Path c = candidate.resolve(name + ".json");
      if (!Files.exists(b) || !Files.exists(c)) {
        result.add(
          new Difference(
            name,
            "<file>",
            Files.exists(b) ? "present" : ABSENT,
            Files.exists(c) ? "present" : ABSENT
          )
        );
        continue;
      }
      result.addAll(compare(name, read(b), read(c)));
    }
    return result;
  }

  /** Compare two JSON documents. The result is sorted by path. */
  public static List<Difference> compare(String scenario, JsonNode baseline, JsonNode candidate) {
    var b = flatten(baseline);
    var c = flatten(candidate);
    var paths = new TreeSet<String>();
    paths.addAll(b.keySet());
    paths.addAll(c.keySet());

    var result = new ArrayList<Difference>();
    for (String path : paths) {
      String bv = b.getOrDefault(path, ABSENT);
      String cv = c.getOrDefault(path, ABSENT);
      if (!bv.equals(cv)) {
        result.add(new Difference(scenario, path, bv, cv));
      }
    }
    return result;
  }

  /** Flatten a document into {@code path -> JSON text of the leaf value}. */
  static Map<String, String> flatten(JsonNode node) {
    var out = new LinkedHashMap<String, String>();
    flatten(node, "", out);
    return out;
  }

  private static void flatten(JsonNode node, String path, Map<String, String> out) {
    if (node.isObject() && !node.isEmpty()) {
      node.fieldNames().forEachRemaining(k -> flatten(node.get(k), Json.childPath(path, k), out));
    } else if (node.isArray() && !node.isEmpty()) {
      for (int i = 0; i < node.size(); i++) {
        flatten(node.get(i), path + "[" + i + "]", out);
      }
    } else {
      out.put(path, node.toString());
    }
  }

  private static TreeSet<String> scenarioNames(Path dir) {
    var names = new TreeSet<String>();
    if (!Files.isDirectory(dir)) {
      return names;
    }
    try (Stream<Path> files = Files.list(dir)) {
      files
        .map(p -> p.getFileName().toString())
        .filter(n -> n.endsWith(".json") && !n.startsWith("_"))
        .forEach(n -> names.add(n.substring(0, n.length() - ".json".length())));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return names;
  }

  private static JsonNode read(Path file) {
    try {
      return Json.MAPPER.readTree(file.toFile());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
