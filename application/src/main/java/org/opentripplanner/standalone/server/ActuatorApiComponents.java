package org.opentripplanner.standalone.server;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.util.Set;
import org.glassfish.jersey.inject.hk2.AbstractBinder;
import org.opentripplanner.ext.actuator.ActuatorAPI;

/**
 * The Actuator API as {@link JerseyComponents}: the resource together with the Prometheus registry
 * it exports, so they are always registered with the same application.
 */
public final class ActuatorApiComponents {

  private ActuatorApiComponents() {}

  /**
   * @param prometheusRegistry the registry exported by the Prometheus endpoint, the same instance
   *                           as used by the rest of OTP
   */
  public static JerseyComponents create(PrometheusMeterRegistry prometheusRegistry) {
    return new JerseyComponents(
      Set.of(ActuatorAPI.class),
      Set.of(new PrometheusRegistryBinder(prometheusRegistry))
    );
  }

  /** Makes the registry available to the Actuator API, which injects it with {@code @Context}. */
  private static final class PrometheusRegistryBinder extends AbstractBinder {

    private final PrometheusMeterRegistry registry;

    private PrometheusRegistryBinder(PrometheusMeterRegistry registry) {
      this.registry = registry;
    }

    @Override
    protected void configure() {
      bind(registry).to(PrometheusMeterRegistry.class);
    }
  }
}
