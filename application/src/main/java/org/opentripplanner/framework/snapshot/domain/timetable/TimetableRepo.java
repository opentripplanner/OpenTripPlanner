package org.opentripplanner.framework.snapshot.domain.timetable;

/**
 * Only save (set) and find (get) methods, NO business logic
 */
public interface TimetableRepo {

  String getTripId();

  void setTripId(String tripId);
}
