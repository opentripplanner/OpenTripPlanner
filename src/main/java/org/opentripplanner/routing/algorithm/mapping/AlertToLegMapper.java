package org.opentripplanner.routing.algorithm.mapping;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.model.site.StopLocation;

public class AlertToLegMapper {

  private final TransitAlertService transitAlertService;

  private final Function<Station, MultiModalStation> getMultiModalStation;

  public AlertToLegMapper(
    TransitAlertService transitAlertService,
    Function<Station, MultiModalStation> getMultiModalStation
  ) {
    this.transitAlertService = transitAlertService;
    this.getMultiModalStation = getMultiModalStation;
  }

  public void addTransitAlertPatchesToLeg(Leg leg, boolean isFirstLeg) {
    // Alert patches are only relevant for transit legs
    if (!leg.isTransitLeg()) {
      return;
    }

    Set<StopCondition> departingStopConditions = isFirstLeg
      ? StopCondition.DEPARTURE
      : StopCondition.FIRST_DEPARTURE;

    ZonedDateTime legStartTime = leg.getStartTime();
    ZonedDateTime legEndTime = leg.getEndTime();
    StopLocation fromStop = leg.getFrom() == null ? null : leg.getFrom().stop;
    StopLocation toStop = leg.getTo() == null ? null : leg.getTo().stop;

    FeedScopedId routeId = leg.getRoute().getId();
    FeedScopedId tripId = leg.getTrip().getId();
    if (fromStop instanceof Stop stop) {
      Collection<TransitAlert> alerts = getAlertsForStopAndRoute(stop, routeId);
      alerts.addAll(getAlertsForStopAndTrip(stop, tripId, leg.getServiceDate()));
      alerts.addAll(getAlertsForStop(stop));
      addTransitAlertPatchesToLeg(leg, departingStopConditions, alerts, legStartTime, legEndTime);
    }
    if (toStop instanceof Stop stop) {
      Collection<TransitAlert> alerts = getAlertsForStopAndRoute(stop, routeId);
      alerts.addAll(getAlertsForStopAndTrip(stop, tripId, leg.getServiceDate()));
      alerts.addAll(getAlertsForStop(stop));
      addTransitAlertPatchesToLeg(leg, StopCondition.ARRIVING, alerts, legStartTime, legEndTime);
    }

    if (leg.getIntermediateStops() != null) {
      for (StopArrival visit : leg.getIntermediateStops()) {
        if (visit.place.stop instanceof Stop stop) {
          Collection<TransitAlert> alerts = getAlertsForStopAndRoute(stop, routeId);
          alerts.addAll(getAlertsForStopAndTrip(stop, tripId, leg.getServiceDate()));
          alerts.addAll(getAlertsForStop(stop));

          ZonedDateTime stopArrival = visit.arrival;
          ZonedDateTime stopDepature = visit.departure;

          addTransitAlertPatchesToLeg(
            leg,
            StopCondition.PASSING,
            alerts,
            stopArrival,
            stopDepature
          );
        }
      }
    }

    Collection<TransitAlert> patches;

    // trips - alerts tagged on ServiceDate
    patches = transitAlertService.getTripAlerts(leg.getTrip().getId(), leg.getServiceDate());
    addTransitAlertPatchesToLeg(leg, patches, legStartTime, legEndTime);

    // trips - alerts tagged on any date
    patches = transitAlertService.getTripAlerts(leg.getTrip().getId(), null);
    addTransitAlertPatchesToLeg(leg, patches, legStartTime, legEndTime);

    // route
    patches = transitAlertService.getRouteAlerts(leg.getRoute().getId());
    addTransitAlertPatchesToLeg(leg, patches, legStartTime, legEndTime);

    // agency
    patches = transitAlertService.getAgencyAlerts(leg.getAgency().getId());
    addTransitAlertPatchesToLeg(leg, patches, legStartTime, legEndTime);

    // Filter alerts when there are multiple timePeriods for each alert
    leg
      .getTransitAlerts()
      .removeIf(alertPatch ->
        !alertPatch.displayDuring(
          leg.getStartTime().toEpochSecond(),
          leg.getEndTime().toEpochSecond()
        )
      );
  }

