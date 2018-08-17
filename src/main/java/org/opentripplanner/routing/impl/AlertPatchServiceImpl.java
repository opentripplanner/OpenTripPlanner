package org.opentripplanner.routing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.AlertPatchService;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

public class AlertPatchServiceImpl implements AlertPatchService {

    private Graph graph;

    private Map<String, AlertPatch> alertPatches = new HashMap<String, AlertPatch>();
    private ListMultimap<FeedScopedId, AlertPatch> patchesByRoute = LinkedListMultimap.create();
    private ListMultimap<FeedScopedId, AlertPatch> patchesByStop = LinkedListMultimap.create();

    public AlertPatchServiceImpl(Graph graph) {
        this.graph = graph;
    }

    @Override
    public Collection<AlertPatch> getAllAlertPatches() {
        return alertPatches.values();
    }

    @Override
    public Collection<AlertPatch> getStopPatches(FeedScopedId stop) {
        List<AlertPatch> result = patchesByStop.get(stop);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getRoutePatches(FeedScopedId route) {
        List<AlertPatch> result = patchesByRoute.get(route);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;

    }

    @Override
    public synchronized void apply(AlertPatch alertPatch) {
        if (alertPatches.containsKey(alertPatch.getId())) {
            expire(alertPatches.get(alertPatch.getId()));
        }

        alertPatch.apply(graph);
        alertPatches.put(alertPatch.getId(), alertPatch);

        FeedScopedId stop = alertPatch.getStop();
        if (stop != null) {
            patchesByStop.put(stop, alertPatch);
        }
        FeedScopedId route = alertPatch.getRoute();
        if (route != null) {
            patchesByRoute.put(route, alertPatch);
        }
    }

    @Override
    public void expire(Set<String> purge) {
        for (String patchId : purge) {
            if (alertPatches.containsKey(patchId)) {
                expire(alertPatches.get(patchId));
            }
        }

        alertPatches.keySet().removeAll(purge);
    }

    @Override
    public void expireAll() {
        for (AlertPatch alertPatch : alertPatches.values()) {
            expire(alertPatch);
        }
        alertPatches.clear();
    }

    @Override
    public void expireAllExcept(Set<String> retain) {
        ArrayList<String> toRemove = new ArrayList<String>();

        for (Entry<String, AlertPatch> entry : alertPatches.entrySet()) {
            final String key = entry.getKey();
            if (!retain.contains(key)) {
                toRemove.add(key);
                expire(entry.getValue());
            }
        }
        alertPatches.keySet().removeAll(toRemove);
    }

    private void expire(AlertPatch alertPatch) {
        FeedScopedId stop = alertPatch.getStop();
        if (stop != null) {
            patchesByStop.remove(stop, alertPatch);
        }
        FeedScopedId route = alertPatch.getRoute();
        if (route != null) {
            patchesByRoute.remove(route, alertPatch);
        }

        alertPatch.remove(graph);
    }
}
