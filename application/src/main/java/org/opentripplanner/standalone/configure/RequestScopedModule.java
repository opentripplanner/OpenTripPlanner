package org.opentripplanner.standalone.configure;

import dagger.Module;
import dagger.Provides;
import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.api.TransactionScope;
import org.opentripplanner.standalone.api.HttpRequestScoped;
import org.opentripplanner.transit.repository.MutableTimetableSnapshot;
import org.opentripplanner.transit.repository.ReadOnlyTimetableSnapshot;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;

/**
 * Provides the bindings that live inside {@link RequestScopedComponent}. A single {@link
 * TransactionScope} is captured once per request, and every other binding here is derived from
 * that same scope, so they all see a consistent, pinned view of real-time data.
 */
@Module
public class RequestScopedModule {

  @Provides
  @HttpRequestScoped
  static TransactionScope transactionScope(RepositoryRegistry repositoryRegistry) {
    return repositoryRegistry.scope();
  }

  @Provides
  @HttpRequestScoped
  static TransitService transitService(
    TimetableRepository timetableRepository,
    RepositoryHandle<ReadOnlyTimetableSnapshot, MutableTimetableSnapshot> timetableRepositoryHandle,
    TransactionScope transactionScope
  ) {
    var timetableSnapshot = timetableRepositoryHandle.repositorySnapshot(transactionScope);
    return new DefaultTransitService(timetableRepository, timetableSnapshot);
  }
}
