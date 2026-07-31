package org.opentripplanner.transit.repository;

import java.time.LocalDate;
import java.util.function.Supplier;
import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;

public class TimetableSnapshotLifecycle
  implements RepositoryLifecycle<ReadOnlyTimetableSnapshot, MutableTimetableSnapshot> {

  private final MutableTimetableSnapshot buffer;
  private final boolean purgeExpiredData;
  private final Supplier<LocalDate> localDateNow;
  private LocalDate lastPurgeDate = null;

  public TimetableSnapshotLifecycle(
    MutableTimetableSnapshot buffer,
    boolean purgeExpiredData,
    Supplier<LocalDate> localDateNow
  ) {
    this.buffer = buffer;
    this.purgeExpiredData = purgeExpiredData;
    this.localDateNow = localDateNow;
  }

  @Override
  public MutableTimetableSnapshot copyOnWrite(ReadOnlyTimetableSnapshot readOnlySnapshot) {
    return buffer;
  }

  @Override
  public ReadOnlyTimetableSnapshot freeze(MutableTimetableSnapshot mutableSnapshot) {
    if (purgeExpiredData) {
      final LocalDate today = localDateNow.get();
      // Keep data for today and the previous day; purge anything older
      final LocalDate previously = today.minusDays(2);
      if (lastPurgeDate == null || lastPurgeDate.compareTo(previously) < 0) {
        lastPurgeDate = previously;
        buffer.purgeExpiredData(previously);
      }
    }
    return buffer.createReadOnlySnapshot();
  }
}
