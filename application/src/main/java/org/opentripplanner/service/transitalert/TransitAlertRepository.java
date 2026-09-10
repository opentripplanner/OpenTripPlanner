package org.opentripplanner.service.transitalert;

import java.util.Collection;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.updater.UpdateIncrementality;

/**
 * The mutable repository for the transit alerts. It is managed by the transaction framework: the
 * alert updaters obtain it through a
 * {@link org.opentripplanner.framework.transaction.api.WriteContext} on the single writer thread of
 * the alert write domain, and the repository lifecycle publishes a new immutable
 * {@link TransitAlertRepositorySnapshot} for the request threads at commit time.
 * <p>
 * The repository is copy-on-write: each transaction gets an instance initialized from the last
 * committed snapshot, so an incremental feed still builds on everything committed before, while the
 * writes of a task that fails are discarded (rolled back) instead of leaking into the next commit.
 * <p>
 * Alerts are partitioned by <em>feed</em>: an alert updater only sees and modifies the alerts of
 * the feed it is configured for. There is at most one alert updater per feed.
 * <p>
 * Some feeds are incremental (classic SIRI-SX with a requestor ref, SIRI-SX over Azure Service
 * Bus), while others deliver the complete set of alerts on every fetch (GTFS-RT alerts, SIRI-SX
 * Lite). The former use {@link #addOrUpdateAlerts} plus {@link #removeAlerts}, the latter use
 * {@link #replaceAlerts}, see {@link UpdateIncrementality}.
 */
public interface TransitAlertRepository {
  /**
   * Replace <em>all</em> alerts of the given feed with the given ones. Alerts of other feeds are
   * left untouched. This is the operation to use for {@link UpdateIncrementality#FULL_DATASET}
   * feeds: alerts that are no longer present in the feed disappear.
   */
  void replaceAlerts(String feedId, Collection<TransitAlert> alerts);

  /**
   * Add the given alerts to the given feed, replacing any existing alert with the same id. Alerts
   * of this feed that are not mentioned are kept. This is the operation to use for
   * {@link UpdateIncrementality#DIFFERENTIAL} feeds.
   */
  void addOrUpdateAlerts(String feedId, Collection<TransitAlert> alerts);

  /**
   * Remove the alerts with the given ids from the given feed. Ids that are not present are
   * ignored. This is how a {@link UpdateIncrementality#DIFFERENTIAL} feed expires an alert, e.g. a
   * SIRI-SX situation with progress {@code CLOSED}.
   */
  void removeAlerts(String feedId, Collection<FeedScopedId> ids);

  /**
   * Returns the number of alerts in the repository.
   */
  int getAlertCount(String feedId);
}
