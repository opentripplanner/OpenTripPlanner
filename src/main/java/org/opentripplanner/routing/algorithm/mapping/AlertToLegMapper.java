package org.opentripplanner.routing.algorithm.mapping;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.AlertPatchService;

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

        if (leg.routeId != null) {
            if (fromStopId != null) {
                Collection<AlertPatch> alerts = getAlertsForStopAndRoute(graph, fromStopId, leg.routeId);
                addAlertPatchesToLeg(leg, departingStopConditions, alerts, requestedLocale, legStartTime, legEndTime);
            }
            if (toStopId != null) {
                Collection<AlertPatch> alerts = getAlertsForStopAndRoute(graph, toStopId, leg.routeId);
                addAlertPatchesToLeg(leg, StopCondition.ARRIVING, alerts, requestedLocale, legStartTime, legEndTime);
            }
        }

        if (leg.tripId != null) {
            if (fromStopId != null) {
                Collection<AlertPatch> alerts = getAlertsForStopAndTrip(graph, fromStopId, leg.tripId);
                addAlertPatchesToLeg(leg, departingStopConditions, alerts, requestedLocale, legStartTime, legEndTime);
            }
            if (toStopId != null) {
                Collection<AlertPatch> alerts = getAlertsForStopAndTrip(graph, toStopId, leg.tripId);
                addAlertPatchesToLeg(leg, StopCondition.ARRIVING, alerts, requestedLocale, legStartTime, legEndTime);
            }
            if (leg.intermediateStops != null) {
                for (StopArrival visit : leg.intermediateStops) {
                    Place place = visit.place;
                    if (place.stopId != null) {
                        Collection<AlertPatch> alerts = getAlertsForStopAndTrip(graph, place.stopId, leg.tripId);
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
                    Collection<AlertPatch> alerts = getAlertsForStop(graph, place.stopId);
                    Date stopArrival = visit.arrival.getTime();
                    Date stopDepature = visit.departure.getTime();
                    addAlertPatchesToLeg(leg, StopCondition.PASSING, alerts, requestedLocale, stopArrival, stopDepature);
                }
            }
        }

        if (leg.from != null && fromStopId != null) {
            Collection<AlertPatch> alerts = getAlertsForStop(graph, fromStopId);
            addAlertPatchesToLeg(leg, departingStopConditions, alerts, requestedLocale, legStartTime, legEndTime);
        }

        if (leg.to != null && toStopId != null) {
            Collection<AlertPatch> alerts = getAlertsForStop(graph, toStopId);
            addAlertPatchesToLeg(leg, StopCondition.ARRIVING, alerts, requestedLocale, legStartTime, legEndTime);
        }

        if (leg.tripId != null) {
            Collection<AlertPatch> patches = alertPatchService(graph).getTripPatches(leg.tripId);
            addAlertPatchesToLeg(leg, patches, requestedLocale, legStartTime, legEndTime);
        }
        if (leg.routeId != null) {
            Collection<AlertPatch> patches = alertPatchService(graph).getRoutePatches(leg.routeId);
            addAlertPatchesToLeg(leg, patches,
                    requestedLocale, legStartTime, legEndTime);
        }

        if (leg.agencyId != null) {
            Collection<AlertPatch> patches = alertPatchService(graph).getAgencyPatches(leg.agencyId);
            addAlertPatchesToLeg(leg, patches, requestedLocale, legStartTime, legEndTime);
        }

        // Filter alerts when there are multiple timePeriods for each alert
        leg.alertPatches.removeIf(alertPatch ->  !alertPatch.displayDuring(leg.startTime.getTimeInMillis()/1000, leg.endTime.getTimeInMillis()/1000));
    }

    private static AlertPatchService alertPatchService(Graph g) {
        return g.getSiriAlertPatchService();
    }

    private static Collection<AlertPatch> getAlertsForStopAndRoute(Graph graph, FeedScopedId stopId, FeedScopedId routeId) {
        return getAlertsForStopAndRoute(graph, stopId, routeId, true);
    }


    private static Collection<AlertPatch> getAlertsForStopAndRoute(Graph graph, FeedScopedId stopId, FeedScopedId routeId, boolean checkParentStop) {

        Stop stop = graph.index.getStopForId(stopId);
        if (stop == null) {
            return new ArrayList<>();
        }
        Collection<AlertPatch> alertsForStopAndRoute = graph.getSiriAlertPatchService().getStopAndRoutePatches(stopId, routeId);
        if (checkParentStop) {
            if (alertsForStopAndRoute == null) {
                alertsForStopAndRoute = new HashSet<>();
            }
            if (stop.isPartOfStation()) {
                //Also check parent
                Collection<AlertPatch> alerts = graph.getSiriAlertPatchService().getStopAndRoutePatches(stop.getParentStation().getId(), routeId);
                if (alerts != null) {
                    alertsForStopAndRoute.addAll(alerts);
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

    private static Collection<AlertPatch> getAlertsForStopAndTrip(Graph graph, FeedScopedId stopId, FeedScopedId tripId) {
        return getAlertsForStopAndTrip(graph, stopId, tripId, true);
    }

    private static Collection<AlertPatch> getAlertsForStopAndTrip(Graph graph, FeedScopedId stopId, FeedScopedId tripId, boolean checkParentStop) {

        Stop stop = graph.index.getStopForId(stopId);
        if (stop == null) {
            return new ArrayList<>();
        }

        Collection<AlertPatch> alertsForStopAndTrip = graph.getSiriAlertPatchService().getStopAndTripPatches(stopId, tripId);
        if (checkParentStop) {
            if (alertsForStopAndTrip == null) {
                alertsForStopAndTrip = new HashSet<>();
            }
            if  (stop.isPartOfStation()) {
                // Also check parent
                Collection<AlertPatch> alerts = graph.getSiriAlertPatchService().getStopAndTripPatches(stop.getParentStation().getId(), tripId);
                if (alerts != null) {
                    alertsForStopAndTrip.addAll(alerts);
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

    private static Collection<AlertPatch> getAlertsForStop(Graph graph, FeedScopedId stopId) {
        return getAlertsForStop(graph, stopId, true);
    }

    private static Collection<AlertPatch> getAlertsForStop(Graph graph, FeedScopedId stopId, boolean checkParentStop) {
        Stop stop = graph.index.getStopForId(stopId);
        if (stop == null) {
            return new ArrayList<>();
        }

        Collection<AlertPatch> alertsForStop  = graph.getSiriAlertPatchService().getStopPatches(stopId);
        if (checkParentStop) {
            if (alertsForStop == null) {
                alertsForStop = new HashSet<>();
            }

            if  (stop.isPartOfStation()) {
                // Also check parent
                Collection<AlertPatch> parentStopAlerts = graph.getSiriAlertPatchService().getStopPatches(stop.getParentStation().getId());
                if (parentStopAlerts != null) {
                    alertsForStop.addAll(parentStopAlerts);
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


    private static void addAlertPatchesToLeg(Leg leg, Collection<StopCondition> stopConditions, Collection<AlertPatch> alertPatches, Locale requestedLocale, Date fromTime, Date toTime) {
        if (alertPatches != null) {
            for (AlertPatch alert : alertPatches) {
                if (alert.getAlert().effectiveStartDate.before(toTime) &&
                        (alert.getAlert().effectiveEndDate == null || alert.getAlert().effectiveEndDate.after(fromTime))) {

                    if (!alert.getStopConditions().isEmpty() &&  // Skip if stopConditions are not set for alert
                            stopConditions != null && !stopConditions.isEmpty()) { // ...or specific stopConditions are not requested
                        for (StopCondition stopCondition : stopConditions) {
                            if (alert.getStopConditions().contains(stopCondition)) {
                                leg.addAlertPatch(alert);
                                break; //Only add alert once
                            }
                        }
                    } else {
                        leg.addAlertPatch(alert);
                    }
                }
            }
        }
    }

    private static void addAlertPatchesToLeg(Leg leg, Collection<AlertPatch> alertPatches, Locale requestedLocale, Date fromTime, Date toTime) {
        addAlertPatchesToLeg(leg, null, alertPatches, requestedLocale, fromTime, toTime);
    }
}
