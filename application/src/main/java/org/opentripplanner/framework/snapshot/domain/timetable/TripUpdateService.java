package org.opentripplanner.framework.snapshot.domain.timetable;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.opentripplanner.framework.snapshot.domain.NewStopUsedByTripPattern;
import org.opentripplanner.framework.snapshot.domain.TripUpdate;
import org.opentripplanner.framework.snapshot.domain.timetable.repository.MutableTimetableSnapshot;
import org.opentripplanner.framework.snapshot.event.DomainEvent;

public class TripUpdateService {

  private final Supplier<MutableTimetableSnapshot> timetableSnapshot;
  private final Consumer<DomainEvent> publisher;

  public TripUpdateService(Supplier<MutableTimetableSnapshot> timetableSnapshot, Consumer<DomainEvent> publisher) {
    this.timetableSnapshot = timetableSnapshot;
    this.publisher = publisher;
  }

  public void doTripUpdate(TripUpdate update) {
    timetableSnapshot.get().addTrip(update.tripId());
    if (update.newStopUsed()) {
      publisher.accept(new NewStopUsedByTripPattern());
    }
  }
}
