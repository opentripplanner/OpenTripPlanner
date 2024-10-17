package org.opentripplanner.updater.alert;

import org.opentripplanner.routing.services.TransitAlertService;

/**
 * Interface for things that maintain their own individual index associating TransitAlerts with the
 * transit entities they affect. In practice, these are always realtime updaters handling GTFS-RT
 * Alerts or Siri SX messages. This interface appears to exist only to allow merging multiple such
 * services together, which appears to be a workaround for not maintaining snapshots of a single
 * instance-wide index.
 *
 * Ideally this will become unnecessary and be removed when updaters are all feeding into a central
 * index. If kept, this should be renamed from TransitAlertProvider to TransitAlertServiceProvider.
 */
public interface TransitAlertProvider {
  TransitAlertService getTransitAlertService();
}
