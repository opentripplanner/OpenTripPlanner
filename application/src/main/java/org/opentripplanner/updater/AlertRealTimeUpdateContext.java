package org.opentripplanner.updater;

import org.opentripplanner.service.transitalert.TransitAlertRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.trip.gtfs.GtfsRealtimeFuzzyTripMatcher;

/**
 * Give access to the transit data in the context of a real-time update task in the alert write
 * domain. The services exposed must be used only from the alert domain's writer thread.
 * <p>
 * Unlike {@link TransitRealTimeUpdateContext}, the transit data reachable from here is read-only
 * and resolved from the <em>last committed</em> timetable snapshot: the alerts have a writer thread
 * of their own and must not touch the timetable write buffer, which belongs to the transit domain.
 * In practice this means that an alert referring to a real-time added trip that was created within
 * the last commit interval resolves on a later poll instead.
 */
public interface AlertRealTimeUpdateContext {
  /**
   * Return the mutable transit-alert repository for this update task. Callers must only use this
   * from the single writer thread. Accessing it causes the current transaction to publish a new
   * alert snapshot at commit — even if nothing was written — so only call it when there are alert
   * updates to apply.
   */
  TransitAlertRepository transitAlertRepository();

  /**
   * Return a transit service that can look up both scheduled and real-time data, as of the last
   * committed timetable snapshot.
   */
  TransitService transitService();

  /**
   * Return a GTFS-RT fuzzy trip matcher backed by {@link #transitService()}.
   */
  GtfsRealtimeFuzzyTripMatcher gtfsRealtimeFuzzyTripMatcher();
}
