package org.opentripplanner.standalone.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.FEED_ID;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.routing.alertpatch.AlertEffect.DETOUR;
import static org.opentripplanner.routing.alertpatch.AlertSeverity.INFO;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.framework.transaction.internal.TransactionFactory;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.alertpatch.TransitAlertBuilder;
import org.opentripplanner.service.transitalert.internal.DefaultTransitAlertRepository;
import org.opentripplanner.service.transitalert.internal.TransitAlertRepositoryLifecycle;

class AlertMetricsTest {

  @Test
  void noAlerts() {
    var binder = alertMetrics(List.of());
    var registry = new SimpleMeterRegistry();

    binder.bindTo(registry);
    binder.recordMetrics();

    assertEquals(List.of(), registry.getMeters());
  }

  @Test
  void registerMultiGauge() {
    var alert1 = alertBuilder("1").withSeverity(INFO).build();
    var alert2 = alertBuilder("2").withEffect(DETOUR).build();

    var binder = alertMetrics(List.of(alert1, alert2));
    var registry = new SimpleMeterRegistry();

    binder.bindTo(registry);
    binder.recordMetrics();

    var expected = """
    alerts(GAUGE)[feedId='F', severity='INFO']; value=1.0
    alerts(GAUGE)[effect='DETOUR', feedId='F']; value=1.0
    """.trim();
    assertEquals(expected, registry.getMetersAsString());
  }

  /**
   * Wire up a real repository registry holding the given alerts, so that the metrics binder can
   * resolve a snapshot the same way it does in production.
   */
  private static AlertMetrics alertMetrics(Collection<TransitAlert> alerts) {
    var repositoryRegistry = TransactionFactory.createRepositoryRegistry();
    var repository = new DefaultTransitAlertRepository();
    repository.replaceAlerts(FEED_ID, alerts);
    var handle = repositoryRegistry.registerRepository(
      repository,
      new TransitAlertRepositoryLifecycle()
    );
    return new AlertMetrics(repositoryRegistry, handle);
  }

  private static TransitAlertBuilder alertBuilder(String id) {
    var builder = TransitAlert.of(id(id)).withHeaderText(I18NString.of("a text"));
    builder.entities().add(new EntitySelector.Agency(id("agency")));
    return builder;
  }
}
