package org.opentripplanner.modularization.harness;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonNormalizeTest {

  private static String normalize(String json, List<Glob> ignore, String sortBy, List<Glob> unordered)
    throws IOException {
    JsonNode in = Json.MAPPER.readTree(json);
    return Json.normalize(in, ignore, sortBy, unordered).toString();
  }

  @Test
  void objectKeysAreSorted() throws IOException {
    assertEquals("{\"a\":1,\"b\":{\"x\":1,\"y\":2}}", normalize("{\"b\":{\"y\":2,\"x\":1},\"a\":1}", List.of(), null, List.of()));
  }

  @Test
  void ignoredPathsAreRemoved() throws IOException {
    assertEquals(
      "{\"keep\":1}",
      normalize("{\"keep\":1,\"time\":5}", List.of(new Glob("time")), null, List.of())
    );
  }

  @Test
  void arraysKeepTheirOrderByDefault() throws IOException {
    assertEquals("[3,1,2]", normalize("[3,1,2]", List.of(), null, List.of()));
  }

  @Test
  void arraysOfNamedObjectsCanBeSortedByName() throws IOException {
    assertEquals(
      "[{\"name\":\"a\"},{\"name\":\"b\"}]",
      normalize("[{\"name\":\"b\"},{\"name\":\"a\"}]", List.of(), "name", List.of())
    );
  }

  @Test
  void unorderedArraysAreSortedByContent() throws IOException {
    assertEquals(
      "{\"l\":[{\"id\":1},{\"id\":2}]}",
      normalize("{\"l\":[{\"id\":2},{\"id\":1}]}", List.of(), null, List.of(new Glob("l")))
    );
  }

  @Test
  void ignoreAppliesInsideUnorderedArrays() throws IOException {
    assertEquals(
      "{\"l\":[{\"id\":1},{\"id\":2}]}",
      normalize(
        "{\"l\":[{\"id\":2,\"t\":9},{\"id\":1,\"t\":8}]}",
        List.of(new Glob("l[*].t")),
        null,
        List.of(new Glob("l"))
      )
    );
  }
}
