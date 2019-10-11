package org.opentripplanner.routing.services;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.Collection;
import java.util.Set;

public interface AlertPatchService {
    Collection<AlertPatch> getAllAlertPatches();

    AlertPatch getPatchById(String id);

    Collection<AlertPatch> getStopPatches(FeedScopedId stop);

    Collection<AlertPatch> getRoutePatches(FeedScopedId route);

    Collection<AlertPatch> getTripPatches(FeedScopedId trip);

    Collection<AlertPatch> getAgencyPatches(String agency);

    Collection<AlertPatch> getStopAndRoutePatches(FeedScopedId stop, FeedScopedId route);

    Collection<AlertPatch> getStopAndTripPatches(FeedScopedId stop, FeedScopedId trip);

    Collection<AlertPatch> getTripPatternPatches(TripPattern tripPattern);

    void apply(AlertPatch alertPatch);

    void expire(Set<String> ids);

    void expireAll();

    void expireAllExcept(Set<String> ids);

    void applyAll(Set<AlertPatch> alertPatches);
}
