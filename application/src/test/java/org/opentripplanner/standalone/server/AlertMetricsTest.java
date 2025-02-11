package org.opentripplanner.standalone.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.alertpatch.AlertEffect.DETOUR;
import static org.opentripplanner.routing.alertpatch.AlertSeverity.INFO;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.alertpatch.TransitAlertBuilder;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.transit.service.TimetableRepository;

class AlertMetricsTest {

  @Test
  void nullService() {
    var binder = new AlertMetrics(() -> null);
    var registry = new SimpleMeterRegistry();

    binder.bindTo(registry);
    binder.recordMetrics();

    assertEquals(List.of(), registry.getMeters());
  }

  @Test
  void registerMultiGauge() {
    var alert1 = alertBuilder("1").withSeverity(INFO).build();
    var alert2 = alertBuilder("2").withEffect(DETOUR).build();
    var service = new TransitAlertServiceImpl(new TimetableRepository());
    service.setAlerts(List.of(alert1, alert2));

    var binder = new AlertMetrics(() -> service);
    var registry = new SimpleMeterRegistry();

    binder.bindTo(registry);
    binder.recordMetrics();

    var expected =
      """
      alerts(GAUGE)[feedId='F', severity='INFO']; value=1.0
      alerts(GAUGE)[effect='DETOUR', feedId='F']; value=1.0
      """.trim();
    assertEquals(expected, registry.getMetersAsString());
  }

  private static TransitAlertBuilder alertBuilder(String id) {
    var builder = TransitAlert.of(id(id)).withHeaderText(I18NString.of("a text"));
    builder.entities().add(new EntitySelector.Agency(id("agency")));
    return builder;
  }
}
