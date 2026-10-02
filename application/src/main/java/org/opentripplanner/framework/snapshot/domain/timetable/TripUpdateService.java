package org.opentripplanner.framework.snapshot.domain.timetable;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.opentripplanner.framework.snapshot.domain.NewStopUsedByTripPattern;
import org.opentripplanner.framework.snapshot.domain.TripUpdate;
import org.opentripplanner.framework.snapshot.domain.timetable.repository.MutableTimetableSnapshot;
import org.opentripplanner.framework.snapshot.event.DomainEvent;

public class TripUpdateService {

  private final Supplier<MutableTimetableSnapshot> ctx;
  private final Consumer<DomainEvent> domainEventConsumer;

  public TripUpdateService(
    Supplier<MutableTimetableSnapshot> timetable,
    Consumer<DomainEvent> eventPublisher
  ) {
    this.ctx = timetable;
    this.domainEventConsumer = eventPublisher;
  }

  public void doTripUpdate(TripUpdate update) {
    ctx.get().addTrip(update.tripId());
    if (update.newStopUsed()) {
      domainEventConsumer.accept(new NewStopUsedByTripPattern());
    }
  }
}
