package org.opentripplanner.standalone.server;

import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.glassfish.jersey.CommonProperties;
import org.opentripplanner.api.common.OTPExceptionMapper;
import org.opentripplanner.standalone.configure.RequestScopedFactory;

/**
 * A minimal JAX-RS Application serving only the Actuator API. It is used when the Actuator API has
 * its own port, so health probes and metrics scraping are reachable from the platform without
 * exposing them, or any other API, through the public ingress.
 * <p>
 * Unlike the {@link OTPWebApplication}, it does not record HTTP request metrics, so probes and
 * scrapes do not show up next to the real traffic.
 */
public class ActuatorWebApplication extends Application {

  private final Supplier<RequestScopedFactory> requestScopedFactoryProvider;
  private final JerseyComponents actuatorApi;

  /**
   * @param requestScopedFactoryProvider provides the request-scoped services, like the
   *                                     {@code TransitService} used by the health check
   * @param actuatorApi                  the Actuator API, see {@link ActuatorApiComponents}
   */
  public ActuatorWebApplication(
    Supplier<RequestScopedFactory> requestScopedFactoryProvider,
    JerseyComponents actuatorApi
  ) {
    this.requestScopedFactoryProvider = requestScopedFactoryProvider;
    this.actuatorApi = actuatorApi;
  }

  @Override
  public Set<Class<?>> getClasses() {
    return actuatorApi.classes();
  }

  @Override
  public Set<Object> getSingletons() {
    var singletons = new HashSet<>(actuatorApi.singletons());
    // Keep the error responses the same as when served together with the other APIs
    singletons.add(new OTPExceptionMapper());
    singletons.add(new DaggerToJerseyBridge(requestScopedFactoryProvider));
    return singletons;
  }

  /** Disable auto-discovery of features, see {@link OTPWebApplication#getProperties()}. */
  @Override
  public Map<String, Object> getProperties() {
    return Map.of(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, Boolean.TRUE);
  }
}
