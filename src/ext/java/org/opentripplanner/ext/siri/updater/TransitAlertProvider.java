package org.opentripplanner.ext.siri.updater;

import org.opentripplanner.routing.services.TransitAlertService;

public interface TransitAlertProvider {
  TransitAlertService getTransitAlertService();
}
