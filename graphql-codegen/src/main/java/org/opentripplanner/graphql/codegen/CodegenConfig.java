package org.opentripplanner.graphql.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * The code generator configuration. It uses the same YAML format as the JavaScript
 * {@code graphql-codegen}, so existing configuration files can be reused unchanged. Only the
 * options OTP uses are supported:
 * <ul>
 *   <li>{@code schema}: the schema file, relative to the configuration file</li>
 *   <li>{@code generates}: the output files, each with a list of {@code plugins} ({@code java}
 *   or {@code java-resolvers}) and a {@code config} section</li>
 *   <li>{@code config}: options shared by all output files ({@code package}, {@code className},
 *   {@code typesPrefix}, {@code listType}, {@code scalars} and {@code mappers})</li>
 * </ul>
 * An option in the {@code config} section of an output file replaces the shared option of the same
 * name; maps are not merged.
 */
record CodegenConfig(Path schema, List<OutputFile> outputFiles) {
  static CodegenConfig read(Path configFile) throws IOException {
    JsonNode root = new YAMLMapper().readTree(Files.readString(configFile));
    Path baseDir = configFile.toAbsolutePath().getParent();
    Path schema = baseDir.resolve(requiredText(root, "schema")).normalize();

    JsonNode sharedConfig = root.path("config");
    var outputs = new ArrayList<OutputFile>();
    for (var entry : root.path("generates").properties()) {
      outputs.add(OutputFile.of(entry.getKey(), entry.getValue(), sharedConfig));
    }
    if (outputs.isEmpty()) {
      throw new IllegalArgumentException("No output files ('generates') in " + configFile);
    }
    return new CodegenConfig(schema, List.copyOf(outputs));
  }

  private static String requiredText(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || !value.isString()) {
      throw new IllegalArgumentException("Missing required config value: " + field);
    }
    return value.asString();
  }

  /** The kind of Java source a {@link OutputFile} contains. */
  enum Generator {
    /** Input types, field argument types and enums. */
    TYPES("java", "Types"),
    /** A {@code DataFetcher} interface for each object type. */
    DATA_FETCHERS("java-resolvers", "Resolvers");

    private final String pluginName;
    private final String defaultClassName;

    Generator(String pluginName, String defaultClassName) {
      this.pluginName = pluginName;
      this.defaultClassName = defaultClassName;
    }
  }

  /**
   * @param fileName the output path relative to the package directory, used only to check that
   *                 it matches the generated class name
   */
  record OutputFile(
    String fileName,
    Generator generator,
    String packageName,
    String className,
    String typesPrefix,
    String listType,
    Map<String, String> scalars,
    Map<String, String> mappers
  ) {
    private static OutputFile of(String fileName, JsonNode node, JsonNode sharedConfig) {
      Generator generator = null;
      for (JsonNode plugin : node.path("plugins")) {
        generator = generatorFor(fileName, plugin.asString(), generator);
      }
      if (generator == null) {
        throw new IllegalArgumentException("No java or java-resolvers plugin for " + fileName);
      }

      JsonNode config = node.path("config");
      String packageName = text(config, sharedConfig, "package", null);
      if (packageName == null) {
        throw new IllegalArgumentException("Missing required config value: package");
      }
      return new OutputFile(
        fileName,
        generator,
        packageName,
        text(config, sharedConfig, "className", generator.defaultClassName),
        text(config, sharedConfig, "typesPrefix", ""),
        text(config, sharedConfig, "listType", "Iterable"),
        map(config, sharedConfig, "scalars"),
        map(config, sharedConfig, "mappers")
      );
    }

    private static Generator generatorFor(String fileName, String plugin, Generator current) {
      for (Generator candidate : Generator.values()) {
        if (candidate.pluginName.equals(plugin)) {
          if (current != null) {
            throw new IllegalArgumentException("More than one code plugin for " + fileName);
          }
          return candidate;
        }
      }
      throw new IllegalArgumentException("Unsupported plugin for " + fileName + ": " + plugin);
    }

    private static String text(JsonNode config, JsonNode shared, String field, String fallback) {
      JsonNode value = config.has(field) ? config.get(field) : shared.get(field);
      return value == null || value.isNull() ? fallback : value.asString();
    }

    private static Map<String, String> map(JsonNode config, JsonNode shared, String field) {
      JsonNode source = config.has(field) ? config.path(field) : shared.path(field);
      var result = new LinkedHashMap<String, String>();
      for (var entry : source.properties()) {
        result.put(entry.getKey(), entry.getValue().asString());
      }
      return result;
    }
  }
}
