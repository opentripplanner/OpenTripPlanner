package org.opentripplanner.service.transitalert.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;

class DefaultTransitAlertRepositoryTest {

  private static final String FEED_A = "A";
  private static final String FEED_B = "B";

  private static final TransitAlert ALERT_1 = alert(FEED_A, "1");
  private static final TransitAlert ALERT_2 = alert(FEED_A, "2");
  private static final TransitAlert OTHER_FEED_ALERT = alert(FEED_B, "1");

  private final DefaultTransitAlertRepository repository = new DefaultTransitAlertRepository();

  @Test
  void replaceAlertsDropsAlertsMissingFromTheNewSet() {
    repository.replaceAlerts(FEED_A, List.of(ALERT_1, ALERT_2));
    assertEquals(2, repository.getAlertCount(FEED_A));
    assertThat(snapshot().getAllAlerts()).containsExactly(ALERT_1, ALERT_2);

    // A full data set no longer containing ALERT_2 must expire it.
    repository.replaceAlerts(FEED_A, List.of(ALERT_1));
    assertEquals(1, repository.getAlertCount(FEED_A));
    assertThat(snapshot().getAllAlerts()).containsExactly(ALERT_1);
  }

  @Test
  void addOrUpdateAlertsKeepsPreviouslyStoredAlerts() {
    repository.addOrUpdateAlerts(FEED_A, List.of(ALERT_1));
    repository.addOrUpdateAlerts(FEED_A, List.of(ALERT_2));

    assertThat(snapshot().getAllAlerts()).containsExactly(ALERT_1, ALERT_2);
  }

  @Test
  void addOrUpdateAlertsReplacesAlertWithTheSameId() {
    var original = alert(FEED_A, "1");
    var updated = TransitAlert.of(id(FEED_A, "1"))
      .addEntity(new EntitySelector.Stop(id(FEED_A, "other-stop")))
      .build();

    repository.addOrUpdateAlerts(FEED_A, List.of(original));
    repository.addOrUpdateAlerts(FEED_A, List.of(updated));

    assertThat(snapshot().getAllAlerts()).containsExactly(updated);
  }

  @Test
  void removeAlertsRemovesOnlyTheGivenIds() {
    repository.addOrUpdateAlerts(FEED_A, List.of(ALERT_1, ALERT_2));
    repository.removeAlerts(FEED_A, List.of(id(FEED_A, "1")));

    assertThat(snapshot().getAllAlerts()).containsExactly(ALERT_2);
  }

  @Test
  void removeAlertsIgnoresUnknownIdsAndUnknownFeeds() {
    repository.addOrUpdateAlerts(FEED_A, List.of(ALERT_1));
    repository.removeAlerts(FEED_A, List.of(id(FEED_A, "does-not-exist")));
    repository.removeAlerts("no-such-feed", List.of(id(FEED_A, "1")));

    assertThat(snapshot().getAllAlerts()).containsExactly(ALERT_1);
  }

  @Test
  void feedsAreIsolatedFromEachOther() {
    repository.replaceAlerts(FEED_A, List.of(ALERT_1));
    repository.replaceAlerts(FEED_B, List.of(OTHER_FEED_ALERT));

    // Replacing the alerts of one feed must not touch the other feed.
    repository.replaceAlerts(FEED_A, List.of());
    assertThat(snapshot().getAllAlerts()).containsExactly(OTHER_FEED_ALERT);

    // Both alerts use the id "1" within their own feed, so removal has to be feed scoped as well.
    repository.removeAlerts(FEED_A, List.of(id(FEED_B, "1")));
    assertThat(snapshot().getAllAlerts()).containsExactly(OTHER_FEED_ALERT);
  }

  @Test
  void snapshotIsNotAffectedByLaterWrites() {
    repository.replaceAlerts(FEED_A, List.of(ALERT_1));
    var before = snapshot();

    repository.replaceAlerts(FEED_A, List.of(ALERT_2));

    assertThat(before.getAllAlerts()).containsExactly(ALERT_1);
    assertThat(snapshot().getAllAlerts()).containsExactly(ALERT_2);
  }

  @Test
  void alertsAreIndexedByAffectedEntity() {
    repository.replaceAlerts(FEED_A, List.of(ALERT_1));

    assertThat(snapshot().getStopAlerts(id(FEED_A, "stop-1"), Set.of())).containsExactly(ALERT_1);
    assertThat(snapshot().getStopAlerts(id(FEED_A, "stop-2"), Set.of())).isEmpty();
    assertThat(snapshot().getAlertById(id(FEED_A, "1"))).isEqualTo(ALERT_1);
    assertThat(snapshot().getAlertById(id(FEED_A, "unknown"))).isNull();
  }

  @Test
  void copyOnWriteCarriesAllCommittedAlertsForward() {
    repository.replaceAlerts(FEED_A, List.of(ALERT_1));
    repository.replaceAlerts(FEED_B, List.of(OTHER_FEED_ALERT));

    // A differential update in the next transaction builds on everything committed before.
    var next = snapshot().copyOnWrite();
    next.addOrUpdateAlerts(FEED_A, List.of(ALERT_2));

    assertEquals(2, next.getAlertCount(FEED_A));
    assertThat(next.freeze().getAllAlerts()).containsExactly(ALERT_1, ALERT_2, OTHER_FEED_ALERT);
  }

  @Test
  void writesToACopyDoNotLeakIntoTheSnapshotItWasCreatedFrom() {
    repository.replaceAlerts(FEED_A, List.of(ALERT_1));
    repository.replaceAlerts(FEED_B, List.of(OTHER_FEED_ALERT));
    var committed = snapshot();

    // This is what a rolled back transaction does: the copy is simply discarded.
    var discarded = committed.copyOnWrite();
    discarded.addOrUpdateAlerts(FEED_A, List.of(ALERT_2));
    discarded.removeAlerts(FEED_B, List.of(id(FEED_B, "1")));
    discarded.replaceAlerts(FEED_A, List.of());

    assertThat(committed.getAllAlerts()).containsExactly(ALERT_1, OTHER_FEED_ALERT);
    assertThat(committed.copyOnWrite().freeze().getAllAlerts()).containsExactly(
      ALERT_1,
      OTHER_FEED_ALERT
    );
  }

  private DefaultTransitAlertRepositorySnapshot snapshot() {
    return (DefaultTransitAlertRepositorySnapshot) repository.freeze();
  }

  private static TransitAlert alert(String feedId, String id) {
    return TransitAlert.of(id(feedId, id))
      .addEntity(new EntitySelector.Stop(id(feedId, "stop-" + id)))
      .build();
  }

  private static FeedScopedId id(String feedId, String id) {
    return new FeedScopedId(feedId, id);
  }
}
