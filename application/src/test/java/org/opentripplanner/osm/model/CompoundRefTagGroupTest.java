package org.opentripplanner.osm.model;

import static com.google.common.truth.Truth.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

public class CompoundRefTagGroupTest {

  @Test
  void compoundValueSingleTag() {
    var group = CompoundRefTagGroup.of("ref");
    var tags = Map.of("ref", "E1");

    assertThat(group.compoundValue(tags::get)).hasValue("E1");
  }

  @Test
  void compoundValueJoinsMultipleTagsWithColon() {
    var group = CompoundRefTagGroup.of("manufacturer", "ref");
    var tags = Map.of("manufacturer", "KONE", "ref", "12345");

    assertThat(group.compoundValue(tags::get)).hasValue("KONE:12345");
  }

  @Test
  void compoundValueEmptyWhenAnyTagMissing() {
    var group = CompoundRefTagGroup.of("manufacturer", "ref");
    var tags = Map.of("ref", "12345");

    assertThat(group.compoundValue(tags::get)).isEmpty();
  }

  @Test
  void compoundValueStripsWhitespaceFromResolvedValues() {
    var group = CompoundRefTagGroup.of("manufacturer", "ref");
    var tags = Map.of("manufacturer", " KONE ", "ref", " 12345 ");

    assertThat(group.compoundValue(tags::get)).hasValue("KONE:12345");
  }

  @Test
  void ofFiltersNullAndBlankTags() {
    var group = CompoundRefTagGroup.of("ref", null, "  ", "manufacturer");
    var tags = Map.of("ref", "12345", "manufacturer", "KONE");

    assertThat(group.compoundValue(tags::get)).hasValue("12345:KONE");
  }

  @Test
  void equalsAndHashCode() {
    assertThat(CompoundRefTagGroup.of("manufacturer", "ref")).isEqualTo(
      CompoundRefTagGroup.of("manufacturer", "ref")
    );
    assertThat(CompoundRefTagGroup.of("manufacturer", "ref").hashCode()).isEqualTo(
      CompoundRefTagGroup.of("manufacturer", "ref").hashCode()
    );
    assertThat(CompoundRefTagGroup.of("manufacturer", "ref")).isNotEqualTo(
      CompoundRefTagGroup.of("ref", "manufacturer")
    );
    assertThat(CompoundRefTagGroup.of("ref")).isNotEqualTo(CompoundRefTagGroup.of("manufacturer"));
  }
}
