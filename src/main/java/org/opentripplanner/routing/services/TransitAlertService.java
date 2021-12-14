package org.opentripplanner.routing.services;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.alertpatch.TransitAlert;

import java.util.Collection;

public interface TransitAlertService {

    void setAlerts(Collection<TransitAlert> alerts);

    Collection<TransitAlert> getAllAlerts();

    TransitAlert getAlertById(String id);

    Collection<TransitAlert> getStopAlerts(FeedScopedId stop);

    Collection<TransitAlert> getRouteAlerts(FeedScopedId route);

    Collection<TransitAlert> getTripAlerts(FeedScopedId trip, ServiceDate serviceDate);

    Collection<TransitAlert> getAgencyAlerts(FeedScopedId agency);

    Collection<TransitAlert> getStopAndRouteAlerts(FeedScopedId stop, FeedScopedId route);

    Collection<TransitAlert> getStopAndTripAlerts(FeedScopedId stop, FeedScopedId trip, ServiceDate serviceDate);

    Collection<TransitAlert> getRouteTypeAndAgencyAlerts(int routeType, FeedScopedId agency);

    Collection<TransitAlert> getRouteTypeAlerts(int routeType, String feedId);

    Collection<TransitAlert> getDirectionAndRouteAlerts(int directionId, FeedScopedId route);
}
