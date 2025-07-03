package org.opentripplanner.routing.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.alert.TransitAlertProvider;

/**
 * This class is used to combine alerts from multiple {@link TransitAlertService}s. Each
 * {@link TransitAlertProvider} has its own service, and all need to be queried in order to fetch
 * all alerts.
 *
 * Concretely: every realtime updater receiving GTFS Alerts or SIRI Situation Exchange (SX)
 * messages currently maintains its own private index of alerts separately from all other updaters.
 * To make the set of all alerts from all updaters available in a single operation and associate it
 * with the graph as a whole, the various indexes are merged in such a way as to have the same
 * index as each individual index.
 */
public class DelegatingTransitAlertServiceImpl implements TransitAlertService {

  private final ArrayList<TransitAlertService> transitAlertServices = new ArrayList<>();

  /**
   * Constructor which scans over all existing GraphUpdaters associated with a TimetableRepository
   * instance and retains references to all their TransitAlertService instances.
   * This implies that these instances are expected to remain in use indefinitely (not be replaced
   * with new instances or taken out of service over time).
   */
  public DelegatingTransitAlertServiceImpl(TimetableRepository timetableRepository) {
    if (timetableRepository.getUpdaterManager() != null) {
      timetableRepository
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
