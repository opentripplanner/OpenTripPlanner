package org.opentripplanner.updater;

import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.updater.trip.gtfs.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.trip.siri.EntityResolver;

/**
 * Give access to the transit data in the context of a real-time update task in the transit write
 * domain. The services exposed must be used only from the transit domain's writer thread.
 */
public interface TransitRealTimeUpdateContext {
  /**
   * Return the mutable realtime-timetable repository (write buffer) for this update task. Callers
   * must only use this from the single writer thread. Entity lookups (trips, routes, patterns)
   * on this repository already fall back to scheduled data, so this is also the way to resolve
   * entities that see all in-progress real-time additions not yet committed to a published
   * snapshot.
   */
  TimetableRepository timetableRepository();

  /**
   * Return the scheduled (non-realtime) transit repository. Use this only for lookups that have
   * no realtime concept (agencies, operators, stops, time zone); for entity lookups that should
   * see realtime data, use {@link #timetableRepository()} instead.
   */
  TransitRepository transitRepository();

  /**
   * Return the mutable realtime-vehicle repository for this update task. Callers must only use
   * this from the single writer thread. Accessing it causes the current transaction to publish a
   * new vehicle snapshot at commit — even if nothing was written — so only call it when there are
   * vehicle updates to apply.
   */
  RealtimeVehicleRepository realtimeVehicleRepository();

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
