package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.services.SPTService;

public interface SPTServiceFactory {

	SPTService instantiate();

}
