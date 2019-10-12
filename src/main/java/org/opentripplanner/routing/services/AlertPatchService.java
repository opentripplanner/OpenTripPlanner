package org.opentripplanner.routing.services;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.Collection;
import java.util.Set;

public interface AlertPatchService {
    Collection<AlertPatch> getAllAlertPatches();

    // TODO OTP2 - Not used, will be used by the new Transit API(not included jet)
    AlertPatch getPatchById(String id);

    Collection<AlertPatch> getStopPatches(FeedScopedId stop);

    Collection<AlertPatch> getRoutePatches(FeedScopedId route);

    // TODO OTP2 - Not used, will be used by the new Transit API(not included jet)
    Collection<AlertPatch> getTripPatches(FeedScopedId trip);

    // TODO OTP2 - Not used, will be used by the new Transit API(not included jet)
    Collection<AlertPatch> getAgencyPatches(String agency);

    // TODO OTP2 - Not used, will be used by the new Transit API(not included jet)
    Collection<AlertPatch> getStopAndRoutePatches(FeedScopedId stop, FeedScopedId route);

    // TODO OTP2 - Not used, will be used by the new Transit API(not included jet)
    Collection<AlertPatch> getStopAndTripPatches(FeedScopedId stop, FeedScopedId trip);

    Collection<AlertPatch> getTripPatternPatches(TripPattern tripPattern);

    void apply(AlertPatch alertPatch);

    void expire(Set<String> ids);

    void expireAll();

    void expireAllExcept(Set<String> ids);

    void applyAll(Set<AlertPatch> alertPatches);
}
