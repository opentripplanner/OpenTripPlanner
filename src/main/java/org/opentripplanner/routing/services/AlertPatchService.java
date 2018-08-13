package org.opentripplanner.routing.services;

import java.util.Collection;
import java.util.Set;

import org.opentripplanner.model.FeedId;
import org.opentripplanner.routing.alertpatch.AlertPatch;

public interface AlertPatchService {
    Collection<AlertPatch> getAllAlertPatches();

    Collection<AlertPatch> getStopPatches(FeedId stop);

    Collection<AlertPatch> getRoutePatches(FeedId route);

    void apply(AlertPatch alertPatch);

    void expire(Set<String> ids);

    void expireAll();

    void expireAllExcept(Set<String> ids);
}
