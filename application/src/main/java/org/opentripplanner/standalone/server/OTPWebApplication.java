package org.opentripplanner.standalone.server;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jersey.server.DefaultJerseyTagsProvider;
import io.micrometer.core.instrument.binder.jersey.server.MetricsApplicationEventListener;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.opentripplanner.api.common.OTPExceptionMapper;
import org.opentripplanner.apis.APIEndpoints;
import org.opentripplanner.ext.httpresponsetimemetrics.HttpResponseTimeMetricsFilter;
import org.opentripplanner.ext.httpresponsetimemetrics.HttpResponseTimeMetricsParameters;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.standalone.configure.RequestScopedFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * A JAX-RS Application subclass which provides hard-wired configuration of an OTP server. Avoids
 * auto-scanning of any kind, and keeps injection to a bare minimum using HK2, the injection library
 * Jersey itself uses.
 * <p>
 * Jersey has its own ResourceConfig class which is a subclass of Application. We can get away with
 * not using any Jersey-specific "conveniences" and stick with stock JAX-RS.
 */
public class OTPWebApplication extends Application {

  /* Builds one Dagger RequestScopedFactory per actual HTTP request, see issue #7441. */
  private final Supplier<RequestScopedFactory> requestScopedFactoryProvider;

  private final List<Class<? extends ContainerResponseFilter>> customFilters;
  private final HttpResponseTimeMetricsParameters httpResponseTimeMetricsParameters;

  static {
    // Remove existing handlers attached to the j.u.l root logger
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    // Bridge j.u.l (used by Jersey) to the SLF4J root logger, so all logging goes through the same
    // API
    SLF4JBridgeHandler.install();
  }

  public OTPWebApplication(
    OTPWebApplicationParameters parameters,
    Supplier<RequestScopedFactory> requestScopedComponentProvider
  ) {
    this.requestScopedFactoryProvider = requestScopedComponentProvider;
    this.customFilters = createCustomFilters(parameters.traceParameters());
    this.httpResponseTimeMetricsParameters = parameters.httpResponseTimeMetricsParameters();
  }

  /**
   * This method registers classes with Jersey to define web resources and enable custom features.
   * These are classes (not instances) that will be instantiated by Jersey for each request (they
   * are request-scoped). Types that have been confirmed to work are: annotated resources, {@code
   * ContextResolver<ObjectMapper>} implementation, ContainerResponseFilter and
   * ContainerRequestFilter. Note that the listed classes do not need to be annotated with @Provider
   * -- that is for scanning config.
   */
  @Override
  public Set<Class<?>> getClasses() {
    // Add API Endpoints defined in the api package
    Set<Class<?>> classes = new HashSet<>(APIEndpoints.listAPIEndpoints());

    classes.addAll(resolveFilterClasses());

    return classes;
  }

  /**
   * Features and Filters: extend Jersey, manipulate requests and responses.
   */
  private Set<Class<? extends ContainerResponseFilter>> resolveFilterClasses() {
    var set = new HashSet<Class<? extends ContainerResponseFilter>>();
    set.addAll(customFilters);
    set.add(CorsFilter.class);
    set.add(EtagRequestFilter.class);
    set.add(VaryRequestFilter.class);
    return set;
  }

  /**
   * Like getClasses, this method declares web resources, providers, and features to the JAX-RS
   * implementation. However, these are single instances that will be reused for all requests (they
   * are singleton-scoped).
   * <p>
   * See https://jersey.java.net/apidocs/latest/jersey/javax/ws/rs/core/Application.html#getSingletons()
   * Leave {@code <Object>} out of method signature to avoid confusing the Guava type inference.
   */
  @Override
  public Set<Object> getSingletons() {
    var singletons = new HashSet<>(
      List.of(
        // Show exception messages in responses
        new OTPExceptionMapper(),
        // Enable Jackson JSON response serialization
        new JacksonJsonProvider(),
        // Inject the OTP request-scoped services (e.g. TransitService) per HTTP request
        new DaggerToJerseyBridge(requestScopedFactoryProvider),
        // Add performance instrumentation of Jersey requests to micrometer
        getMetricsApplicationEventListener()
      )
    );

    if (OTPFeature.ActuatorAPI.isOn()) {
      singletons.add(getBoundPrometheusRegistry());

      // Add HTTP response time metrics filter if enabled
      if (OTPFeature.HttpResponseTimeMetrics.isOn()) {
        singletons.add(
          new HttpResponseTimeMetricsFilter(
            httpResponseTimeMetricsParameters.clientHeader(),
            httpResponseTimeMetricsParameters.monitoredClients(),
            httpResponseTimeMetricsParameters.monitoredEndpoints(),
            httpResponseTimeMetricsParameters.metricName(),
            httpResponseTimeMetricsParameters.minExpectedResponseTime(),
            httpResponseTimeMetricsParameters.maxExpectedResponseTime()
          )
        );
      }
    }

    return singletons;
  }

  /**
   * Disable auto-discovery of features because it's extremely obnoxious to debug and
   * interacts in confusing ways with manually registered features.
   */
  @Override
  public Map<String, Object> getProperties() {
    return Map.of(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, Boolean.TRUE);
  }

  private MetricsApplicationEventListener getMetricsApplicationEventListener() {
    return new MetricsApplicationEventListener(
      Metrics.globalRegistry,
      new DefaultJerseyTagsProvider(),
      "http.server.requests",
      true
    );
  }

  /**
   * Instantiate and add the prometheus micrometer registry to the global composite registry.
   *
   * @return An AbstractBinder, which can be used to inject the registry into the Actuator API calls
   */
  private Binder getBoundPrometheusRegistry() {
    PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(
      PrometheusConfig.DEFAULT
    );

    Metrics.globalRegistry.add(prometheusRegistry);

    return new AbstractBinder() {
      @Override
      protected void configure() {
        bind(prometheusRegistry).to(PrometheusMeterRegistry.class);
      }
    };
  }

  private List<Class<? extends ContainerResponseFilter>> createCustomFilters(
    List<RequestTraceParameter> traceParameters
  ) {
    if (traceParameters.isEmpty()) {
      return List.of();
    }
    RequestTraceFilter.init(traceParameters);

    return List.of(RequestTraceFilter.class);
  }
}
