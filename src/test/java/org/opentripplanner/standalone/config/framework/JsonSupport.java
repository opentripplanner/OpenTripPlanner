package org.opentripplanner.standalone.config.framework;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class JsonSupport {

  public static final ObjectMapper LENIENT_MAPPER = new ObjectMapper();

  static {
    LENIENT_MAPPER.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    LENIENT_MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    LENIENT_MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
  }

  /**
   * Convert text to a JsonNode.
   * <p>
   * Comments and unquoted fields are allowed as well as using ' instead of ".
   */
  public static JsonNode jsonNodeForTest(String jsonText) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
      mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
      mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

      // Replace ' with "
      jsonText = jsonText.replace("'", "\"");

      return mapper.readTree(jsonText);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Load a JSON file from a resource file.
   *
   * @param path Use format like 'standalone/config/build-config.json'
   */
  public static JsonNode jsonNodeFromResource(String path) {
    try {
      @SuppressWarnings("ConstantConditions")
      URI uri = ClassLoader.getSystemClassLoader().getResource(path).toURI();

      return jsonNodeFromPath(Paths.get(uri));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Load a JSON file from a Path.
   */
  public static JsonNode jsonNodeFromPath(Path path) {
    try {
      var json = Files.readString(path);

      return LENIENT_MAPPER.readTree(json);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Load a JSON file from a Path.
   */
  public static JsonNode jsonNodeFromString(String input) {
    try {
      return LENIENT_MAPPER.readTree(input);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public static NodeAdapter newNodeAdapterForTest(String configText) {
    JsonNode config = jsonNodeForTest(configText);
    return new NodeAdapter(config, "Test");
  }
}
