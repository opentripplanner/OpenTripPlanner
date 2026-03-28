package org.opentripplanner.framework.snapshot.domain.world;

import org.opentripplanner.framework.snapshot.domain.timetable.TimetableRepo;
import org.opentripplanner.framework.snapshot.domain.transfer.TransferRepo;

// ToDo: vllt in event/application irgendwo hin
public interface TransitWorld {

  TransferRepo transfers();

  TimetableRepo timetables();
}
