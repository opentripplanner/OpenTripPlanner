package org.opentripplanner.framework.snapshot.persistence.world;

import org.opentripplanner.framework.snapshot.domain.world.TransitWorld;
import org.opentripplanner.framework.snapshot.persistence.timetable.MutableTimetableRepo;
import org.opentripplanner.framework.snapshot.persistence.transfer.MutableTransferRepo;

public class MutableTransitWorld implements TransitWorld {

  private final MutableTimetableRepo timetableRepo;
  private final MutableTransferRepo transferRepo;

  public MutableTransitWorld(MutableTimetableRepo timetableRepo, MutableTransferRepo transferRepo) {
    this.timetableRepo = timetableRepo;
    this.transferRepo = transferRepo;
  }

  public static MutableTransitWorld from(ImmutableTransitWorld transitWorld) {
    return new MutableTransitWorld(
      MutableTimetableRepo.from(transitWorld.timetables()),
      MutableTransferRepo.from(transitWorld.transfers())
    );
  }

  public ImmutableTransitWorld freeze() {
    return new ImmutableTransitWorld(
      timetableRepo.freeze(),
      transferRepo.freeze()
    );
  }

  public MutableTimetableRepo timetables() {
    return timetableRepo;
  }

  public MutableTransferRepo transfers() {
    return transferRepo;
  }
}
