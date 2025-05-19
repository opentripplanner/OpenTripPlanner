package org.opentripplanner.routing.algorithm.mapping;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.model.plan.leg.StopArrival;
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
   * Takes the (immutable) leg and returns a copy of it with the alerts attached.
   *
   * @param isFirstLeg Whether the leg is a first leg of the itinerary. This affects the matched
   *                   stop condition.
   */
  public TransitLeg decorateWithAlerts(TransitLeg leg, boolean isFirstLeg) {
    ZonedDateTime legStartTime = leg.startTime();
    ZonedDateTime legEndTime = leg.endTime();
    StopLocation fromStop = leg.from() == null ? null : leg.from().stop;
    StopLocation toStop = leg.to() == null ? null : leg.to().stop;

    FeedScopedId routeId = leg.route().getId();
    FeedScopedId tripId = leg.trip().getId();
    LocalDate serviceDate = leg.serviceDate();

    var totalAlerts = new HashSet<TransitAlert>();

    if (fromStop instanceof RegularStop stop) {
      Set<StopCondition> stopConditions = isFirstLeg
        ? StopCondition.FIRST_DEPARTURE
        : StopCondition.DEPARTURE;

      Collection<TransitAlert> alerts = getAlertsForStopAndRoute(stop, routeId, stopConditions);
      alerts.addAll(getAlertsForStopAndTrip(stop, tripId, serviceDate, stopConditions));
      alerts.addAll(
        getAlertsForRelatedStops(stop, id -> transitAlertService.getStopAlerts(id, stopConditions))
      );
      totalAlerts.addAll(filterAlertsByTime(alerts, legStartTime, legEndTime));
    }
    if (toStop instanceof RegularStop stop) {
      Set<StopCondition> stopConditions = StopCondition.ARRIVING;
      Collection<TransitAlert> alerts = getAlertsForStopAndRoute(stop, routeId, stopConditions);
      alerts.addAll(getAlertsForStopAndTrip(stop, tripId, serviceDate, stopConditions));
      alerts.addAll(
        getAlertsForRelatedStops(stop, id -> transitAlertService.getStopAlerts(id, stopConditions))
      );
      totalAlerts.addAll(filterAlertsByTime(alerts, legStartTime, legEndTime));
    }

    if (leg.listIntermediateStops() != null) {
      Set<StopCondition> stopConditions = StopCondition.PASSING;
      for (StopArrival visit : leg.listIntermediateStops()) {
        if (visit.place.stop instanceof RegularStop stop) {
          Collection<TransitAlert> alerts = getAlertsForStopAndRoute(stop, routeId, stopConditions);
          alerts.addAll(getAlertsForStopAndTrip(stop, tripId, serviceDate, stopConditions));
          alerts.addAll(
            getAlertsForRelatedStops(stop, id ->
              transitAlertService.getStopAlerts(id, stopConditions)
            )
          );

          ZonedDateTime stopArrival = visit.arrival.scheduledTime();
          ZonedDateTime stopDeparture = visit.departure.scheduledTime();

          totalAlerts.addAll(filterAlertsByTime(alerts, stopArrival, stopDeparture));
        }
      }
    }

    Collection<TransitAlert> alerts;

    // trips
    alerts = transitAlertService.getTripAlerts(leg.trip().getId(), serviceDate);
    totalAlerts.addAll(filterAlertsByTime(alerts, legStartTime, legEndTime));

    // route
    alerts = transitAlertService.getRouteAlerts(leg.route().getId());
    totalAlerts.addAll(filterAlertsByTime(alerts, legStartTime, legEndTime));

    // agency
    alerts = transitAlertService.getAgencyAlerts(leg.agency().getId());
    totalAlerts.addAll(filterAlertsByTime(alerts, legStartTime, legEndTime));

    // Filter alerts when there are multiple timePeriods for each alert
    totalAlerts.removeIf(alert ->
      !alert.displayDuring(leg.startTime().toEpochSecond(), leg.endTime().toEpochSecond())
    );

    return leg.decorateWithAlerts(Set.copyOf(totalAlerts));
  }

  /**
   * Filter alerts if they are valid for the duration of the leg.
   */
  private static List<TransitAlert> filterAlertsByTime(
    Collection<TransitAlert> alerts,
    ZonedDateTime fromTime,
    ZonedDateTime toTime
  ) {
    return alerts
      .stream()
      .filter(alert -> alert.displayDuring(fromTime.toEpochSecond(), toTime.toEpochSecond()))
      .toList();
  }

  private Collection<TransitAlert> getAlertsForStopAndRoute(
    RegularStop stop,
    FeedScopedId routeId,
    Set<StopCondition> stopConditions
  ) {
    return getAlertsForRelatedStops(stop, id ->
      transitAlertService.getStopAndRouteAlerts(id, routeId, stopConditions)
    );
  }

  private Collection<TransitAlert> getAlertsForStopAndTrip(
    RegularStop stop,
    FeedScopedId tripId,
    LocalDate serviceDate,
    Set<StopCondition> stopConditions
  ) {
    return getAlertsForRelatedStops(stop, id ->
      transitAlertService.getStopAndTripAlerts(id, tripId, serviceDate, stopConditions)
    );
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
