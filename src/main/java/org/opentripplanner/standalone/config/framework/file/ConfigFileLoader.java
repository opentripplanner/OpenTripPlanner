package org.opentripplanner.standalone.config.framework.file;

import static org.opentripplanner.standalone.config.framework.file.IncludeFileDirective.includeFileDirective;
import static org.opentripplanner.standalone.config.framework.project.EnvironmentVariableReplacer.insertEnvironmentVariables;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.opentripplanner.framework.application.OtpAppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic config file loader. This is used to load all configuration files.
 * <p>
 * This class is also provide logging when a config file is loaded. We load and parse config files
 * early to reveal syntax errors without waiting for graph build.
 */
public class ConfigFileLoader {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigFileLoader.class);

  /** When echoing config files to logs, values for these keys will be hidden. */
  private static final Set<String> REDACT_KEYS = Set.of("secretKey", "accessKey", "gsCredentials");

  private final ObjectMapper mapper = new ObjectMapper();

  @Nullable
  private File configDir = null;

  @Nullable
  private String jsonFallback = null;

  private ConfigFileLoader() {
    // Configure mapper
    mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
  }

  public static ConfigFileLoader of() {
    return new ConfigFileLoader();
  }

  public ConfigFileLoader withConfigDir(File configDir) {
    this.configDir = assertConfigDirExist(configDir);
    return this;
  }

  public ConfigFileLoader withJsonFallback(String jsonFallback) {
    this.jsonFallback = jsonFallback;
    return this;
  }

  /**
   * Generic method to parse the given json and return a JsonNode tree. The {@code source} is used
   * to generate a proper error message in case the string is not a proper JSON document.
   */
  public static JsonNode nodeFromString(String json, String source) {
    return of().stringToJsonNode(json, source);
  }

  /**
   * Load the configuration file as a JsonNode tree. An empty node is returned if the given
   * {@code configDir}  is {@code null} or config file is NOT found.
   * <p>
   * This is public to allow loading configuration files from tests like the SpeedTest.
   *
   * @see #loadJsonFile for more details.
   */
  public JsonNode loadFromFile(String filename) {
    // Use default parameters if no configDir is available.
    if (configDir == null) {
      if (jsonFallback != null) {
        return stringToJsonNode(jsonFallback, filename);
      }
      LOG.warn("Config '{}' not loaded, using defaults. Config directory not set.", filename);
      return MissingNode.getInstance();
    }
    return loadJsonFile(new File(configDir, filename));
  }

  /**
   * Convert the JsonNode to a pretty-printed string with secrets hidden, operating on a protective
   * copy of the node to avoid losing information.
   */
  private static String toRedactedString(JsonNode node) {
    JsonNode redactedNode = node.deepCopy();
    redactSecretsRecursive(redactedNode);
    return redactedNode.toPrettyString();
  }

  /** Note that this method destructively modifies the node and its children in place. */
  private static void redactSecretsRecursive(JsonNode node) {
    if (node.isObject()) {
      node
        .fields()
        .forEachRemaining(entry -> {
          if (entry.getValue().isObject()) {
            redactSecretsRecursive(entry.getValue());
          } else if (REDACT_KEYS.contains(entry.getKey())) {
            entry.setValue(new TextNode("********"));
          }
        });
    }
  }

  /**
   * Open and parse the JSON file at the given path into a Jackson JSON tree. Comments and unquoted
   * keys are allowed. Returns an empty node if the file does not exist. Throws an exception if the
   * file contains syntax errors or cannot be parsed for some other reason.
   * <p>
   * We do not require any JSON config files to be present because that would get in the way of the
   * simplest rapid deployment workflow. Therefore we return an empty JSON node when the file is
   * missing, causing us to fall back on all the default values as if there was a JSON file present
   * with no fields defined.
   */
  private JsonNode loadJsonFile(File file) {
    try {
      String configString = IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);
      JsonNode node = stringToJsonNode(configString, file.toString());
      LOG.info("Load JSON configuration file '{}'", file.getPath());
      LOG.info("Summarizing '{}': {}", file.getPath(), toRedactedString(node));
      return node;
    } catch (FileNotFoundException ex) {
      LOG.info("File '{}' is not present. Using default configuration.", file);
      return MissingNode.getInstance();
    } catch (IOException e) {
      LOG.error("Error while parsing JSON config file '{}': {}", file, e.getMessage());
      throw new RuntimeException(e.getLocalizedMessage(), e);
    }
  }

  /**
   * Convert a String into JsonNode. Comments and unquoted fields are allowed in the given {@code
   * jsonAsString} input.
   *
   * @param source is used only to generate a human friendly error message in case of an error
   *               parsing the JSON or inserting environment variables.
   */
  private JsonNode stringToJsonNode(String jsonAsString, String source) {
    try {
      if (jsonAsString == null || jsonAsString.isBlank()) {
        return MissingNode.getInstance();
      }
      jsonAsString = includeFileDirective(configDir, jsonAsString, source);
      jsonAsString = insertEnvironmentVariables(jsonAsString, source);

      return mapper.readTree(jsonAsString);
    } catch (IOException ie) {
      LOG.error("Error while parsing config '{}'.", source, ie);
      throw new OtpAppException("Failed to load config: " + source);
    }
  }

  public static File assertConfigDirExist(File configDir) {
    // Config dir not set, using defaults
    if (configDir == null) {
      return null;
    }
    if (!configDir.isDirectory()) {
      throw new IllegalArgumentException(configDir + " is not a readable configuration directory.");
    }
    return configDir;
  }
}
