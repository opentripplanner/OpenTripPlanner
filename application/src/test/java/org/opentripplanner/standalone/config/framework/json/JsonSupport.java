package org.opentripplanner.standalone.config.framework.json;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

public class JsonSupport {

  private static final ObjectMapper LENIENT_MAPPER = JsonMapper.builder()
    .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
    .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
    .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
    .build();
  private static final ObjectWriter PRETTY_PRINTER;

  static {
    // -Dline.seperator cannot be reliably overridden
    // 2 spaces + LF
    var lfOnlyIndenter = new DefaultIndenter("  ", "\n");
    var pp = new DefaultPrettyPrinter()
      .withObjectIndenter(lfOnlyIndenter)
      .withArrayIndenter(lfOnlyIndenter);
    PRETTY_PRINTER = LENIENT_MAPPER.writer(pp);
  }

  /**
   * Convert text to a JsonNode.
   * <p>
   * Comments and unquoted fields are allowed as well as using ' instead of ".
   */
  public static JsonNode jsonNodeForTest(String jsonText) {
    try {
      ObjectMapper mapper = JsonMapper.builder()
        .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
        .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        .build();

      // Replace ' with "
      jsonText = jsonText.replace("'", "\"");

      return mapper.readTree(jsonText);
    } catch (JacksonException e) {
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
   * Returns a pretty-printed version of the json node. In particular, array elements
   * are put on a new line.
   */
  public static String prettyPrint(JsonNode body) {
    try {
      return PRETTY_PRINTER.writeValueAsString(body);
    } catch (JacksonException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a pretty-printed version of the input string, which must be valid JSON.
   * In particular, array elements are put on a new line.
   */
  public static String prettyPrint(String input) {
    try {
      var json = LENIENT_MAPPER.readTree(input);
      return PRETTY_PRINTER.writeValueAsString(json);
    } catch (JacksonException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Load a JSON file from a Path.
   */
  public static JsonNode jsonNodeFromPath(Path path) {
    try {
      var json = Files.readString(path);

      return LENIENT_MAPPER.readTree(json);
    } catch (JacksonException | IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Load a JSON file from a Path.
   */
  public static JsonNode jsonNodeFromString(String input) {
    try {
      return LENIENT_MAPPER.readTree(input);
    } catch (JacksonException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public static NodeAdapter newNodeAdapterForTest(String configText) {
    JsonNode config = jsonNodeForTest(configText);
    return new NodeAdapter(config, "Test");
  }
}
