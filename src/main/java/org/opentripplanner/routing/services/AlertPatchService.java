package org.opentripplanner.routing.services;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.alertpatch.AlertPatch;

import java.util.Collection;

public interface AlertPatchService {
    Collection<AlertPatch> getAllAlertPatches();

    // TODO OTP2 - Not used, will be used by the new Transit API(not included jet)
    AlertPatch getPatchById(String id);

    Collection<AlertPatch> getStopPatches(FeedScopedId stop);

    Collection<AlertPatch> getRoutePatches(FeedScopedId route);

    Collection<AlertPatch> getTripPatches(FeedScopedId trip);

    Collection<AlertPatch> getAgencyPatches(FeedScopedId agency);

    Collection<AlertPatch> getStopAndRoutePatches(FeedScopedId stop, FeedScopedId route);

    Collection<AlertPatch> getStopAndTripPatches(FeedScopedId stop, FeedScopedId trip);

    // TODO OTP2 - Not used, will be used by the new Transit API(not included jet)
    Collection<AlertPatch> getTripPatternPatches(TripPattern tripPattern);

    void apply(AlertPatch alertPatch);

    void expire(Collection<String> ids);

    void expireAll();

    void expireAllExcept(Collection<String> ids);

    void applyAll(Collection<AlertPatch> alertPatches);
}
