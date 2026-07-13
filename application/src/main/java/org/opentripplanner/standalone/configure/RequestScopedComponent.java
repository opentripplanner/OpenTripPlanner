package org.opentripplanner.standalone.configure;

import dagger.Subcomponent;
import org.opentripplanner.framework.transaction.api.TransactionScope;
import org.opentripplanner.standalone.api.HttpRequestScoped;
import org.opentripplanner.transit.service.TransitService;

/**
 * A Dagger subcomponent scoped to the lifetime of one HTTP request. Every binding here is
 * derived from a single {@link TransactionScope} captured once per {@link #build()}, so they are
 * guaranteed to be consistent with each other — no possibility of a mid-request update being
 * visible to one binding but not another.
 * <p>
 * Not yet wired into the Jersey/HK2 bridge — see issue #7441. Build one instance per actual HTTP
 * request (never reuse across requests, never share across concurrent requests).
 */
@HttpRequestScoped
@Subcomponent(modules = { RequestScopedModule.class })
public interface RequestScopedComponent {
  TransactionScope transactionScope();

  TransitService transitService();

  @Subcomponent.Builder
  interface Builder {
    RequestScopedComponent build();
  }
}
