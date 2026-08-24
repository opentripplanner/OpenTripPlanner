package org.opentripplanner.updater.alert.siri.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.updater.alert.siri.mapping.SiriSeverityMapper.getAlertSeverityForSiriSeverity;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import uk.org.siri.siri21.SeverityEnumeration;

class SiriSeverityMapperTest {

  @Test
  void mapNullToWarning() {
    assertThat(getAlertSeverityForSiriSeverity(null)).isEqualTo(AlertSeverity.WARNING);
  }

  /**
   * SIRI does not distinguish between 'no severity given' and 'undefined severity', so both
   * 'undefined' and 'normal' are mapped to {@link AlertSeverity#WARNING}.
   */
  @Test
  void mapUndefinedToWarning() {
    assertThat(getAlertSeverityForSiriSeverity(SeverityEnumeration.UNDEFINED)).isEqualTo(
      AlertSeverity.WARNING
    );
    assertThat(getAlertSeverityForSiriSeverity(SeverityEnumeration.NORMAL)).isEqualTo(
      AlertSeverity.WARNING
    );
  }

  @Test
  void mapSeverities() {
    assertThat(getAlertSeverityForSiriSeverity(SeverityEnumeration.UNKNOWN)).isEqualTo(
      AlertSeverity.UNKNOWN_SEVERITY
    );
    assertThat(getAlertSeverityForSiriSeverity(SeverityEnumeration.NO_IMPACT)).isEqualTo(
      AlertSeverity.INFO
    );
    assertThat(getAlertSeverityForSiriSeverity(SeverityEnumeration.VERY_SLIGHT)).isEqualTo(
      AlertSeverity.VERY_SLIGHT
    );
    assertThat(getAlertSeverityForSiriSeverity(SeverityEnumeration.SLIGHT)).isEqualTo(
      AlertSeverity.SLIGHT
    );
    assertThat(getAlertSeverityForSiriSeverity(SeverityEnumeration.SEVERE)).isEqualTo(
      AlertSeverity.SEVERE
    );
    assertThat(getAlertSeverityForSiriSeverity(SeverityEnumeration.VERY_SEVERE)).isEqualTo(
      AlertSeverity.VERY_SEVERE
    );
  }
}
