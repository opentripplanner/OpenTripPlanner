package org.opentripplanner.routing.impl;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.timetable.Direction;

/**
 * This class is used to combine alerts from multiple {@link TransitAlertService}s. Each
 * {@link org.opentripplanner.updater.alert.TransitAlertProvider} has its own service, and all need
 * to be queried in order to fetch all alerts.
 *
 * Concretely: every realtime updater receiving GTFS Alerts or SIRI Situation Exchange (SX)
 * messages currently maintains its own private index of alerts separately from all other updaters.
 * To make the set of all alerts from all updaters available in a single operation and associate it
 * with the application as a whole, the various indexes are merged in such a way as to have the same
 * index as each individual index.
 *
 * <p>Instances are registered with {@link #addDelegate(TransitAlertService)} when the updaters are
 * configured. This class is an application-wide singleton, so registration and reads may happen
 * concurrently; a {@link CopyOnWriteArrayList} is used to keep reads lock-free.
 */
public class DelegatingTransitAlertServiceImpl implements TransitAlertService {

  private final List<TransitAlertService> transitAlertServices = new CopyOnWriteArrayList<>();

  /**
   * Register a delegate service, typically owned by a single realtime updater.
   */
  public void addDelegate(TransitAlertService transitAlertService) {
    transitAlertServices.add(transitAlertService);
  }

  @Override
  public void setAlerts(Collection<TransitAlert> alerts) {
    throw new UnsupportedOperationException(
      "This delegating TransitAlertService is not intended to hold any TransitAlerts of its own."
    );
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
  public TransitAlert getAlertById(FeedScopedId id) {
    return transitAlertServices
      .stream()
      .map(transitAlertService -> transitAlertService.getAlertById(id))
      .filter(Objects::nonNull)
      .findAny()
      .orElse(null);
  }

  @Override
  public Collection<TransitAlert> getStopAlerts(
    FeedScopedId stop,
    Set<StopCondition> stopConditions
  ) {
    return transitAlertServices
      .stream()
      .map(transitAlertService -> transitAlertService.getStopAlerts(stop, stopConditions))
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  @Override
  public Set<TransitAlert> getStopLocationsAlerts(List<FeedScopedId> stopLocationIds) {
    return transitAlertServices
      .stream()
      .map(transitAlertService -> transitAlertService.getStopLocationsAlerts(stopLocationIds))
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());
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
  public Collection<TransitAlert> getTripAlerts(FeedScopedId trip) {
    return transitAlertServices
      .stream()
      .map(transitAlertService -> transitAlertService.getTripAlerts(trip))
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
  public Collection<TransitAlert> getStopAndRouteAlerts(
    FeedScopedId stop,
    FeedScopedId route,
    Set<StopCondition> stopConditions
  ) {
    return transitAlertServices
      .stream()
      .map(transitAlertService ->
        transitAlertService.getStopAndRouteAlerts(stop, route, stopConditions)
      )
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<TransitAlert> getStopAndTripAlerts(
    FeedScopedId stop,
    FeedScopedId trip,
    LocalDate serviceDate,
    Set<StopCondition> stopConditions
  ) {
    return transitAlertServices
      .stream()
      .map(transitAlertService ->
        transitAlertService.getStopAndTripAlerts(stop, trip, serviceDate, stopConditions)
      )
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<TransitAlert> getRouteTypeAndAgencyAlerts(int routeType, FeedScopedId agency) {
    return transitAlertServices
      .stream()
      .map(transitAlertService ->
        transitAlertService.getRouteTypeAndAgencyAlerts(routeType, agency)
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
