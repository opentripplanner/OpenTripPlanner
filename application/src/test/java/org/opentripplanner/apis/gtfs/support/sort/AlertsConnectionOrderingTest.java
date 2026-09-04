package org.opentripplanner.apis.gtfs.support.sort;

import static com.google.common.truth.Truth.assertThat;

import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.routing.alertpatch.AlertCalendar;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.TransitAlert;

class AlertsConnectionOrderingTest {

  private static final Instant EARLY = Instant.parse("2025-01-01T00:00:00Z");
  private static final Instant LATE = Instant.parse("2025-06-01T00:00:00Z");

  @Test
  void sortsBySeverityDescending() {
    var info = alert("info", AlertSeverity.INFO, EARLY);
    var severe = alert("severe", AlertSeverity.SEVERE, EARLY);
    var verySevere = alert("very-severe", AlertSeverity.VERY_SEVERE, EARLY);

    assertThat(AlertsConnectionOrdering.sort(List.of(info, verySevere, severe)))
      .containsExactly(verySevere, severe, info)
      .inOrder();
  }

  @Test
  void sortsByEffectiveStartAscendingWithinSeverity() {
    var late = alert("late", AlertSeverity.WARNING, LATE);
    var early = alert("early", AlertSeverity.WARNING, EARLY);

    assertThat(AlertsConnectionOrdering.sort(List.of(late, early)))
      .containsExactly(early, late)
      .inOrder();
  }

  @Test
  void alertWithoutSeverityIsTreatedAsUnknown() {
    var noSeverity = alert("no-severity", null, EARLY);
    var unknown = alert("unknown", AlertSeverity.UNKNOWN_SEVERITY, LATE);
    var info = alert("info", AlertSeverity.INFO, EARLY);
    var slight = alert("slight", AlertSeverity.SLIGHT, EARLY);

    // unknown severity sorts between SLIGHT and INFO, according to the enum declaration order
    assertThat(AlertsConnectionOrdering.sort(List.of(info, unknown, slight, noSeverity)))
      .containsExactly(slight, noSeverity, unknown, info)
      .inOrder();
  }

  @Test
  void sortsByIdAsTiebreaker() {
    var b = alert("b", AlertSeverity.WARNING, EARLY);
    var a = alert("a", AlertSeverity.WARNING, EARLY);
    var c = alert("c", AlertSeverity.WARNING, EARLY);

    assertThat(AlertsConnectionOrdering.sort(List.of(c, b, a)))
      .containsExactly(a, b, c)
      .inOrder();
  }

  @Test
  void orderIsStableAcrossCalls() {
    var alerts = List.of(
      alert("b", AlertSeverity.WARNING, EARLY),
      alert("a", AlertSeverity.WARNING, EARLY),
      alert("c", AlertSeverity.SEVERE, LATE)
    );

    var first = AlertsConnectionOrdering.sort(alerts);
    var second = AlertsConnectionOrdering.sort(
      List.of(alerts.get(2), alerts.get(0), alerts.get(1))
    );

    assertThat(second).containsExactlyElementsIn(first).inOrder();
  }

  private static TransitAlert alert(
    String id,
    @Nullable AlertSeverity severity,
    Instant effectiveStart
  ) {
    return TransitAlert.of(id(id))
      .withSeverity(severity)
      .withCalendar(AlertCalendar.of(TimePeriod.of(effectiveStart, null)))
      .build();
  }

  private static FeedScopedId id(String id) {
    return new FeedScopedId("F", id);
  }
}
