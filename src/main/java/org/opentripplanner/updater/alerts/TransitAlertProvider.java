package org.opentripplanner.updater.alerts;

import org.opentripplanner.routing.services.TransitAlertService;

public interface TransitAlertProvider {
  TransitAlertService getTransitAlertService();
}
