package org.opentripplanner.framework.snapshot;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
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
import org.opentripplanner.framework.snapshot.transaction.RepositoryHandle;
import org.opentripplanner.framework.snapshot.transaction.RepositoryRegistry;
import org.opentripplanner.framework.snapshot.transaction.RepositoryScope;
import org.opentripplanner.framework.snapshot.transaction.UpdateManager;
import org.opentripplanner.framework.snapshot.transaction.internal.TransactionConfig;

public class TransactionPrototypeTest {

  private static final RepositoryRegistry REPOSITORY_REGISTRY =
    TransactionConfig.createRepositoryRegistry();
  private static final RepositoryHandle<
    ReadOnlyTimetableSnapshot,
    MutableTimetableSnapshot
  > TIMETABLE_HANDLE = TimetableConfig.createRepo(REPOSITORY_REGISTRY);
  private static final RepositoryHandle<
    ReadOnlyTransferSnapshot,
    MutableTransferSnapshot
  > TRANSFER_HANDLE = TransferConfig.createRepo(REPOSITORY_REGISTRY);
  private static final UpdateManager UPDATE_MANAGER = TransactionConfig.createUpdateManager(
    REPOSITORY_REGISTRY
  );

  @BeforeAll
  static void beforeAll() {
    NewStopHandler newStopHandler = new NewStopHandler();
    UPDATE_MANAGER.register(newStopHandler, TRANSFER_HANDLE);
  }

  @Test
  public void handleReadOnlyRequest() {
    RepositoryScope scope = REPOSITORY_REGISTRY.scope();
    TimetableService timetableService = TimetableConfig.getRequestScopedTimetableService(
      scope,
      TIMETABLE_HANDLE
    );

    List<String> trips = timetableService.getTrips();
    assertThat(trips).isEmpty();
  }

  @Test
  public void handleUpdate() throws Exception {
    // given
    TripUpdate update = new TripUpdate("trip 1", true);

    // when
    UPDATE_MANAGER.submit(ctx -> {
      TripUpdateService updateService = TimetableConfig.createUpdateService(
        ctx.mutable(TIMETABLE_HANDLE),
        ctx::publish
      );
      updateService.doTripUpdate(update);
    }).get(5, TimeUnit.SECONDS);

    // then
    RepositoryScope scope = REPOSITORY_REGISTRY.scope();
    ReadOnlyTransferSnapshot transfers = scope.snapshot(TRANSFER_HANDLE);

    assertThat(transfers.getNumberOfRecalculations()).isEqualTo(1);
  }
}
