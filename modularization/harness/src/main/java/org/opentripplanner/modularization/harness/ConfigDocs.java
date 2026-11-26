package org.opentripplanner.modularization.harness;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Captures the generated configuration documentation of an OTP source tree.
 * <p>
 * OTP generates the parameter documentation from the configuration code, and checks the generated
 * files into {@code doc/user}. The templates in {@code doc/templates} tell which files are
 * generated. The documentation is the contract of the configuration: parameter names, types,
 * defaults, descriptions and the example configurations. Each document is stored as JSON with
 * one entry per section, so a change is reported per section and not per line.
 */
final class ConfigDocs {

  private ConfigDocs() {}

  /** Capture all generated documents of the tree into {@code out/<path>.json}. */
  static int capture(Path tree, Path out) throws IOException {
    Path templates = tree.resolve("doc/templates");
    Path docs = tree.resolve("doc/user");
    if (!Files.isDirectory(templates) || !Files.isDirectory(docs)) {
      throw new IllegalArgumentException("Not an OTP source tree: " + tree);
    }
    List<Path> generated;
    try (Stream<Path> files = Files.walk(templates)) {
      generated = files
        .filter(Files::isRegularFile)
        .map(templates::relativize)
        .filter(p -> p.toString().endsWith(".md"))
        .sorted()
        .toList();
    }
    int count = 0;
    // The OSM mapper template is the source of one document per mapper in doc/user/osm.
    Path osm = docs.resolve("osm");
    if (Files.isDirectory(osm)) {
      try (Stream<Path> files = Files.list(osm)) {
        for (Path doc : files.filter(p -> p.toString().endsWith(".md")).sorted().toList()) {
          count += write(doc, out.resolve("osm"));
        }
      }
    }
    for (Path rel : generated) {
      if (rel.getFileName().toString().equals("OsmMapper.md")) {
        continue;
      }
      Path doc = findDocument(docs, rel);
      if (doc == null) {
        System.err.println("No generated document found for template " + rel);
        continue;
      }
      count += write(doc, out.resolve(rel.getParent() == null ? Path.of("") : rel.getParent()));
    }
    return count;
  }

  private static int write(Path doc, Path outDir) throws IOException {
    String name = doc.getFileName().toString();
    Path target = outDir.resolve(name.substring(0, name.length() - ".md".length()) + ".json");
    Files.createDirectories(outDir);
    Json.MAPPER.writerWithDefaultPrettyPrinter().writeValue(
      target.toFile(),
      toSections(Files.readString(doc))
    );
    return 1;
  }

  /**
   * The generated document has the path of its template, or else the same file name somewhere
   * else under {@code doc/user}. Not unique means not found.
   */
  private static Path findDocument(Path docs, Path rel) throws IOException {
    Path direct = docs.resolve(rel);
    if (Files.exists(direct)) {
      return direct;
    }
    try (Stream<Path> files = Files.walk(docs)) {
      var matches = files
        .filter(Files::isRegularFile)
        .filter(p -> p.getFileName().equals(rel.getFileName()))
        .toList();
      return matches.size() == 1 ? matches.get(0) : null;
    }
  }

  /**
   * Split a Markdown document into sections keyed by the path of its headings. Lines inside code
   * fences are not headings. A repeated heading path gets a {@code #2}, {@code #3}, ... suffix.
   */
  static ObjectNode toSections(String markdown) {
    var sections = new LinkedHashMap<String, StringBuilder>();
    var stack = new ArrayList<String>();
    String key = "<top>";
    sections.put(key, new StringBuilder());
    boolean fenced = false;
    for (String line : markdown.split("\n", -1)) {
      if (line.stripLeading().startsWith("```")) {
        fenced = !fenced;
      }
      if (!fenced && line.startsWith("#")) {
        int level = 0;
        while (level < line.length() && line.charAt(level) == '#') {
          level++;
        }
        if (level < line.length() && line.charAt(level) == ' ') {
          while (stack.size() >= level) {
            stack.remove(stack.size() - 1);
          }
          while (stack.size() < level - 1) {
            stack.add("");
          }
          stack.add(line.substring(level + 1).trim());
          String base = String.join(" > ", stack);
          key = base;
          for (int n = 2; sections.containsKey(key); n++) {
            key = base + " #" + n;
          }
          sections.put(key, new StringBuilder());
          continue;
        }
      }
      sections.get(key).append(line).append('\n');
    }
    ObjectNode node = Json.MAPPER.createObjectNode();
    for (Map.Entry<String, StringBuilder> e : sections.entrySet()) {
      String text = e.getValue().toString().strip();
      if (!text.isEmpty() || !e.getKey().equals("<top>")) {
        node.put(e.getKey(), text);
      }
    }
    return node;
  }
}
