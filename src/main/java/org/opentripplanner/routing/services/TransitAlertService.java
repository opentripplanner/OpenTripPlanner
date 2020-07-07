package org.opentripplanner.routing.services;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.alertpatch.TransitAlert;

import java.util.Collection;

public interface TransitAlertService {

    void setAlerts(Collection<TransitAlert> alerts);

    Collection<TransitAlert> getAllAlerts();

    TransitAlert getAlertById(String id);

    Collection<TransitAlert> getStopAlerts(FeedScopedId stop);

    Collection<TransitAlert> getRouteAlerts(FeedScopedId route);

    Collection<TransitAlert> getTripAlerts(FeedScopedId trip);

    Collection<TransitAlert> getAgencyAlerts(FeedScopedId agency);

    Collection<TransitAlert> getStopAndRouteAlerts(FeedScopedId stop, FeedScopedId route);

    Collection<TransitAlert> getStopAndTripAlerts(FeedScopedId stop, FeedScopedId trip);

    Collection<TransitAlert> getTripPatternAlerts(FeedScopedId tripPattern);
}
