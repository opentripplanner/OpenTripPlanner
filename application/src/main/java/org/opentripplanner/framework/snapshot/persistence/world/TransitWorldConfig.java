package org.opentripplanner.framework.snapshot.persistence.world;

import org.opentripplanner.framework.snapshot.persistence.timetable.TimetableConfig;
import org.opentripplanner.framework.snapshot.persistence.transfer.TransferConfig;

public class TransitWorldConfig {

  public static ImmutableTransitWorld provideTransitWorld() {
    return new ImmutableTransitWorld(
      TimetableConfig.provideTimetableRepo(),
      TransferConfig.provideTransferRepo()
    );
  }
}
