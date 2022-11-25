package org.opentripplanner.standalone.config.framework.file;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.lang.StringUtils;

public class IncludeFileDirectiveTest {

  private static final String FILE_PREFIX = "o_o_standalone_config_IncludeFileDirectiveTest_";
  private static final String PART_FILE_NAME = FILE_PREFIX + "part.json";
  private static final File CONFIG_DIR = new File(".");
  private static final File PART_FILE = new File(CONFIG_DIR, PART_FILE_NAME);

  @AfterAll
  static void teardown() {
    //noinspection ResultOfMethodCallIgnored
    PART_FILE.delete();
  }

  @Test
  void includeFileWithoutQuotes() throws IOException {
    savePartialFile(json("value"));
    String result = IncludeFileDirective.includeFileDirective(
      CONFIG_DIR,
      json("{${includeFile:" + PART_FILE_NAME + "}}"),
      PART_FILE_NAME
    );
    assertEquals(json("{value}"), result);
  }

  @Test
  void includeFileWithQuotesAndProperJsonInput() throws IOException {
    savePartialFile(json("\t {\n  'foo' : 'bar' \n  }\n"));
    String result = IncludeFileDirective.includeFileDirective(
      CONFIG_DIR,
      json("{ 'key' : '${includeFile:" + PART_FILE_NAME + "}'}"),
      PART_FILE_NAME
    );
    assertEquals(json("{ 'key' : \t {\n  'foo' : 'bar' \n  }\n}"), result);
  }

  @Test
  void includeFileWithQuotesWithNoJsonInput() throws IOException {
    savePartialFile("value");
    String result = IncludeFileDirective.includeFileDirective(
      CONFIG_DIR,
      json("{ 'key' : '${includeFile:" + PART_FILE_NAME + "}' }"),
      PART_FILE_NAME
    );
    assertEquals(json("{ 'key' : 'value' }"), result);
  }

  private static String json(String text) {
    return StringUtils.quoteReplace(text);
  }

  private static void savePartialFile(String text) throws IOException {
    FileUtils.write(PART_FILE, text, UTF_8);
  }
}
