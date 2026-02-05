package org.opentripplanner.framework.snapshot.persistence.world;

import org.opentripplanner.framework.snapshot.domain.world.TransitWorld;
import org.opentripplanner.framework.snapshot.persistence.timetable.ImmutableTimetableRepo;
import org.opentripplanner.framework.snapshot.persistence.transfer.ImmutableTransferRepo;

public class ImmutableTransitWorld implements TransitWorld {

  private final ImmutableTransferRepo transferRepo;
  private final ImmutableTimetableRepo timetableRepo;

  public ImmutableTransitWorld(ImmutableTimetableRepo timetableRepo, ImmutableTransferRepo transferRepo) {
    this.transferRepo = transferRepo;
    this.timetableRepo = timetableRepo;
  }

  public ImmutableTransferRepo transfers() {
    return transferRepo;
  }

  public ImmutableTimetableRepo timetables() {
    return timetableRepo;
  }
}
