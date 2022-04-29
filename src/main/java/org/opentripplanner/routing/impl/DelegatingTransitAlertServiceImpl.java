package org.opentripplanner.routing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.updater.alerts.TransitAlertProvider;

public class DelegatingTransitAlertServiceImpl implements TransitAlertService {

  private final ArrayList<TransitAlertService> transitAlertServices = new ArrayList<>();

  public DelegatingTransitAlertServiceImpl(Graph graph) {
    if (graph.updaterManager != null) {
      graph.updaterManager
        .getUpdaterList()
        .stream()
        .filter(TransitAlertProvider.class::isInstance)
        .map(TransitAlertProvider.class::cast)
        .map(TransitAlertProvider::getTransitAlertService)
        .forEach(transitAlertServices::add);
    }
  }

  @Override
  public void setAlerts(Collection<TransitAlert> alerts) {
    throw new UnsupportedOperationException("Not supported");
  }

  @Override
  public Collection<TransitAlert> getAllAlerts() {
    return transitAlertServices
      .stream()
      .map(TransitAlertService::getAllAlerts)
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  @Override
  public TransitAlert getAlertById(String id) {
    return transitAlertServices
      .stream()
      .map(transitAlertService -> transitAlertService.getAlertById(id))
      .filter(Objects::nonNull)
      .findAny()
      .orElse(null);
  }

  @Override
  public Collection<TransitAlert> getStopAlerts(FeedScopedId stop) {
    return transitAlertServices
      .stream()
      .map(transitAlertService -> transitAlertService.getStopAlerts(stop))
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<TransitAlert> getRouteAlerts(FeedScopedId route) {
    return transitAlertServices
      .stream()
      .map(transitAlertService -> transitAlertService.getRouteAlerts(route))
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<TransitAlert> getTripAlerts(FeedScopedId trip, ServiceDate serviceDate) {
    return transitAlertServices
      .stream()
      .map(transitAlertService -> transitAlertService.getTripAlerts(trip, serviceDate))
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<TransitAlert> getAgencyAlerts(FeedScopedId agency) {
    return transitAlertServices
      .stream()
      .map(transitAlertService -> transitAlertService.getAgencyAlerts(agency))
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<TransitAlert> getStopAndRouteAlerts(FeedScopedId stop, FeedScopedId route) {
    return transitAlertServices
      .stream()
      .map(transitAlertService -> transitAlertService.getStopAndRouteAlerts(stop, route))
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<TransitAlert> getStopAndTripAlerts(
    FeedScopedId stop,
    FeedScopedId trip,
    ServiceDate serviceDate
  ) {
    return transitAlertServices
      .stream()
      .map(transitAlertService -> transitAlertService.getStopAndTripAlerts(stop, trip, serviceDate))
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<TransitAlert> getRouteTypeAndAgencyAlerts(int routeType, FeedScopedId agency) {
    return transitAlertServices
      .stream()
      .map(transitAlertService -> transitAlertService.getRouteTypeAndAgencyAlerts(routeType, agency)
      )
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<TransitAlert> getRouteTypeAlerts(int routeType, String feedId) {
    return transitAlertServices
      .stream()
      .map(transitAlertService -> transitAlertService.getRouteTypeAlerts(routeType, feedId))
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<TransitAlert> getDirectionAndRouteAlerts(int directionId, FeedScopedId route) {
    return transitAlertServices
      .stream()
      .map(transitAlertService -> transitAlertService.getDirectionAndRouteAlerts(directionId, route)
      )
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }
}
