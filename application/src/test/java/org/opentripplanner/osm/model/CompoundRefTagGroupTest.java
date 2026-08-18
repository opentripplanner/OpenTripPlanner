package org.opentripplanner.osm.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class CompoundRefTagGroupTest {

  @Test
  void compoundValueSingleTag() {
    var group = CompoundRefTagGroup.of("ref");
    var tags = Map.of("ref", "E1");

    assertEquals(Optional.of("E1"), group.compoundValue(tags::get));
  }

  @Test
  void compoundValueJoinsMultipleTagsWithColon() {
    var group = CompoundRefTagGroup.of("manufacturer", "ref");
    var tags = Map.of("manufacturer", "KONE", "ref", "12345");

    assertEquals(Optional.of("KONE:12345"), group.compoundValue(tags::get));
  }

  @Test
  void compoundValueEmptyWhenAnyTagMissing() {
    var group = CompoundRefTagGroup.of("manufacturer", "ref");
    var tags = Map.of("ref", "12345");

    assertEquals(Optional.empty(), group.compoundValue(tags::get));
  }

  @Test
  void compoundValueStripsWhitespaceFromResolvedValues() {
    var group = CompoundRefTagGroup.of("manufacturer", "ref");
    var tags = Map.of("manufacturer", " KONE ", "ref", " 12345 ");

    assertEquals(Optional.of("KONE:12345"), group.compoundValue(tags::get));
  }

  @Test
  void ofFiltersNullAndBlankTags() {
    var group = CompoundRefTagGroup.of("ref", null, "  ", "manufacturer");
    var tags = Map.of("ref", "12345", "manufacturer", "KONE");

    assertEquals(Optional.of("12345:KONE"), group.compoundValue(tags::get));
  }

  @Test
  void equalsAndHashCode() {
    assertEquals(
      CompoundRefTagGroup.of("manufacturer", "ref"),
      CompoundRefTagGroup.of("manufacturer", "ref")
    );
    assertEquals(
      CompoundRefTagGroup.of("manufacturer", "ref").hashCode(),
      CompoundRefTagGroup.of("manufacturer", "ref").hashCode()
    );
    assertNotEquals(
      CompoundRefTagGroup.of("manufacturer", "ref"),
      CompoundRefTagGroup.of("ref", "manufacturer")
    );
    assertNotEquals(CompoundRefTagGroup.of("ref"), CompoundRefTagGroup.of("manufacturer"));
  }
}
