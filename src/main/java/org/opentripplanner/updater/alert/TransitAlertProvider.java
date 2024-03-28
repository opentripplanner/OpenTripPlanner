package org.opentripplanner.updater.alert;

import org.opentripplanner.routing.services.TransitAlertService;

/**
 * Interface for things that maintain their own individual index associating TransitAlerts with the
 * transit entities they affect. In practice, these are always realtime updaters handling GTFS-RT
 * Alerts or Siri SX messages. This interface appears to exist only to allow merging multiple such
 * services together, which appears to be a workaround for not maintaining snapshots of a single
 * instance-wide index.
 */
public interface TransitAlertProvider {
  TransitAlertService getTransitAlertService();
}
