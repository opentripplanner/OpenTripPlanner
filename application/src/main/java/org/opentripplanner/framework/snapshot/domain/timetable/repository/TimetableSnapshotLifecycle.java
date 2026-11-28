package org.opentripplanner.framework.snapshot.domain.timetable.repository;

import org.opentripplanner.framework.snapshot.transaction.RepositoryLifecycle;

public class TimetableSnapshotLifecycle
  implements RepositoryLifecycle<ReadOnlyTimetableSnapshot, MutableTimetableSnapshot> {

  @Override
  public MutableTimetableSnapshot copyOnWrite(ReadOnlyTimetableSnapshot readOnlySnapshot) {
    return new MutableTimetableSnapshot(readOnlySnapshot.getTrips());
  }

  @Override
  public ReadOnlyTimetableSnapshot freeze(MutableTimetableSnapshot mutableSnapshot) {
    return new ReadOnlyTimetableSnapshot(mutableSnapshot.getTrips());
  }
}
