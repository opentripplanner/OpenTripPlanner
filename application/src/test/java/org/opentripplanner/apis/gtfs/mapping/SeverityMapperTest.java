package org.opentripplanner.apis.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.apis.gtfs.mapping.SeverityMapper.getGraphQLSeverity;

import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertSeverityLevelType;
import org.opentripplanner.routing.alertpatch.AlertSeverity;

class SeverityMapperTest {

  @Test
  void mapNullToUnknown() {
    assertThat(getGraphQLSeverity(null)).isEqualTo(GraphQLAlertSeverityLevelType.UNKNOWN_SEVERITY);
  }

  @Test
  void mapSeverities() {
    assertThat(getGraphQLSeverity(AlertSeverity.UNKNOWN_SEVERITY)).isEqualTo(
      GraphQLAlertSeverityLevelType.UNKNOWN_SEVERITY
    );
    assertThat(getGraphQLSeverity(AlertSeverity.INFO)).isEqualTo(
      GraphQLAlertSeverityLevelType.INFO
    );
    assertThat(getGraphQLSeverity(AlertSeverity.VERY_SLIGHT)).isEqualTo(
      GraphQLAlertSeverityLevelType.WARNING
    );
    assertThat(getGraphQLSeverity(AlertSeverity.SLIGHT)).isEqualTo(
      GraphQLAlertSeverityLevelType.WARNING
    );
    assertThat(getGraphQLSeverity(AlertSeverity.WARNING)).isEqualTo(
      GraphQLAlertSeverityLevelType.WARNING
    );
    assertThat(getGraphQLSeverity(AlertSeverity.SEVERE)).isEqualTo(
      GraphQLAlertSeverityLevelType.SEVERE
    );
    assertThat(getGraphQLSeverity(AlertSeverity.VERY_SEVERE)).isEqualTo(
      GraphQLAlertSeverityLevelType.SEVERE
    );
  }
}
