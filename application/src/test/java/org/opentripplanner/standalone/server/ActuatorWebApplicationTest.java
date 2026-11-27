package org.opentripplanner.standalone.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.binder.jersey.server.MetricsApplicationEventListener;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.actuator.ActuatorAPI;

class ActuatorWebApplicationTest {

  private final JerseyComponents actuatorApi = ActuatorApiComponents.create(
    new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
  );
  private final ActuatorWebApplication subject = new ActuatorWebApplication(
    () -> null,
    actuatorApi
  );

  @Test
  void servesOnlyTheActuatorAPI() {
    assertEquals(Set.of(ActuatorAPI.class), subject.getClasses());
  }

  @Test
  void registersThePrometheusRegistryAndTheRequestScopedServices() {
    var singletons = subject.getSingletons();

    assertTrue(singletons.containsAll(actuatorApi.singletons()));
    assertTrue(singletons.stream().anyMatch(DaggerToJerseyBridge.class::isInstance));
  }

  @Test
  void doesNotRecordHttpRequestMetrics() {
    assertTrue(
      subject.getSingletons().stream().noneMatch(MetricsApplicationEventListener.class::isInstance)
    );
  }
}
