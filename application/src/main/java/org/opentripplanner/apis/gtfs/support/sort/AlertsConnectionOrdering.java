package org.opentripplanner.apis.gtfs.support.sort;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.TransitAlert;

/**
 * Defines the ordering of the alerts returned by the GraphQL {@code alertsConnection} field.
 * <b>Note, this ordering is not set in stone and can be changed/removed if need be.</b>
 * <p>
 * The order is fixed, deterministic and not configurable through the API:
 * <ol>
 *   <li>{@link AlertSeverity} descending, using {@link AlertSeverity#sortingIndex()}, so the most
 *   severe alerts come first. An alert without a severity is treated as
 *   {@link AlertSeverity#UNKNOWN_SEVERITY}.</li>
 *   <li>Within the same severity, the effective start of the alert's validity
 *   ({@link TransitAlert#getEffectiveStartDate()}) ascending. Alerts with an open start (or which
 *   are never active) have no effective start and come first.</li>
 *   <li>The alert id ascending. This is a pure tiebreaker which makes the order stable, so that
 *   paging through the connection never skips or duplicates an alert.</li>
 * </ol>
 * The connection uses index based cursors, so the ordering only needs to be applied to the
 * complete list before the connection is built.
 */
public class AlertsConnectionOrdering {

  private static final Comparator<Instant> BY_EFFECTIVE_START = Comparator.nullsFirst(
    Comparator.naturalOrder()
  );

  private static final Comparator<TransitAlert> COMPARATOR = Comparator.comparingInt(
    (TransitAlert alert) -> severity(alert).sortingIndex()
  )
    .reversed()
    .thenComparing(TransitAlert::getEffectiveStartDate, BY_EFFECTIVE_START)
    .thenComparing(TransitAlert::getId);

  /**
   * Returns a new list containing the given alerts in the order described in
   * {@link AlertsConnectionOrdering}.
   */
  public static List<TransitAlert> sort(Collection<TransitAlert> alerts) {
    return alerts.stream().sorted(COMPARATOR).toList();
  }

  private static AlertSeverity severity(TransitAlert alert) {
    var severity = alert.severity();
    return severity == null ? AlertSeverity.UNKNOWN_SEVERITY : severity;
  }
}
