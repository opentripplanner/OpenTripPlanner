package org.opentripplanner.routing.services;

import java.util.Collection;
import java.util.Set;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.alertpatch.AlertPatch;

public interface AlertPatchService {
    Collection<AlertPatch> getAllAlertPatches();

    Collection<AlertPatch> getStopPatches(FeedScopedId stop);

    Collection<AlertPatch> getRoutePatches(FeedScopedId route);

    void apply(AlertPatch alertPatch);

    void expire(Set<String> ids);

    void expireAll();

    void expireAllExcept(Set<String> ids);
}
