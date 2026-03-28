package org.opentripplanner.framework.snapshot.persistence.snapshot.world;

import org.opentripplanner.framework.snapshot.persistence.repository.timetable.TimetableConfig;
import org.opentripplanner.framework.snapshot.persistence.repository.transfer.TransferConfig;

public class TransitWorldConfig {

  public static ImmutableTransitWorld provideTransitWorld() {
    return new ImmutableTransitWorld(
      TimetableConfig.provideTimetableRepo(),
      TransferConfig.provideTransferRepo()
    );
  }
}
