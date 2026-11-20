package org.opentripplanner.transit.model.basic;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class NarrowedTransitModeTest {

  private static final SubMode REGIONAL_RAIL = SubMode.getOrBuildAndCacheForever("regionalRail");

  @Test
  void defaults() {
    var subject = NarrowedTransitMode.of(TransitMode.RAIL).build();

    assertThat(subject.getMode()).isEqualTo(TransitMode.RAIL);
    assertThat(subject.getSubMode()).isNull();
    assertThat(subject.getReplacement()).isEqualTo(ReplacementRequirement.IGNORED);
    assertThat(subject.getDistanceCategories()).isNull();
    assertThat(subject.isMainModeOnly()).isTrue();
    assertThat(subject.isConvertibleToMainAndSubMode()).isTrue();
  }

  @Test
  void ofMainAndSubMode() {
    var subject = NarrowedTransitMode.ofMainAndSubMode(
      new MainAndSubMode(TransitMode.RAIL, REGIONAL_RAIL)
    );

    assertThat(subject).isEqualTo(
      NarrowedTransitMode.of(TransitMode.RAIL).withSubMode(REGIONAL_RAIL).build()
    );
    assertThat(subject.isMainModeOnly()).isFalse();
    assertThat(subject.toMainAndSubMode()).isEqualTo(
      new MainAndSubMode(TransitMode.RAIL, REGIONAL_RAIL)
    );
  }

  @Test
  void distanceCategoryIsNotConvertibleToMainAndSubMode() {
    var subject = NarrowedTransitMode.of(TransitMode.RAIL)
      .withDistanceCategories(List.of(DistanceCategory.REGIONAL))
      .build();

    assertThat(subject.isMainModeOnly()).isFalse();
    assertThat(subject.isConvertibleToMainAndSubMode()).isFalse();
    assertThrows(IllegalArgumentException.class, subject::toMainAndSubMode);
    assertThat(subject.toString()).isEqualTo("RAIL::::IGNORED::[REGIONAL]");
  }

  @Test
  void equalsAndHashCode() {
    var regional = NarrowedTransitMode.of(TransitMode.RAIL)
      .withDistanceCategories(List.of(DistanceCategory.REGIONAL))
      .build();
    var sameRegional = NarrowedTransitMode.of(TransitMode.RAIL)
      .withDistanceCategories(List.of(DistanceCategory.REGIONAL))
      .build();

    assertThat(regional).isEqualTo(sameRegional);
    assertThat(regional.hashCode()).isEqualTo(sameRegional.hashCode());
    assertThat(regional).isNotEqualTo(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withDistanceCategories(List.of(DistanceCategory.LOCAL))
        .build()
    );
    assertThat(regional).isNotEqualTo(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withReplacement(ReplacementRequirement.FORBIDDEN)
        .withDistanceCategories(List.of(DistanceCategory.REGIONAL))
        .build()
    );
    assertThat(regional).isNotEqualTo(NarrowedTransitMode.of(TransitMode.RAIL).build());
    // The order of the categories does not matter
    assertThat(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withDistanceCategories(List.of(DistanceCategory.REGIONAL, DistanceCategory.LOCAL))
        .build()
    ).isEqualTo(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withDistanceCategories(List.of(DistanceCategory.LOCAL, DistanceCategory.REGIONAL))
        .build()
    );
  }

  @Test
  void emptyDistanceCategoriesAreRejected() {
    var builder = NarrowedTransitMode.of(TransitMode.RAIL);
    assertThrows(IllegalArgumentException.class, () -> builder.withDistanceCategories(List.of()));
  }
}