  private static void addTransitAlertPatchesToLeg(
    Leg leg,
    Collection<StopCondition> stopConditions,
    Collection<TransitAlert> alertPatches,
    ZonedDateTime fromTime,
    ZonedDateTime toTime
  ) {
    if (alertPatches != null) {
      for (TransitAlert alert : alertPatches) {
        if (alert.displayDuring(fromTime.toEpochSecond(), toTime.toEpochSecond())) {
          if (
            !alert.getStopConditions().isEmpty() && // Skip if stopConditions are not set for alert
            stopConditions != null &&
            !stopConditions.isEmpty()
          ) { // ...or specific stopConditions are not requested
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

  private static void addTransitAlertPatchesToLeg(
    Leg leg,
    Collection<TransitAlert> alertPatches,
    ZonedDateTime fromTime,
    ZonedDateTime toTime
  ) {
    addTransitAlertPatchesToLeg(leg, null, alertPatches, fromTime, toTime);
  }

  private Collection<TransitAlert> getAlertsForStopAndRoute(Stop stop, FeedScopedId routeId) {
    return getAlertsForStopAndRoute(stop, routeId, true);
  }

  private Collection<TransitAlert> getAlertsForStopAndRoute(
    Stop stop,
    FeedScopedId routeId,
    boolean checkParentStop
  ) {
    if (stop == null) {
      return new ArrayList<>();
    }
    Collection<TransitAlert> alertsForStopAndRoute = transitAlertService.getStopAndRouteAlerts(
      stop.getId(),
      routeId
    );
    if (checkParentStop) {
      if (alertsForStopAndRoute == null) {
        alertsForStopAndRoute = new HashSet<>();
      }

      if (stop.isPartOfStation()) {
        //Also check parent
        final Station parentStation = stop.getParentStation();
        Collection<TransitAlert> alerts = transitAlertService.getStopAndRouteAlerts(
          parentStation.getId(),
          routeId
        );
        if (alerts != null) {
          alertsForStopAndRoute.addAll(alerts);
        }

        // ...and siblings - platform may have been changed
        for (var siblingStop : parentStation.getChildStops()) {
          if (!stop.getId().equals(siblingStop.getId())) {
            Collection<TransitAlert> siblingAlerts = transitAlertService.getStopAndRouteAlerts(
              parentStation.getId(),
              routeId
            );
            if (siblingAlerts != null) {
              alertsForStopAndRoute.addAll(siblingAlerts);
            }
          }
        }

        // Also check multimodal parent
        MultiModalStation multiModalStation = getMultiModalStation.apply(parentStation);
        if (multiModalStation != null) {
          Collection<TransitAlert> multimodalStopAlerts = transitAlertService.getStopAndRouteAlerts(
            multiModalStation.getId(),
            routeId
          );
          if (multimodalStopAlerts != null) {
            alertsForStopAndRoute.addAll(multimodalStopAlerts);
          }
        }
      }
    }
    return alertsForStopAndRoute;
  }

  private Collection<TransitAlert> getAlertsForStopAndTrip(
    Stop stop,
    FeedScopedId tripId,
    ServiceDate serviceDate
  ) {
    // Finding alerts for ServiceDate
    final Collection<TransitAlert> alerts = getAlertsForStopAndTrip(
      stop,
      tripId,
      true,
      serviceDate
    );

    // Finding alerts for any date
    alerts.addAll(getAlertsForStopAndTrip(stop, tripId, true, null));

    return alerts;
  }

  private Collection<TransitAlert> getAlertsForStopAndTrip(
    Stop stop,
    FeedScopedId tripId,
    boolean checkParentStop,
    ServiceDate serviceDate
  ) {
    if (stop == null) {
      return new ArrayList<>();
    }

    Collection<TransitAlert> alertsForStopAndTrip = transitAlertService.getStopAndTripAlerts(
      stop.getId(),
      tripId,
      serviceDate
    );
    if (checkParentStop) {
      if (alertsForStopAndTrip == null) {
        alertsForStopAndTrip = new HashSet<>();
      }
      if (stop.isPartOfStation()) {
        // Also check parent
        final Station parentStation = stop.getParentStation();
        Collection<TransitAlert> alerts = transitAlertService.getStopAndTripAlerts(
          parentStation.getId(),
          tripId,
          serviceDate
        );
        if (alerts != null) {
          alertsForStopAndTrip.addAll(alerts);
        }

        // ...and siblings - platform may have been changed
        for (var siblingStop : parentStation.getChildStops()) {
          if (!stop.getId().equals(siblingStop.getId())) {
            Collection<TransitAlert> siblingAlerts = transitAlertService.getStopAndTripAlerts(
              parentStation.getId(),
              tripId,
              serviceDate
            );
            if (siblingAlerts != null) {
              alertsForStopAndTrip.addAll(siblingAlerts);
            }
          }
        }

        // Also check multimodal parent
        MultiModalStation multiModalStation = getMultiModalStation.apply(parentStation);
        if (multiModalStation != null) {
          Collection<TransitAlert> multimodalStopAlerts = transitAlertService.getStopAndTripAlerts(
            multiModalStation.getId(),
            tripId,
            serviceDate
          );
          if (multimodalStopAlerts != null) {
            alertsForStopAndTrip.addAll(multimodalStopAlerts);
          }
        }
      }
    }
    return alertsForStopAndTrip;
  }

  private Collection<TransitAlert> getAlertsForStop(Stop stopId) {
    return getAlertsForStop(stopId, true);
  }

  private Collection<TransitAlert> getAlertsForStop(Stop stop, boolean checkParentStop) {
    if (stop == null) {
      return new ArrayList<>();
    }

    Collection<TransitAlert> alertsForStop = transitAlertService.getStopAlerts(stop.getId());
    if (checkParentStop) {
      if (alertsForStop == null) {
        alertsForStop = new HashSet<>();
      }

      if (stop.isPartOfStation()) {
        // Also check parent
        final Station parentStation = stop.getParentStation();
        Collection<TransitAlert> parentStopAlerts = transitAlertService.getStopAlerts(
          parentStation.getId()
        );
        if (parentStopAlerts != null) {
          alertsForStop.addAll(parentStopAlerts);
        }

        // ...and siblings - platform may have been changed
        for (var siblingStop : parentStation.getChildStops()) {
          if (!stop.getId().equals(siblingStop.getId())) {
            Collection<TransitAlert> siblingAlerts = transitAlertService.getStopAlerts(
              parentStation.getId()
            );
            if (siblingAlerts != null) {
              alertsForStop.addAll(siblingAlerts);
            }
          }
        }

        // Also check multimodal parent
        MultiModalStation multiModalStation = getMultiModalStation.apply(parentStation);
        if (multiModalStation != null) {
          Collection<TransitAlert> multimodalStopAlerts = transitAlertService.getStopAlerts(
            multiModalStation.getId()
          );
          if (multimodalStopAlerts != null) {
            alertsForStop.addAll(multimodalStopAlerts);
          }
        }
      }
    }
    return alertsForStop;
  }
}
