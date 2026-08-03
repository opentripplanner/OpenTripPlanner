package org.opentripplanner.transit.repository;

import java.time.LocalDate;
import java.util.function.Supplier;
import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;

/**
 * Copy-on-write / freeze lifecycle for the realtime-timetable repository. The repository is a
 * single long-lived buffer shared by all transactions: {@link #copyOnWrite} hands out the same
 * buffer instance, and {@link #freeze} publishes an immutable snapshot of it (after optionally
 * purging expired data).
 */
public class TimetableRepositoryLifecycle
  implements RepositoryLifecycle<TimetableRepositorySnapshot, TimetableRepository> {

  private final TimetableRepository buffer;
  private final boolean purgeExpiredData;
  private final Supplier<LocalDate> localDateNow;
  private LocalDate lastPurgeDate = null;

  public TimetableRepositoryLifecycle(
    TimetableRepository buffer,
    boolean purgeExpiredData,
    Supplier<LocalDate> localDateNow
  ) {
    this.buffer = buffer;
    this.purgeExpiredData = purgeExpiredData;
    this.localDateNow = localDateNow;
  }

  @Override
  public TimetableRepository copyOnWrite(TimetableRepositorySnapshot snapshot) {
    return buffer;
  }

  @Override
  public TimetableRepositorySnapshot freeze(TimetableRepository repository) {
    if (purgeExpiredData) {
      final LocalDate today = localDateNow.get();
      // Keep data for today and the previous day; purge anything older
      final LocalDate previously = today.minusDays(2);
      if (lastPurgeDate == null || lastPurgeDate.compareTo(previously) < 0) {
        lastPurgeDate = previously;
        buffer.purgeExpiredData(previously);
      }
    }
    return buffer.createSnapshot();
  }
}
