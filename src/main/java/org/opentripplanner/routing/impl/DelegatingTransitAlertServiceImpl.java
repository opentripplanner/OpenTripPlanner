package org.opentripplanner.routing.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.alerts.TransitAlertProvider;

public class DelegatingTransitAlertServiceImpl implements TransitAlertService {

  private final ArrayList<TransitAlertService> transitAlertServices = new ArrayList<>();

  public DelegatingTransitAlertServiceImpl(TransitModel transitModel) {
    if (transitModel.getUpdaterManager() != null) {
      transitModel
        .getUpdaterManager()
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
  public Collection<TransitAlert> getTripAlerts(FeedScopedId trip, LocalDate serviceDate) {
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
    LocalDate serviceDate
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
  public Collection<TransitAlert> getDirectionAndRouteAlerts(
    Direction direction,
    FeedScopedId route
  ) {
    return transitAlertServices
      .stream()
      .map(transitAlertService -> transitAlertService.getDirectionAndRouteAlerts(direction, route))
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }
}
