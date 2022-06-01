package org.opentripplanner.routing.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.basic.FeedScopedId;

/**
 * When an alert is added with more than one transit entity, e.g. a Stop and a Trip, both conditions
 * must be met for the alert to be displayed. This is the case in both the Norwegian interpretation
 * of SIRI, and the GTFS-RT alerts specification.
 */
public class TransitAlertServiceImpl implements TransitAlertService {

  private final Graph graph;

  private Multimap<EntitySelector, TransitAlert> alerts = HashMultimap.create();

  public TransitAlertServiceImpl(Graph graph) {
    this.graph = graph;
  }

  @Override
  public void setAlerts(Collection<TransitAlert> alerts) {
    Multimap<EntitySelector, TransitAlert> newAlerts = HashMultimap.create();
    for (TransitAlert alert : alerts) {
      for (EntitySelector entity : alert.getEntities()) {
        newAlerts.put(entity, alert);
      }
    }

    this.alerts = newAlerts;
  }

  @Override
  public Collection<TransitAlert> getAllAlerts() {
    return new HashSet<>(alerts.values());
  }

  @Override
  public TransitAlert getAlertById(String id) {
    return alerts
      .values()
      .stream()
      .filter(transitAlert -> transitAlert.getId().equals(id))
      .findAny()
      .orElse(null);
  }

  @Override
  public Collection<TransitAlert> getStopAlerts(FeedScopedId stopId) {
    // TODO StopConditions: Need to take the required StopConditions into account.
    //  These are matched using equals/hashCode, but required StopConditions do not necessarily
    //  match, but may be a subset/superset.
    Set<TransitAlert> result = new HashSet<>(
      alerts.get(new EntitySelector.Stop(stopId, Collections.EMPTY_SET))
    );
    if (result.isEmpty()) {
      // Search for alerts on parent-stop
      if (graph != null && graph.index != null) {
        var quay = graph.index.getStopForId(stopId);
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
  public Collection<TransitAlert> getRouteAlerts(FeedScopedId route) {
    return alerts.get(new EntitySelector.Route(route));
  }

  @Override
  public Collection<TransitAlert> getTripAlerts(FeedScopedId trip, ServiceDate serviceDate) {
    return alerts.get(new EntitySelector.Trip(trip, serviceDate));
  }

  @Override
  public Collection<TransitAlert> getAgencyAlerts(FeedScopedId agency) {
    return alerts.get(new EntitySelector.Agency(agency));
  }

  @Override
  public Collection<TransitAlert> getStopAndRouteAlerts(FeedScopedId stop, FeedScopedId route) {
    // TODO StopConditions: Need to take the required StopConditions into account.
    //  These are matched using equals/hashCode, but required StopConditions do not necessarily
    //  match, but may be a subset/superset.

    return alerts.get(new EntitySelector.StopAndRoute(stop, route));
  }

  @Override
  public Collection<TransitAlert> getStopAndTripAlerts(
    FeedScopedId stop,
    FeedScopedId trip,
    ServiceDate serviceDate
  ) {
    // TODO StopConditions: Need to take the required StopConditions into account.
    //  These are matched using equals/hashCode, but required StopConditions do not necessarily
    //  match, but may be a subset/superset.

    return alerts.get(new EntitySelector.StopAndTrip(stop, trip, serviceDate));
  }

  @Override
  public Collection<TransitAlert> getRouteTypeAndAgencyAlerts(int routeType, FeedScopedId agency) {
    return alerts.get(new EntitySelector.RouteTypeAndAgency(routeType, agency));
  }

  @Override
  public Collection<TransitAlert> getRouteTypeAlerts(int routeType, String feedId) {
    return alerts.get(new EntitySelector.RouteType(routeType, feedId));
  }

  @Override
  public Collection<TransitAlert> getDirectionAndRouteAlerts(int directionId, FeedScopedId route) {
    return alerts.get(new EntitySelector.DirectionAndRoute(directionId, route));
  }
}
