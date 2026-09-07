package org.opentripplanner.service.transitalert.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.internal.TransactionFactory;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;

/**
 * Verifies the rollback contract the alert write domain relies on: alert updaters run on an
 * {@link org.opentripplanner.framework.transaction.UpdateManager} with atomic commits, so a task
 * that throws must leave no trace in the published snapshot, while the alerts committed by earlier
 * tasks - the state a differential feed builds on - are kept.
 */
class TransitAlertRepositoryLifecycleTest {

  private static final String FEED = "F";
  private static final TransitAlert ALERT_1 = alert("1");
  private static final TransitAlert ALERT_2 = alert("2");

  private final RepositoryRegistry registry = TransactionFactory.createRepositoryRegistry();

  @Test
  void aFailedTaskIsRolledBackAndDoesNotAffectAlreadyCommittedAlerts() throws Exception {
    var handle = registry.registerRepository(
      new DefaultTransitAlertRepository(),
      new TransitAlertRepositoryLifecycle()
    );
    var updateManager = TransactionFactory.createUpdateManagerWithAtomicCommits(
      "test-alert",
      registry,
      Executors.defaultThreadFactory()
    );

    updateManager
      .submit(ctx -> ctx.repository(handle).addOrUpdateAlerts(FEED, List.of(ALERT_1)))
      .get();

    var failing = updateManager.submit(ctx -> {
      ctx.repository(handle).addOrUpdateAlerts(FEED, List.of(ALERT_2));
      throw new RuntimeException("Mapping the second half of the delivery failed");
    });
    assertThrows(ExecutionException.class, failing::get);

    assertThat(handle.repositorySnapshot(registry.scope()).getAllAlerts()).containsExactly(ALERT_1);

    // The next task still builds on the alerts committed before the failure.
    updateManager
      .submit(ctx -> ctx.repository(handle).addOrUpdateAlerts(FEED, List.of(ALERT_2)))
      .get();
    assertThat(handle.repositorySnapshot(registry.scope()).getAllAlerts()).containsExactly(
      ALERT_1,
      ALERT_2
    );

    updateManager.shutdown();
  }

  private static TransitAlert alert(String id) {
    var alertId = new FeedScopedId(FEED, id);
    return TransitAlert.of(alertId)
      .addEntity(new EntitySelector.Stop(new FeedScopedId(FEED, "stop-" + id)))
      .build();
  }
}
