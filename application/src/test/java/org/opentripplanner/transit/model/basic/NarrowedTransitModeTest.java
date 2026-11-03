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
    assertThat(subject.getGeographicalScope()).isNull();
    assertThat(subject.getExcludedGeographicalScopes()).isEmpty();
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
  void geographicalScopeIsNotConvertibleToMainAndSubMode() {
    var subject = NarrowedTransitMode.of(TransitMode.RAIL)
      .withGeographicalScope(GeographicalScope.REGIONAL)
      .build();

    assertThat(subject.isMainModeOnly()).isFalse();
    assertThat(subject.isConvertibleToMainAndSubMode()).isFalse();
    assertThrows(IllegalArgumentException.class, subject::toMainAndSubMode);
    assertThat(subject.toString()).isEqualTo("RAIL::::IGNORED::REGIONAL");
  }

  @Test
  void equalsAndHashCode() {
    var regional = NarrowedTransitMode.of(TransitMode.RAIL)
      .withGeographicalScope(GeographicalScope.REGIONAL)
      .build();
    var sameRegional = NarrowedTransitMode.of(TransitMode.RAIL)
      .withGeographicalScope(GeographicalScope.REGIONAL)
      .build();

    assertThat(regional).isEqualTo(sameRegional);
    assertThat(regional.hashCode()).isEqualTo(sameRegional.hashCode());
    assertThat(regional).isNotEqualTo(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withGeographicalScope(GeographicalScope.SUBURBAN)
        .build()
    );
    assertThat(regional).isNotEqualTo(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withReplacement(ReplacementRequirement.FORBIDDEN)
        .withGeographicalScope(GeographicalScope.REGIONAL)
        .build()
    );
    assertThat(regional).isNotEqualTo(NarrowedTransitMode.of(TransitMode.RAIL).build());
  }

  @Test
  void excludedGeographicalScopes() {
    var subject = NarrowedTransitMode.of(TransitMode.RAIL)
      .withExcludedGeographicalScopes(
        List.of(GeographicalScope.LONG_DISTANCE, GeographicalScope.REGIONAL)
      )
      .build();

    assertThat(subject.getExcludedGeographicalScopes()).containsExactly(
      GeographicalScope.REGIONAL,
      GeographicalScope.LONG_DISTANCE
    );
    assertThat(subject.isMainModeOnly()).isFalse();
    assertThat(subject.isConvertibleToMainAndSubMode()).isFalse();
    assertThrows(IllegalArgumentException.class, subject::toMainAndSubMode);
    assertThat(subject.toString()).isEqualTo("RAIL::::IGNORED::NOT[REGIONAL, LONG_DISTANCE]");
  }

  @Test
  void excludedGeographicalScopesEquality() {
    var subject = NarrowedTransitMode.of(TransitMode.RAIL)
      .withExcludedGeographicalScopes(
        List.of(GeographicalScope.LONG_DISTANCE, GeographicalScope.REGIONAL)
      )
      .build();
    var sameInOtherOrder = NarrowedTransitMode.of(TransitMode.RAIL)
      .withExcludedGeographicalScopes(
        List.of(GeographicalScope.REGIONAL, GeographicalScope.LONG_DISTANCE)
      )
      .build();

    assertThat(subject).isEqualTo(sameInOtherOrder);
    assertThat(subject.hashCode()).isEqualTo(sameInOtherOrder.hashCode());
    assertThat(subject).isNotEqualTo(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withExcludedGeographicalScopes(List.of(GeographicalScope.LONG_DISTANCE))
        .build()
    );
    assertThat(
      NarrowedTransitMode.of(TransitMode.RAIL).withExcludedGeographicalScopes(List.of()).build()
    ).isEqualTo(NarrowedTransitMode.of(TransitMode.RAIL).build());
  }

  @Test
  void geographicalScopeAndExcludedGeographicalScopesCantBeCombined() {
    var builder = NarrowedTransitMode.of(TransitMode.RAIL)
      .withGeographicalScope(GeographicalScope.REGIONAL)
      .withExcludedGeographicalScopes(List.of(GeographicalScope.SUBURBAN));

    assertThrows(IllegalArgumentException.class, builder::build);
  }
}
