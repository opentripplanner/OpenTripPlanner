package org.opentripplanner.modularization.harness;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class ConfigDocsTest {

  @Test
  void splitsByHeadingPath() {
    var node = ConfigDocs.toSections(
      "intro\n# A\ntext a\n## B\ntext b\n## B\nsecond\n# C\n```\n# not a heading\n```\n"
    );
    var keys = new ArrayList<String>();
    node.fieldNames().forEachRemaining(keys::add);
    assertEquals(java.util.List.of("<top>", "A", "A > B", "A > B #2", "C"), keys);
    assertEquals("text b", node.get("A > B").asText());
    assertEquals("```\n# not a heading\n```", node.get("C").asText());
  }

  @Test
  void ignoresEmptyTop() {
    var node = ConfigDocs.toSections("# A\nx\n");
    var keys = new ArrayList<String>();
    node.fieldNames().forEachRemaining(keys::add);
    assertEquals(java.util.List.of("A"), keys);
  }
}
