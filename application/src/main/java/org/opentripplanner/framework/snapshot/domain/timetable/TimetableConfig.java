package org.opentripplanner.framework.snapshot.domain.timetable;

import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.framework.snapshot.event.DomainEvent;
import org.opentripplanner.framework.snapshot.domain.timetable.repository.MutableTimetableSnapshot;
import org.opentripplanner.framework.snapshot.domain.timetable.repository.ReadOnlyTimetableSnapshot;
import org.opentripplanner.framework.snapshot.domain.timetable.repository.TimetableSnapshotLifecycle;
import org.opentripplanner.framework.snapshot.transaction.RepositoryHandle;
import org.opentripplanner.framework.snapshot.transaction.RepositoryRegistry;
import org.opentripplanner.framework.snapshot.transaction.RepositoryScope;

/**
 * wiring, this will be done by dagger
 */
public class TimetableConfig {

  public static RepositoryHandle<ReadOnlyTimetableSnapshot, MutableTimetableSnapshot> createRepo(RepositoryRegistry repositoryRegistry) {
    // this is a singleton
    return (repositoryRegistry.register(
      new ReadOnlyTimetableSnapshot(List.of()), new TimetableSnapshotLifecycle()
    ));
  }

  public static TripUpdateService createUpdateService(
    RepositoryHandle<ReadOnlyTimetableSnapshot, MutableTimetableSnapshot> repo,
    Consumer<DomainEvent> eventPublisher
  ) {
    // singleton
    return new TripUpdateService(repo.mutableSnapshot(), eventPublisher);
  }

  public static TimetableService getRequestScopedTimetableService(
    RepositoryScope scope, RepositoryHandle<ReadOnlyTimetableSnapshot, MutableTimetableSnapshot> timetableRepository
  ) {
    ReadOnlyTimetableSnapshot snapshot = scope.snapshot(timetableRepository);
    return new TimetableService(snapshot);
  }
}
