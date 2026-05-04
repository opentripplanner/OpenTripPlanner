package org.opentripplanner.framework.snapshot;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.snapshot.domain.TripUpdate;
import org.opentripplanner.framework.snapshot.domain.timetable.TimetableConfig;
import org.opentripplanner.framework.snapshot.domain.timetable.TimetableService;
import org.opentripplanner.framework.snapshot.domain.timetable.TripUpdateService;
import org.opentripplanner.framework.snapshot.domain.timetable.repository.MutableTimetableSnapshot;
import org.opentripplanner.framework.snapshot.domain.timetable.repository.ReadOnlyTimetableSnapshot;
import org.opentripplanner.framework.snapshot.domain.transfer.NewStopHandler;
import org.opentripplanner.framework.snapshot.domain.transfer.TransferConfig;
import org.opentripplanner.framework.snapshot.domain.transfer.repository.MutableTransferSnapshot;
import org.opentripplanner.framework.snapshot.domain.transfer.repository.ReadOnlyTransferSnapshot;
import org.opentripplanner.framework.snapshot.event.EventConfig;
import org.opentripplanner.framework.snapshot.event.EventDispatcher;
import org.opentripplanner.framework.snapshot.transaction.RepositoryHandle;
import org.opentripplanner.framework.snapshot.transaction.RepositoryRegistry;
import org.opentripplanner.framework.snapshot.transaction.RepositoryScope;
import org.opentripplanner.framework.snapshot.transaction.TransactionConfig;

public class TransactionPrototypeTest {

  private static final RepositoryRegistry repositoryRegistry = TransactionConfig.createRepositoryRegistry();
  private static final RepositoryHandle<ReadOnlyTimetableSnapshot, MutableTimetableSnapshot> timetableRepository = TimetableConfig.createRepo(repositoryRegistry);
  private static final RepositoryHandle<ReadOnlyTransferSnapshot, MutableTransferSnapshot> transferRepository = TransferConfig.createRepo(repositoryRegistry);
  private static final EventDispatcher dispatcher = EventConfig.createEventDispatcher();
  private static final NewStopHandler newStopHandler = TransferConfig.createApplicationScopedStopHandler(dispatcher, transferRepository.mutableSnapshot());
  private static final TripUpdateService tripUpdateService= TimetableConfig.createUpdateService(timetableRepository, dispatcher::publish);

  @Test
  public void handleReadOnlyRequest() {

    RepositoryScope scope = repositoryRegistry.scope();
    TimetableService timetableService = TimetableConfig.getRequestScopedTimetableService(
      scope, timetableRepository);

    List<String> trips = timetableService.getTrips();
    assertThat(trips).isEmpty();
  }

  @Test
  public void handleUpdate() {

    tripUpdateService.doTripUpdate(new TripUpdate("trip 1", true));

    repositoryRegistry.commit();

    RepositoryScope scope = repositoryRegistry.scope();
    ReadOnlyTransferSnapshot transfers = scope.snapshot(transferRepository);

    assertThat(transfers.getNumberOfRecalculations()).isEqualTo(1);
  }
}
