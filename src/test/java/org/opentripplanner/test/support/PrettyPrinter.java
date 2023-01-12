package org.opentripplanner.test.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

public class PrettyPrinter {

  private static final ObjectWriter PRETTY_PRINTER;
  private static final ObjectMapper mapper = new ObjectMapper();

  static {
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    var pp = new DefaultPrettyPrinter();
    pp.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
    PRETTY_PRINTER = mapper.writer(pp);
  }

  public static String json(JsonNode body) {
    try {
      return PRETTY_PRINTER.writeValueAsString(body);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
  public static String json(String input) {
    try {
      var json = mapper.readTree(input);
      return PRETTY_PRINTER.writeValueAsString(json);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
