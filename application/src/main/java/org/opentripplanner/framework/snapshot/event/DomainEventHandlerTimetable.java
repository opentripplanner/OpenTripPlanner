package org.opentripplanner.framework.snapshot.event;

import org.opentripplanner.framework.snapshot.domain.DomainEvent;
import org.opentripplanner.framework.snapshot.domain.timetable.TimetableRepo;

public non-sealed interface DomainEventHandlerTimetable<T extends DomainEvent> extends DomainEventHandler<T> {

    void handle(T event, TimetableRepo timetables);
}
