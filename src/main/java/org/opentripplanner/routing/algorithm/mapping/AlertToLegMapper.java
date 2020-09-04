package org.opentripplanner.routing.algorithm.mapping;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.TransitAlertService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class AlertToLegMapper {

    public static void addAlertPatchesToLeg(Graph graph, Leg leg, boolean isFirstLeg, Locale requestedLocale) {
        Set<StopCondition> departingStopConditions = isFirstLeg
                ? StopCondition.DEPARTURE
                : StopCondition.FIRST_DEPARTURE;

        Date legStartTime = leg.startTime.getTime();
        Date legEndTime = leg.endTime.getTime();
        FeedScopedId fromStopId = leg.from==null ? null : leg.from.stopId;
        FeedScopedId toStopId = leg.to==null ? null : leg.to.stopId;

        if (leg.isTransitLeg()) {
            FeedScopedId routeId = leg.getRoute().getId();
            if (fromStopId != null) {
                Collection<TransitAlert> alerts = getAlertsForStopAndRoute(graph, fromStopId, routeId);
                addAlertPatchesToLeg(leg, departingStopConditions, alerts, requestedLocale, legStartTime, legEndTime);
            }
            if (toStopId != null) {
                Collection<TransitAlert> alerts = getAlertsForStopAndRoute(graph, toStopId, routeId);
                addAlertPatchesToLeg(leg, StopCondition.ARRIVING, alerts, requestedLocale, legStartTime, legEndTime);
            }

            FeedScopedId tripId = leg.getTrip().getId();
            if (fromStopId != null) {
                Collection<TransitAlert> alerts = getAlertsForStopAndTrip(graph, fromStopId, tripId);
                addAlertPatchesToLeg(leg, departingStopConditions, alerts, requestedLocale, legStartTime, legEndTime);
            }
            if (toStopId != null) {
                Collection<TransitAlert> alerts = getAlertsForStopAndTrip(graph, toStopId, tripId);
                addAlertPatchesToLeg(leg, StopCondition.ARRIVING, alerts, requestedLocale, legStartTime, legEndTime);
            }
            if (leg.intermediateStops != null) {
                for (StopArrival visit : leg.intermediateStops) {
                    Place place = visit.place;
                    if (place.stopId != null) {
                        Collection<TransitAlert> alerts = getAlertsForStopAndTrip(graph, place.stopId, tripId);
                        Date stopArrival = visit.arrival.getTime();
                        Date stopDepature = visit.departure.getTime();
                        addAlertPatchesToLeg(leg, StopCondition.PASSING, alerts, requestedLocale, stopArrival, stopDepature);
                    }
                }
            }
        }

        if (leg.intermediateStops != null) {
            for (StopArrival visit : leg.intermediateStops) {
                Place place = visit.place;
                if (place.stopId != null) {
                    Collection<TransitAlert> alerts = getAlertsForStop(graph, place.stopId);
                    Date stopArrival = visit.arrival.getTime();
                    Date stopDepature = visit.departure.getTime();
                    addAlertPatchesToLeg(leg, StopCondition.PASSING, alerts, requestedLocale, stopArrival, stopDepature);
                }
            }
        }

        if (leg.from != null && fromStopId != null) {
            Collection<TransitAlert> alerts = getAlertsForStop(graph, fromStopId);
            addAlertPatchesToLeg(leg, departingStopConditions, alerts, requestedLocale, legStartTime, legEndTime);
        }

        if (leg.to != null && toStopId != null) {
            Collection<TransitAlert> alerts = getAlertsForStop(graph, toStopId);
            addAlertPatchesToLeg(leg, StopCondition.ARRIVING, alerts, requestedLocale, legStartTime, legEndTime);
        }

        if(leg.isTransitLeg()) {
            Collection<TransitAlert> patches;

            // trips
            patches = alertPatchService(graph).getTripAlerts(leg.getTrip().getId());
            addAlertPatchesToLeg(leg, patches, requestedLocale, legStartTime, legEndTime);

            // route
            patches = alertPatchService(graph).getRouteAlerts(leg.getRoute().getId());
            addAlertPatchesToLeg(leg, patches, requestedLocale, legStartTime, legEndTime);

            // agency
            patches = alertPatchService(graph).getAgencyAlerts(leg.getAgency().getId());
            addAlertPatchesToLeg(leg, patches, requestedLocale, legStartTime, legEndTime);
        }

        // Filter alerts when there are multiple timePeriods for each alert
        leg.transitAlerts.removeIf(alertPatch ->  !alertPatch.displayDuring(leg.startTime.getTimeInMillis()/1000, leg.endTime.getTimeInMillis()/1000));
    }

    private static TransitAlertService alertPatchService(Graph g) {
        return g.getTransitAlertService();
    }

    private static Collection<TransitAlert> getAlertsForStopAndRoute(Graph graph, FeedScopedId stopId, FeedScopedId routeId) {
        return getAlertsForStopAndRoute(graph, stopId, routeId, true);
    }


    private static Collection<TransitAlert> getAlertsForStopAndRoute(Graph graph, FeedScopedId stopId, FeedScopedId routeId, boolean checkParentStop) {

        Stop stop = graph.index.getStopForId(stopId);
        if (stop == null) {
            return new ArrayList<>();
        }
        Collection<TransitAlert> alertsForStopAndRoute = graph.getTransitAlertService().getStopAndRouteAlerts(stopId, routeId);
        if (checkParentStop) {
            if (alertsForStopAndRoute == null) {
                alertsForStopAndRoute = new HashSet<>();
            }
            if (stop.isPartOfStation()) {
                //Also check parent
                Collection<TransitAlert> alerts = graph.getTransitAlertService().getStopAndRouteAlerts(stop.getParentStation().getId(), routeId);
                if (alerts != null) {
                    alertsForStopAndRoute.addAll(alerts);
                }

                // ...and siblings - platform may have been changed
                for (Stop siblingStop : stop.getParentStation().getChildStops()) {
                    if (!stop.getId().equals(siblingStop.getId())) {
                        Collection<TransitAlert> siblingAlerts = graph.getTransitAlertService().getStopAndRouteAlerts(stop.getParentStation().getId(), routeId);
                        if (siblingAlerts != null) {
                            alertsForStopAndRoute.addAll(siblingAlerts);
                        }
                    }
                }
            }

            // TODO SIRI: Add support for fetching alerts attached to MultiModal-stops
            //            if (stop.getMultiModalStation() != null) {
            //                //Also check multimodal parent
            //
            //                FeedScopedId multimodalStopId = new FeedScopedId(stopId.getAgencyId(), stop.getMultiModalStation());
            //                Collection<AlertPatch> multimodalStopAlerts = graph.index.getAlertsForStopAndRoute(multimodalStopId, routeId);
            //                if (multimodalStopAlerts != null) {
            //                    alertsForStopAndRoute.addAll(multimodalStopAlerts);
            //                }
            //            }
        }
        return alertsForStopAndRoute;
    }

    private static Collection<TransitAlert> getAlertsForStopAndTrip(Graph graph, FeedScopedId stopId, FeedScopedId tripId) {
        return getAlertsForStopAndTrip(graph, stopId, tripId, true);
    }

    private static Collection<TransitAlert> getAlertsForStopAndTrip(Graph graph, FeedScopedId stopId, FeedScopedId tripId, boolean checkParentStop) {

        Stop stop = graph.index.getStopForId(stopId);
        if (stop == null) {
            return new ArrayList<>();
        }

        Collection<TransitAlert> alertsForStopAndTrip = graph.getTransitAlertService().getStopAndTripAlerts(stopId, tripId);
        if (checkParentStop) {
            if (alertsForStopAndTrip == null) {
                alertsForStopAndTrip = new HashSet<>();
            }
            if  (stop.isPartOfStation()) {
                // Also check parent
                Collection<TransitAlert> alerts = graph.getTransitAlertService().getStopAndTripAlerts(stop.getParentStation().getId(), tripId);
                if (alerts != null) {
                    alertsForStopAndTrip.addAll(alerts);
                }

                // ...and siblings - platform may have been changed
                for (Stop siblingStop : stop.getParentStation().getChildStops()) {
                    if (!stop.getId().equals(siblingStop.getId())) {
                        Collection<TransitAlert> siblingAlerts = graph.getTransitAlertService().getStopAndTripAlerts(stop.getParentStation().getId(), tripId);
                        if (siblingAlerts != null) {
                            alertsForStopAndTrip.addAll(siblingAlerts);
                        }
                    }
                }

            }
            // TODO SIRI: Add support for fetching alerts attached to MultiModal-stops
            //            if (stop.getMultiModalStation() != null) {
            //                //Also check multimodal parent
            //                FeedScopedId multimodalStopId = new FeedScopedId(stopId.getAgencyId(), stop.getMultiModalStation());
            //                Collection<AlertPatch> multimodalStopAlerts = graph.index.getAlertsForStopAndTrip(multimodalStopId, tripId);
            //                if (multimodalStopAlerts != null) {
            //                    alertsForStopAndTrip.addAll(multimodalStopAlerts);
            //                }
            //            }
        }
        return alertsForStopAndTrip;
    }

    private static Collection<TransitAlert> getAlertsForStop(Graph graph, FeedScopedId stopId) {
        return getAlertsForStop(graph, stopId, true);
    }

    private static Collection<TransitAlert> getAlertsForStop(Graph graph, FeedScopedId stopId, boolean checkParentStop) {
        Stop stop = graph.index.getStopForId(stopId);
        if (stop == null) {
            return new ArrayList<>();
        }

        Collection<TransitAlert> alertsForStop  = graph.getTransitAlertService().getStopAlerts(stopId);
        if (checkParentStop) {
            if (alertsForStop == null) {
                alertsForStop = new HashSet<>();
            }

            if  (stop.isPartOfStation()) {
                // Also check parent
                Collection<TransitAlert> parentStopAlerts = graph.getTransitAlertService().getStopAlerts(stop.getParentStation().getId());
                if (parentStopAlerts != null) {
                    alertsForStop.addAll(parentStopAlerts);
                }

                // ...and siblings - platform may have been changed
                for (Stop siblingStop : stop.getParentStation().getChildStops()) {
                    if (!stop.getId().equals(siblingStop.getId())) {
                        Collection<TransitAlert> siblingAlerts = graph.getTransitAlertService().getStopAlerts(stop.getParentStation().getId());
                        if (siblingAlerts != null) {
                            alertsForStop.addAll(siblingAlerts);
                        }
                    }
                }
            }

            // TODO SIRI: Add support for fetching alerts attached to MultiModal-stops
            //            if (stop.getMultiModalStation() != null) {
            //                //Also check multimodal parent
            //                FeedScopedId multimodalStopId = new FeedScopedId(stopId.getAgencyId(), stop.getMultiModalStation());
            //                Collection<AlertPatch> multimodalStopAlerts = graph.index.getAlertsForStopId(multimodalStopId);
            //                if (multimodalStopAlerts != null) {
            //                    alertsForStop.addAll(multimodalStopAlerts);
            //                }
            //            }

        }
        return alertsForStop;
    }


    private static void addAlertPatchesToLeg(Leg leg, Collection<StopCondition> stopConditions, Collection<TransitAlert> alertPatches, Locale requestedLocale, Date fromTime, Date toTime) {
        if (alertPatches != null) {
            for (TransitAlert alert : alertPatches) {
                if (alert.displayDuring(fromTime.getTime() / 1000, toTime.getTime() / 1000)) {
                    if (!alert.getStopConditions().isEmpty() &&  // Skip if stopConditions are not set for alert
                            stopConditions != null && !stopConditions.isEmpty()) { // ...or specific stopConditions are not requested
                        for (StopCondition stopCondition : stopConditions) {
                            if (alert.getStopConditions().contains(stopCondition)) {
                                leg.addAlert(alert);
                                break; //Only add alert once
                            }
                        }
                    } else {
                        leg.addAlert(alert);
                    }
                }
            }
        }
    }

    private static void addAlertPatchesToLeg(Leg leg, Collection<TransitAlert> alertPatches, Locale requestedLocale, Date fromTime, Date toTime) {
        addAlertPatchesToLeg(leg, null, alertPatches, requestedLocale, fromTime, toTime);
    }
}
