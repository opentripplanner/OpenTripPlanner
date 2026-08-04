package org.opentripplanner.updater;

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
   * Return a transit service that can look up both scheduled and real-time data.
   * The transit service has access to all real-time updates applied so far,
   * including those not yet committed in a published snapshot.
   */
  TransitService transitService();

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
