package org.opentripplanner.updater.alert;

import org.opentripplanner.routing.services.TransitAlertService;

public interface TransitAlertProvider {
  TransitAlertService getTransitAlertService();
}
