package org.opentripplanner.framework.snapshot.application;

import org.opentripplanner.framework.snapshot.domain.DomainEvent;
import org.opentripplanner.framework.snapshot.domain.timetable.TimetableRepo;
import org.opentripplanner.framework.snapshot.domain.transfer.TransferRepo;

public interface TransactionalContext {

  TimetableRepo timetable();

  TransferRepo transfers();

  void publish(DomainEvent event);
}
