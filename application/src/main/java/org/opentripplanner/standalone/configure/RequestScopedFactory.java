package org.opentripplanner.standalone.configure;

import dagger.Subcomponent;
import org.opentripplanner.framework.transaction.api.TransactionScope;
import org.opentripplanner.standalone.api.HttpRequestScoped;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitService;

/**
 * A Dagger subcomponent scoped to the lifetime of one HTTP request. Every binding here is derived
 * from a single {@link TransactionScope} captured once per {@link Builder#build()}, so they are
 * guaranteed to be consistent with each other — no possibility of a mid-request update being
 * visible to one binding but not another.
 * <p>
 * Build one instance per actual HTTP request (never reuse across requests, never share across
 * concurrent requests) — see issue #7441.
 */
@HttpRequestScoped
@Subcomponent(modules = { RequestScopedModule.class })
public interface RequestScopedFactory {
  TransactionScope transactionScope();

  TransitService transitService();

  OtpServerRequestContext createServerContext();

  @Subcomponent.Builder
  interface Builder {
    RequestScopedFactory build();
  }
}
