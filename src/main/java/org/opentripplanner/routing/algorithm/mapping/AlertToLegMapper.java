package org.opentripplanner.routing.algorithm.mapping;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * This class is responsible for finding and adding transit alerts to individual transit legs.
 */
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

  /**
   * Find and add alerts to the leg passed in.
   *
   * @param isFirstLeg Whether the leg is a first leg of the itinerary. This affects the matched
   *                   stop condition.
   */
  public void addTransitAlertsToLeg(Leg leg, boolean isFirstLeg) {
    // Alert alerts are only relevant for transit legs
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
    LocalDate serviceDate = leg.getServiceDate();

    if (fromStop instanceof RegularStop stop) {
      Collection<TransitAlert> alerts = getAlertsForStopAndRoute(stop, routeId);
      alerts.addAll(getAlertsForStopAndTrip(stop, tripId, serviceDate));
      alerts.addAll(getAlertsForRelatedStops(stop, transitAlertService::getStopAlerts));
      addTransitAlertsToLeg(leg, departingStopConditions, alerts, legStartTime, legEndTime);
    }
    if (toStop instanceof RegularStop stop) {
      Collection<TransitAlert> alerts = getAlertsForStopAndRoute(stop, routeId);
      alerts.addAll(getAlertsForStopAndTrip(stop, tripId, serviceDate));
      alerts.addAll(getAlertsForRelatedStops(stop, transitAlertService::getStopAlerts));
      addTransitAlertsToLeg(leg, StopCondition.ARRIVING, alerts, legStartTime, legEndTime);
    }

    if (leg.getIntermediateStops() != null) {
      for (StopArrival visit : leg.getIntermediateStops()) {
        if (visit.place.stop instanceof RegularStop stop) {
          Collection<TransitAlert> alerts = getAlertsForStopAndRoute(stop, routeId);
          alerts.addAll(getAlertsForStopAndTrip(stop, tripId, serviceDate));
          alerts.addAll(getAlertsForRelatedStops(stop, transitAlertService::getStopAlerts));

          ZonedDateTime stopArrival = visit.arrival;
          ZonedDateTime stopDeparture = visit.departure;

          addTransitAlertsToLeg(leg, StopCondition.PASSING, alerts, stopArrival, stopDeparture);
        }
      }
    }

    Collection<TransitAlert> alerts;

    // trips - alerts tagged on ServiceDate
    alerts = transitAlertService.getTripAlerts(leg.getTrip().getId(), serviceDate);
    addTransitAlertsToLeg(leg, alerts, legStartTime, legEndTime);

    // trips - alerts tagged on any date
    alerts = transitAlertService.getTripAlerts(leg.getTrip().getId(), null);
    addTransitAlertsToLeg(leg, alerts, legStartTime, legEndTime);

    // route
    alerts = transitAlertService.getRouteAlerts(leg.getRoute().getId());
    addTransitAlertsToLeg(leg, alerts, legStartTime, legEndTime);

    // agency
    alerts = transitAlertService.getAgencyAlerts(leg.getAgency().getId());
    addTransitAlertsToLeg(leg, alerts, legStartTime, legEndTime);

    // Filter alerts when there are multiple timePeriods for each alert
    leg
      .getTransitAlerts()
      .removeIf(alert ->
        !alert.displayDuring(leg.getStartTime().toEpochSecond(), leg.getEndTime().toEpochSecond())
      );
  }

  /**
   * Add alerts for the leg, if they are valid for the duration of the leg, and if the stop
   * condition(s) match
   */
  private static void addTransitAlertsToLeg(
    Leg leg,
    Collection<StopCondition> stopConditions,
    Collection<TransitAlert> alerts,
    ZonedDateTime fromTime,
    ZonedDateTime toTime
  ) {
    if (alerts != null) {
      for (TransitAlert alert : alerts) {
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

  /**
   * Add alerts for the leg, if they are valid for the duration of the leg, without considering the
   * stop condition(s) of the alert
   */
  private static void addTransitAlertsToLeg(
    Leg leg,
    Collection<TransitAlert> alerts,
    ZonedDateTime fromTime,
    ZonedDateTime toTime
  ) {
    addTransitAlertsToLeg(leg, null, alerts, fromTime, toTime);
  }

  private Collection<TransitAlert> getAlertsForStopAndRoute(
    RegularStop stop,
    FeedScopedId routeId
  ) {
    return getAlertsForRelatedStops(
      stop,
      id -> transitAlertService.getStopAndRouteAlerts(id, routeId)
    );
  }

  private Collection<TransitAlert> getAlertsForStopAndTrip(
    RegularStop stop,
    FeedScopedId tripId,
    LocalDate serviceDate
  ) {
    // Finding alerts for ServiceDate
    final Collection<TransitAlert> alerts = getAlertsForRelatedStops(
      stop,
      id -> transitAlertService.getStopAndTripAlerts(id, tripId, serviceDate)
    );

    // Finding alerts for any date
    alerts.addAll(
      getAlertsForRelatedStops(
        stop,
        id -> transitAlertService.getStopAndTripAlerts(id, tripId, null)
      )
    );

    return alerts;
  }

  /**
   * Find alerts, which are for the stop, its parent(s) and siblings, using a provided function for
   * finding alerts for those stops. This can be used to only find eg. alerts that are valid for
   * only a specific route at that stop.
   */
  private Collection<TransitAlert> getAlertsForRelatedStops(
    RegularStop stop,
    Function<FeedScopedId, Collection<TransitAlert>> getAlertsForStop
  ) {
    if (stop == null) {
      return new ArrayList<>();
    }

    Collection<TransitAlert> alertsForStop = getAlertsForStop.apply(stop.getId());
    if (alertsForStop == null) {
      alertsForStop = new HashSet<>();
    }

    if (stop.isPartOfStation()) {
      // Also check parent
      final Station parentStation = stop.getParentStation();
      Collection<TransitAlert> parentStopAlerts = getAlertsForStop.apply(parentStation.getId());
      if (parentStopAlerts != null) {
        alertsForStop.addAll(parentStopAlerts);
      }

      // ...and siblings - platform may have been changed
      for (var siblingStop : parentStation.getChildStops()) {
        if (!stop.getId().equals(siblingStop.getId())) {
          Collection<TransitAlert> siblingAlerts = getAlertsForStop.apply(parentStation.getId());
          if (siblingAlerts != null) {
            alertsForStop.addAll(siblingAlerts);
          }
        }
      }

      // Also check multimodal parent
      MultiModalStation multiModalStation = getMultiModalStation.apply(parentStation);
      if (multiModalStation != null) {
        Collection<TransitAlert> multimodalStopAlerts = getAlertsForStop.apply(
          multiModalStation.getId()
        );
        if (multimodalStopAlerts != null) {
          alertsForStop.addAll(multimodalStopAlerts);
        }
      }
    }
    return alertsForStop;
  }
}
