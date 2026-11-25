package org.opentripplanner.modularization.harness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DifferTest {

  private static JsonNode json(String text) throws IOException {
    return Json.MAPPER.readTree(text);
  }

  @Test
  void identicalDocumentsHaveNoDifferences() throws IOException {
    var doc = json("{\"a\":[1,{\"b\":\"x\"}],\"c\":null,\"d\":{}}");
    assertTrue(Differ.compare("s", doc, doc).isEmpty());
  }

  @Test
  void changedValueIsReportedWithItsPath() throws IOException {
    var diffs = Differ.compare("s", json("{\"a\":[1,{\"b\":\"x\"}]}"), json("{\"a\":[1,{\"b\":\"y\"}]}"));
    assertEquals(List.of(new Difference("s", "a[1].b", "\"x\"", "\"y\"")), diffs);
  }

  @Test
  void addedAndRemovedValuesAreReportedAsAbsent() throws IOException {
    var diffs = Differ.compare("s", json("{\"a\":1,\"b\":2}"), json("{\"b\":2,\"c\":3}"));
    assertEquals(
      List.of(
        new Difference("s", "a", "1", Differ.ABSENT),
        new Difference("s", "c", Differ.ABSENT, "3")
      ),
      diffs
    );
  }

  @Test
  void shorterArrayIsReportedAtTheMissingIndex() throws IOException {
    var diffs = Differ.compare("s", json("{\"a\":[1,2]}"), json("{\"a\":[1]}"));
    assertEquals(List.of(new Difference("s", "a[1]", "2", Differ.ABSENT)), diffs);
  }

  @Test
  void emptyContainerDiffersFromMissingValue() throws IOException {
    var diffs = Differ.compare("s", json("{\"x\":1,\"a\":[]}"), json("{\"x\":1}"));
    assertEquals(List.of(new Difference("s", "a", "[]", Differ.ABSENT)), diffs);
  }

  @Test
  void directoriesAreComparedByScenarioAndUnderscoreFilesAreSkipped(@TempDir Path tmp)
    throws IOException {
    Path b = Files.createDirectory(tmp.resolve("b"));
    Path c = Files.createDirectory(tmp.resolve("c"));
    Files.writeString(b.resolve("one.json"), "{\"v\":1}");
    Files.writeString(c.resolve("one.json"), "{\"v\":2}");
    Files.writeString(b.resolve("only-baseline.json"), "{}");
    Files.writeString(b.resolve("_log.json"), "{\"ignored\":1}");

    var diffs = Differ.compareDirectories(b, c);

    assertEquals(
      List.of(
        new Difference("one", "v", "1", "2"),
        new Difference("only-baseline", "<file>", "present", Differ.ABSENT)
      ),
      diffs
    );
  }
}
