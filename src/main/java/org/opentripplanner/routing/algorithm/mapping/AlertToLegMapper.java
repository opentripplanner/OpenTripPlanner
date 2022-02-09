package org.opentripplanner.routing.algorithm.mapping;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Leg;
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

    public static void addTransitAlertPatchesToLeg(Graph graph, Leg leg, boolean isFirstLeg) {

        // Alert patches are only relevant for transit legs
        if (!leg.isTransitLeg()) { return; }

        Set<StopCondition> departingStopConditions = isFirstLeg
                ? StopCondition.DEPARTURE
                : StopCondition.FIRST_DEPARTURE;

        Date legStartTime = leg.getStartTime().getTime();
        Date legEndTime = leg.getEndTime().getTime();
        StopLocation fromStop = leg.getFrom() == null ? null : leg.getFrom().stop;
        StopLocation toStop = leg.getTo() == null ? null : leg.getTo().stop;

        FeedScopedId routeId = leg.getRoute().getId();
        FeedScopedId tripId = leg.getTrip().getId();
        if (fromStop instanceof Stop) {
            Collection<TransitAlert> alerts = getAlertsForStopAndRoute(graph, (Stop) fromStop, routeId);
            alerts.addAll(getAlertsForStopAndTrip(graph, (Stop) fromStop, tripId,
                    leg.getServiceDate()
            ));
            alerts.addAll(getAlertsForStop(graph, (Stop) fromStop));
            addTransitAlertPatchesToLeg(leg, departingStopConditions, alerts, legStartTime, legEndTime);
        }
        if (toStop instanceof Stop) {
            Collection<TransitAlert> alerts = getAlertsForStopAndRoute(graph, (Stop) toStop, routeId);
            alerts.addAll(getAlertsForStopAndTrip(graph, (Stop) toStop, tripId,
                    leg.getServiceDate()
            ));
            alerts.addAll(getAlertsForStop(graph, (Stop) toStop));
            addTransitAlertPatchesToLeg(leg, StopCondition.ARRIVING, alerts, legStartTime, legEndTime);
        }

        if (leg.getIntermediateStops() != null) {
            for (StopArrival visit : leg.getIntermediateStops()) {
                if (visit.place.stop instanceof Stop) {
                    Stop stop = (Stop) visit.place.stop;
                    Collection<TransitAlert> alerts = getAlertsForStopAndRoute(graph, stop, routeId);
                    alerts.addAll(getAlertsForStopAndTrip(graph, stop, tripId, leg.getServiceDate()));
                    alerts.addAll(getAlertsForStop(graph, stop));

                    Date stopArrival = visit.arrival.getTime();
                    Date stopDepature = visit.departure.getTime();

                    addTransitAlertPatchesToLeg(leg, StopCondition.PASSING, alerts, stopArrival, stopDepature);
                }
            }
        }

        Collection<TransitAlert> patches;

        // trips - alerts tagged on ServiceDate
        patches = alertPatchService(graph)
                .getTripAlerts(leg.getTrip().getId(), leg.getServiceDate());
        addTransitAlertPatchesToLeg(leg, patches, legStartTime, legEndTime);

        // trips - alerts tagged on any date
        patches = alertPatchService(graph).getTripAlerts(leg.getTrip().getId(), null);
        addTransitAlertPatchesToLeg(leg, patches, legStartTime, legEndTime);

        // route
        patches = alertPatchService(graph).getRouteAlerts(leg.getRoute().getId());
        addTransitAlertPatchesToLeg(leg, patches, legStartTime, legEndTime);

        // agency
        patches = alertPatchService(graph).getAgencyAlerts(leg.getAgency().getId());
        addTransitAlertPatchesToLeg(leg, patches, legStartTime, legEndTime);

        // Filter alerts when there are multiple timePeriods for each alert
        leg.getTransitAlerts().removeIf(alertPatch ->  
                !alertPatch.displayDuring(
                        leg.getStartTime().getTimeInMillis()/1000,
                        leg.getEndTime().getTimeInMillis()/1000
                )
        );
    }

    private static TransitAlertService alertPatchService(Graph g) {
        return g.getTransitAlertService();
    }

    private static Collection<TransitAlert> getAlertsForStopAndRoute(Graph graph, Stop stop, FeedScopedId routeId) {
        return getAlertsForStopAndRoute(graph, stop, routeId, true);
    }


    private static Collection<TransitAlert> getAlertsForStopAndRoute(Graph graph, Stop stop, FeedScopedId routeId, boolean checkParentStop) {

        if (stop == null) {
            return new ArrayList<>();
        }
        Collection<TransitAlert> alertsForStopAndRoute = graph.getTransitAlertService().getStopAndRouteAlerts(stop.getId(), routeId);
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
                for (var siblingStop : stop.getParentStation().getChildStops()) {
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

    private static Collection<TransitAlert> getAlertsForStopAndTrip(Graph graph, Stop stop, FeedScopedId tripId, ServiceDate serviceDate) {

        // Finding alerts for ServiceDate
        final Collection<TransitAlert> alerts = getAlertsForStopAndTrip(
            graph,
            stop,
            tripId,
            true,
            serviceDate
        );

        // Finding alerts for any date
        alerts.addAll(getAlertsForStopAndTrip(
            graph,
            stop,
            tripId,
            true,
            null
        ));

        return alerts;
    }

    private static Collection<TransitAlert> getAlertsForStopAndTrip(Graph graph, Stop stop, FeedScopedId tripId, boolean checkParentStop, ServiceDate serviceDate) {

        if (stop == null) {
            return new ArrayList<>();
        }

        Collection<TransitAlert> alertsForStopAndTrip = graph.getTransitAlertService().getStopAndTripAlerts(stop.getId(), tripId, serviceDate);
        if (checkParentStop) {
            if (alertsForStopAndTrip == null) {
                alertsForStopAndTrip = new HashSet<>();
            }
            if  (stop.isPartOfStation()) {
                // Also check parent
                Collection<TransitAlert> alerts = graph.getTransitAlertService().getStopAndTripAlerts(stop.getParentStation().getId(), tripId, serviceDate);
                if (alerts != null) {
                    alertsForStopAndTrip.addAll(alerts);
                }

                // ...and siblings - platform may have been changed
                for (var siblingStop : stop.getParentStation().getChildStops()) {
                    if (!stop.getId().equals(siblingStop.getId())) {
                        Collection<TransitAlert> siblingAlerts = graph.getTransitAlertService().getStopAndTripAlerts(stop.getParentStation().getId(), tripId, serviceDate);
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

    private static Collection<TransitAlert> getAlertsForStop(Graph graph, Stop stopId) {
        return getAlertsForStop(graph, stopId, true);
    }

    private static Collection<TransitAlert> getAlertsForStop(Graph graph, Stop stop, boolean checkParentStop) {
        if (stop == null) {
            return new ArrayList<>();
        }

        Collection<TransitAlert> alertsForStop  = graph.getTransitAlertService().getStopAlerts(stop.getId());
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
                for (var siblingStop : stop.getParentStation().getChildStops()) {
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


    private static void addTransitAlertPatchesToLeg(Leg leg, Collection<StopCondition> stopConditions, Collection<TransitAlert> alertPatches, Date fromTime, Date toTime) {
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

    private static void addTransitAlertPatchesToLeg(Leg leg, Collection<TransitAlert> alertPatches, Date fromTime, Date toTime) {
        addTransitAlertPatchesToLeg(leg, null, alertPatches, fromTime, toTime);
    }
}
