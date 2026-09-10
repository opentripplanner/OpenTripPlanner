package org.opentripplanner.service.transitalert.internal;

import com.google.common.collect.ImmutableListMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.EntityKey;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.service.transitalert.TransitAlertRepository;
import org.opentripplanner.service.transitalert.TransitAlertRepositorySnapshot;

/**
 * Mutable repository for the transit alerts. A new instance is created for each transaction that
 * writes alerts - initialized from the last committed snapshot - and is only ever accessed from
 * the single writer thread. {@link #freeze()} publishes an immutable snapshot of its state at
 * commit time.
 * <p>
 * The state is copied lazily: the constructor copies only the outer map, while the (immutable) map
 * of a feed is copied the first time that feed is written. Alerts of feeds that a transaction does
 * not touch are therefore shared with the snapshot it was created from, and edits made to a
 * repository that is never frozen are simply discarded - this is what makes rollback work.
 * <p>
 * The alerts are stored per feed (there is at most one alert updater per feed) and only merged into
 * a single index when {@link #freeze()} publishes an immutable snapshot at commit time.
 */
public class DefaultTransitAlertRepository implements TransitAlertRepository {

  /**
   * Alerts by feed id, then by alert id. The inner maps are immutable and may be shared with the
   * snapshot this repository was created from, see {@link #mutableAlerts(String)}.
   */
  private final Map<String, Map<FeedScopedId, TransitAlert>> alertsByFeedId;

  /**
   * The feeds whose inner map has been copied and may be mutated by this instance.
   */
  private final Set<String> copiedFeeds = new HashSet<>();

  /** Create an empty repository. */
  public DefaultTransitAlertRepository() {
    this.alertsByFeedId = new HashMap<>();
  }

  /** Create a repository initialized with the state of the given snapshot. */
  DefaultTransitAlertRepository(Map<String, Map<FeedScopedId, TransitAlert>> alertsByFeedId) {
    this.alertsByFeedId = new HashMap<>(alertsByFeedId);
  }

  @Override
  public void replaceAlerts(String feedId, Collection<TransitAlert> alerts) {
    var newAlerts = new HashMap<FeedScopedId, TransitAlert>();
    for (TransitAlert alert : alerts) {
      newAlerts.put(alert.getId(), alert);
    }
    alertsByFeedId.put(feedId, newAlerts);
    copiedFeeds.add(feedId);
  }

  @Override
  public void addOrUpdateAlerts(String feedId, Collection<TransitAlert> alerts) {
    var existing = mutableAlerts(feedId);
    for (TransitAlert alert : alerts) {
      existing.put(alert.getId(), alert);
    }
  }

  @Override
  public void removeAlerts(String feedId, Collection<FeedScopedId> ids) {
    if (!alertsByFeedId.containsKey(feedId)) {
      return;
    }
    var existing = mutableAlerts(feedId);
    ids.forEach(existing::remove);
  }

  @Override
  public int getAlertCount(String feedId) {
    var alerts = alertsByFeedId.get(feedId);
    return alerts == null ? 0 : alerts.size();
  }

  /**
   * Produce an immutable copy of the state to be used in the repository life-cycle.
   * <p>
   * <b>THIS SHOULD ONLY BE USED BY {@link TransitAlertRepositoryLifecycle}. TEST USAGE
   * CAN BE REMOVED WHEN WE ARE ABLE TO USE DAGGER CONTEXT IN ALL TESTS AND THIS CAN THEN BE MADE
   * PACKAGE LOCAL.</b>
   */
  public TransitAlertRepositorySnapshot freeze() {
    var indexBuilder = ImmutableListMultimap.<EntityKey, TransitAlert>builder();
    var allAlerts = new ArrayList<TransitAlert>();
    var alertsById = new HashMap<FeedScopedId, TransitAlert>();
    var byFeedIdCopy = new HashMap<String, Map<FeedScopedId, TransitAlert>>();

    for (var entry : alertsByFeedId.entrySet()) {
      // Feeds this transaction did not touch are already immutable and can be shared as they are.
      byFeedIdCopy.put(
        entry.getKey(),
        copiedFeeds.contains(entry.getKey()) ? Map.copyOf(entry.getValue()) : entry.getValue()
      );
      for (TransitAlert alert : entry.getValue().values()) {
        allAlerts.add(alert);
        alertsById.put(alert.getId(), alert);
        for (EntitySelector entity : alert.entities()) {
          indexBuilder.put(entity.key(), alert);
        }
      }
    }
    return new DefaultTransitAlertRepositorySnapshot(
      indexBuilder.build(),
      List.copyOf(allAlerts),
      Map.copyOf(alertsById),
      Map.copyOf(byFeedIdCopy)
    );
  }

  /**
   * Return the alerts of the given feed, copying them first if they are still shared with the
   * snapshot this repository was created from.
   */
  private Map<FeedScopedId, TransitAlert> mutableAlerts(String feedId) {
    if (copiedFeeds.add(feedId)) {
      var existing = alertsByFeedId.get(feedId);
      alertsByFeedId.put(feedId, existing == null ? new HashMap<>() : new HashMap<>(existing));
    }
    return alertsByFeedId.get(feedId);
  }
}
