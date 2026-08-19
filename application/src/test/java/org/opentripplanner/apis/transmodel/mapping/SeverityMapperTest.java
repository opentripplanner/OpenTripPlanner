package org.opentripplanner.apis.transmodel.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.apis.transmodel.mapping.SeverityMapper.getTransmodelSeverity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentripplanner.routing.alertpatch.AlertSeverity;

class SeverityMapperTest {

  @Test
  void mapNullToNormal() {
    assertThat(getTransmodelSeverity(null)).isEqualTo("normal");
  }

  @Test
  void mapSeverities() {
    assertThat(getTransmodelSeverity(AlertSeverity.UNKNOWN_SEVERITY)).isEqualTo("unknown");
    assertThat(getTransmodelSeverity(AlertSeverity.INFO)).isEqualTo("noImpact");
    assertThat(getTransmodelSeverity(AlertSeverity.VERY_SLIGHT)).isEqualTo("verySlight");
    assertThat(getTransmodelSeverity(AlertSeverity.SLIGHT)).isEqualTo("slight");
    assertThat(getTransmodelSeverity(AlertSeverity.WARNING)).isEqualTo("normal");
    assertThat(getTransmodelSeverity(AlertSeverity.SEVERE)).isEqualTo("severe");
    assertThat(getTransmodelSeverity(AlertSeverity.VERY_SEVERE)).isEqualTo("verySevere");
  }

  /**
   * The deprecated 'undefined' value of the API enum is never returned by OTP.
   */
  @ParameterizedTest
  @EnumSource(AlertSeverity.class)
  void undefinedIsNeverReturned(AlertSeverity severity) {
    assertThat(getTransmodelSeverity(severity)).isNotEqualTo("undefined");
  }
}
