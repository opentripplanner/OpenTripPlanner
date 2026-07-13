package org.opentripplanner.standalone.server;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.function.Supplier;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.configure.RequestScopedComponent;

/**
 * HK2-constructed bridge that resolves {@link OtpServerRequestContext} from the current HTTP
 * request's Dagger {@link RequestScopedComponent}. See {@link
 * OTPWebApplication#makeRequestScopedBinder}.
 */
class RequestScopedServerContextSupplier implements Supplier<OtpServerRequestContext> {

  private final Provider<RequestScopedComponent> requestScopedComponent;

  @Inject
  RequestScopedServerContextSupplier(Provider<RequestScopedComponent> requestScopedComponent) {
    this.requestScopedComponent = requestScopedComponent;
  }

  @Override
  public OtpServerRequestContext get() {
    return requestScopedComponent.get().createServerContext();
  }
}
