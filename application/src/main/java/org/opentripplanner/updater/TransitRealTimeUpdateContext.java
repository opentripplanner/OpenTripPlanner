package org.opentripplanner.updater;

import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.trip.gtfs.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.trip.siri.EntityResolver;

/**
 * Give access to the transit data in the context of a real-time update task in the transit write
 * domain. The services exposed must be used only from the transit domain's writer thread.
 */
public interface TransitRealTimeUpdateContext {
  /**
   * Return the mutable realtime-timetable repository (write buffer) for this update task. Callers
   * must only use this from the single writer thread.
   */
  TimetableRepository timetableRepository();

  /**
   * Return the mutable realtime-vehicle repository for this update task. Callers must only use
   * this from the single writer thread. Accessing it causes the current transaction to publish a
   * new vehicle snapshot at commit — even if nothing was written — so only call it when there are
   * vehicle updates to apply.
   */
  RealtimeVehicleRepository realtimeVehicleRepository();

  /**
   * Return a transit service that can look up both scheduled and real-time data.
   * The transit service has access to all real-time updates applied so far,
   * including those not yet committed in a published snapshot.
   */
  TransitService transitService();

  /**
   * Return a transit service that combines scheduled data with the last <em>committed</em>
   * timetable snapshot. Unlike {@link #transitService()}, it does not see real-time updates
   * applied in the current, uncommitted transaction, and resolving it does not cause a new
   * timetable snapshot to be published at commit. Use this in updaters that only read
   * transit data in order to attach their own real-time entities to it, such as the
   * vehicle-position updater.
   */
  TransitService committedTransitService();

  /**
   * Return a GTFS-RT fuzzy trip matcher that can look up both scheduled and real-time data.
   * The GTFS-RT fuzzy trip matcher has access to all real-time updates applied so far,
   * including those not yet committed in a published snapshot.
   */
  GtfsRealtimeFuzzyTripMatcher gtfsRealtimeFuzzyTripMatcher();

  /**
   * Return an entity resolver that can look up both scheduled and real-time data.
   * The entity resolver has access to all real-time updates applied so far,
   * including those not yet committed in a published snapshot.
   */
  EntityResolver entityResolver(String feedId);
}
