package org.opentripplanner.framework.snapshot.domain.timetable;

import java.util.function.Consumer;
import org.opentripplanner.framework.snapshot.domain.TripUpdate;
import org.opentripplanner.framework.snapshot.domain.world.NewStopUsedByTripPattern;
import org.opentripplanner.framework.snapshot.domain.DomainEvent;

public class TripUpdateService {

  private final TimetableRepo timetableRepo;
  private final Consumer<DomainEvent> publisher;

  public TripUpdateService(TimetableRepo timetableRepo, Consumer<DomainEvent> publisher) {
    this.timetableRepo = timetableRepo;
    this.publisher = publisher;
  }

  public void doTripUpdate(TripUpdate update) {
    timetableRepo.setTripId(update.tripId());
    if (update.newStopUsed()) {
      publisher.accept(new NewStopUsedByTripPattern());
    }
  }
}
