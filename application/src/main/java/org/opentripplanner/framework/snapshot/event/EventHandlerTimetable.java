package org.opentripplanner.framework.snapshot.event;

import org.opentripplanner.framework.snapshot.domain.timetable.repository.MutableTimetableSnapshot;

/**
 * A {@link EventHandler} that writes to the timetable repository.
 *
 * @param <E> the domain event type
 */
public non-sealed interface EventHandlerTimetable<E extends DomainEvent>
  extends EventHandler<E, MutableTimetableSnapshot> {}
