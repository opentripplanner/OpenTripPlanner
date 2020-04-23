package org.opentripplanner.routing.impl;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.AlertPatchService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * When an alert is added with more than one transit entity, e.g. a Stop and a Trip, both conditions must be met for
 * the alert to be displayed. This is the case in both the Norwegian interpretation of SIRI, and the GTFS-RT alerts
 * specification.
 *
 * TODO OTP2 - Simplify this large set of Map fields down into one Map with wrapper objects in the keys or values
 *           - that do more complex matching and filtering. We could store alerts keyed on the single more specific
 *           - entity (e.g. trip rather than stop or route) and then include the full filter logic in AlertPatch.
 */
public class AlertPatchServiceImpl implements AlertPatchService {

    private Graph graph;

    private Map<String, AlertPatch> alertPatches = new ConcurrentHashMap<>();
    private Map<FeedScopedId, Set<AlertPatch>> patchesByRoute = new ConcurrentHashMap<>();
    private Map<FeedScopedId, Set<AlertPatch>> patchesByStop = new ConcurrentHashMap<>();
    private Map<StopAndRouteOrTripKey, Set<AlertPatch>> patchesByStopAndRoute = new ConcurrentHashMap<>();
    private Map<StopAndRouteOrTripKey, Set<AlertPatch>> patchesByStopAndTrip = new ConcurrentHashMap<>();
    private Map<FeedScopedId, Set<AlertPatch>> patchesByTrip = new ConcurrentHashMap<>();
    private Map<FeedScopedId, Set<AlertPatch>> patchesByAgency = new ConcurrentHashMap<>();
    private Map<String, Set<AlertPatch>> patchesByTripPattern = new ConcurrentHashMap<>();

    public AlertPatchServiceImpl(Graph graph) {
        this.graph = graph;
    }

    @Override
    public Collection<AlertPatch> getAllAlertPatches() {
        return alertPatches.values();
    }

    @Override
    public AlertPatch getPatchById(String id) {
        return alertPatches.get(id);
    }

