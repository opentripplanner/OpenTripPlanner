package org.opentripplanner.standalone.server;

import jakarta.inject.Provider;
import java.util.function.Function;
import java.util.function.Supplier;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.configure.RequestScopedFactory;
import org.opentripplanner.transit.service.TransitService;

/**
 * Bridges the Dagger-managed {@link RequestScopedFactory} into HK2/Jersey: the component itself,
 * {@link OtpServerRequestContext} (grab-bag of everything not yet migrated), and individual
 * request-scoped services (currently just {@link TransitService}) that resources can inject
 * directly with {@code @Context}.
 * <p>
 * All bindings are scoped {@code .in(RequestScoped.class)} — Jersey's own per-actual-HTTP-request
 * HK2 scope — so they all resolve from the SAME pinned transaction for a given request. See issue
 * #7441.
 * <p>
 * More on custom injection in Jersey 2:
 * http://jersey.576304.n2.nabble.com/Custom-providers-in-Jersey-2-tp7580699p7580715.html
 */
final class DaggerToJerseyBridge extends AbstractBinder {

  private final Supplier<RequestScopedFactory> factorySupplier;

  DaggerToJerseyBridge(Supplier<RequestScopedFactory> factorySupplier) {
    this.factorySupplier = factorySupplier;
  }

  @Override
  protected void configure() {
    // factorySupplier.get() calls the Dagger builder directly, creating a brand-new
    // RequestScopedFactory (and transaction pin) on every call, so it must only be invoked once
    // per request, right here, as the source behind the request-scoped binding below.
    bindFactory(factorySupplier).to(RequestScopedFactory.class).in(RequestScoped.class);

    // factory.get() is different: it's an HK2 service lookup against the binding above, not a
    // call to the Dagger builder. HK2's RequestScoped context caches that binding's result for
    // the lifetime of the request, so every accessor lambda below — no matter how many times it's
    // invoked — resolves the SAME RequestScopedFactory instance the request already has, exactly
    // as @Context/@Inject would.
    var factory = createManagedInstanceProvider(RequestScopedFactory.class);

    // Binding for all request-scoped services used by resources
    bridge(factory, RequestScopedFactory::transitService, TransitService.class);
    bridge(factory, RequestScopedFactory::createServerContext, OtpServerRequestContext.class);
  }

  /**
   * Create a binding for a request-scoped service using the given accessor function and the
   * factory.
   */
  private <T> void bridge(
    Provider<RequestScopedFactory> factory,
    Function<RequestScopedFactory, T> accessor,
    Class<T> type
  ) {
    bindFactory(() -> accessor.apply(factory.get()))
      .to(type)
      .in(RequestScoped.class);
  }
}
