package org.opentripplanner.updater.trip;

import java.util.List;
import org.opentripplanner.transit.model.network.Route;

record RealtimeTripInput(String id, Route route, List<StopCall> stops) {}