    @Override
    public Collection<AlertPatch> getStopPatches(FeedScopedId stopId) {
        Set<AlertPatch> result = patchesByStop.get(stopId);
        if (result == null || result.isEmpty()) {
            result = new HashSet<>();
            // Search for alerts on parent-stop
            if (graph != null && graph.index != null) {
                Stop quay = graph.index.getStopForId(stopId);
                if (quay != null) {
                    
                    // TODO - SIRI: Add alerts from parent- and multimodal-stops
                    /*
                    if ( quay.isPartOfStation()) {
                        // Add alerts for parent-station
                        result.addAll(patchesByStop.getOrDefault(quay.getParentStationFeedScopedId(), Collections.emptySet()));
                    }
                    if (quay.getMultiModalStation() != null) {
                        // Add alerts for multimodal-station
                        result.addAll(patchesByStop.getOrDefault(new FeedScopedId(stop.getAgencyId(), quay.getMultiModalStation()), Collections.emptySet()));
                    }
                    */
                }
            }
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getRoutePatches(FeedScopedId route) {
        Set<AlertPatch> result = new HashSet<>();
        if (patchesByRoute.containsKey(route)) {
            result.addAll(patchesByRoute.get(route));
        }
        return result;

    }

    @Override
    public Collection<AlertPatch> getTripPatches(FeedScopedId trip) {
        Set<AlertPatch> result = new HashSet<>();
        if (patchesByTrip.containsKey(trip)) {
            result.addAll(patchesByTrip.get(trip));
        }
        return result;
    }


    @Override
    public Collection<AlertPatch> getAgencyPatches(FeedScopedId agency) {
        Set<AlertPatch> result = new HashSet<>();
        if (patchesByAgency.containsKey(agency)) {
            result.addAll(patchesByAgency.get(agency));
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getStopAndRoutePatches(FeedScopedId stop, FeedScopedId route) {
        Set<AlertPatch> result = new HashSet<>();
        StopAndRouteOrTripKey key = new StopAndRouteOrTripKey(stop, route);
        if (patchesByStopAndRoute.containsKey(key)) {
            result.addAll(patchesByStopAndRoute.get(key));
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getStopAndTripPatches(FeedScopedId stop, FeedScopedId trip) {
        Set<AlertPatch> result = new HashSet<>();
        StopAndRouteOrTripKey key = new StopAndRouteOrTripKey(stop, trip);
        if (patchesByStopAndTrip.containsKey(key)) {
            result.addAll(patchesByStopAndTrip.get(key));
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getTripPatternPatches(TripPattern pattern) {
        Set<AlertPatch> result = new HashSet<>();
        if (patchesByTripPattern.containsKey(pattern.getFeedId())) {
            result.addAll(patchesByTripPattern.get(pattern.getFeedId()));
        }
        return result;
    }

    @Override
    public synchronized void applyAll(Collection<AlertPatch> alertPatches) {
        for (AlertPatch alertPatch : alertPatches) {
            apply(alertPatch);
        }
    }

    @Override
    public synchronized void apply(AlertPatch alertPatch) {
        if (alertPatches.containsKey(alertPatch.getId())) {
            expire(alertPatches.get(alertPatch.getId()));
        }

        alertPatch.apply(graph);
        alertPatches.put(alertPatch.getId(), alertPatch);

        FeedScopedId stop = alertPatch.getStop();
        FeedScopedId route = alertPatch.getRoute();
        FeedScopedId trip = alertPatch.getTrip();

        if (stop != null && trip != null) {
            StopAndRouteOrTripKey key = new StopAndRouteOrTripKey(stop, trip);
            Set<AlertPatch> set = patchesByStopAndTrip.getOrDefault(key, new HashSet());
            set.add(alertPatch);
            patchesByStopAndTrip.put(key, set);
        } else if (stop != null && route != null) {
            StopAndRouteOrTripKey key = new StopAndRouteOrTripKey(stop, route);
            Set<AlertPatch> set = patchesByStopAndRoute.getOrDefault(key, new HashSet());
            set.add(alertPatch);
            patchesByStopAndRoute.put(key, set);
        } else {
            if (stop != null) {
                Set<AlertPatch> set = patchesByStop.getOrDefault(stop, new HashSet());
                set.add(alertPatch);
                patchesByStop.put(stop, set);
            }

            if (route != null) {
                Set<AlertPatch> set = patchesByRoute.getOrDefault(route, new HashSet());
                set.add(alertPatch);
                patchesByRoute.put(route, set);
            }

            if (trip != null) {
                Set<AlertPatch> set = patchesByTrip.getOrDefault(trip, new HashSet());
                set.add(alertPatch);
                patchesByTrip.put(trip, set);
            }
        }

        FeedScopedId agency = alertPatch.getAgency();
        if (agency != null) {
            Set<AlertPatch> set = patchesByAgency.getOrDefault(agency, new HashSet());
            set.add(alertPatch);
            patchesByAgency.put(agency, set);
        }
    }

    @Override
    public void expire(Collection<String> purge) {
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
    public void expireAllExcept(Collection<String> retain) {
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
        FeedScopedId route = alertPatch.getRoute();
        FeedScopedId trip = alertPatch.getTrip();
        FeedScopedId agency = alertPatch.getAgency();

        if (stop != null) {
            removeAlertPatch(patchesByStop.get(stop), alertPatch);
        }

        if (route != null) {
            removeAlertPatch(patchesByRoute.get(route), alertPatch);
        }

        if (trip != null) {
            removeAlertPatch(patchesByTrip.get(trip), alertPatch);
        }

        if (stop != null && route != null) {
            removeAlertPatch(patchesByStopAndRoute.get(new StopAndRouteOrTripKey(stop, route)), alertPatch);
        }

        if (stop != null && trip != null) {
            removeAlertPatch(patchesByStopAndTrip.get(new StopAndRouteOrTripKey(stop, trip)), alertPatch);
        }

        if (agency != null) {
            removeAlertPatch(patchesByAgency.get(agency), alertPatch);
        }

        alertPatch.remove(graph);
    }

    private void removeAlertPatch(Set<AlertPatch> alertPatches, AlertPatch alertPatch) {

        if (alertPatches != null) {
            alertPatches.remove(alertPatch);
        }
    }

    private class StopAndRouteOrTripKey {
        private final FeedScopedId stop;
        private final FeedScopedId routeOrTrip;
        private transient int hash = 0;

        public StopAndRouteOrTripKey(FeedScopedId stop, FeedScopedId routeOrTrip) {
            this.stop = stop;
            this.routeOrTrip = routeOrTrip;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            StopAndRouteOrTripKey that = (StopAndRouteOrTripKey) o;

            if (!stop.equals(that.stop)) {
                return false;
            }
            return routeOrTrip.equals(that.routeOrTrip);
        }

        @Override
        public int hashCode() {
            if (hash == 0) {
                int result = stop.hashCode();
                hash = 31 * result + routeOrTrip.hashCode();
            }
            return hash;
        }
    }
}
