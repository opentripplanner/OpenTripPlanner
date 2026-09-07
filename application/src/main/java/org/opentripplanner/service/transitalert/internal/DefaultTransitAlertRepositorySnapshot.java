package org.opentripplanner.service.transitalert.internal;

import com.google.common.collect.ImmutableListMultimap;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.EntityKey;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.service.transitalert.TransitAlertRepositorySnapshot;
import org.opentripplanner.transit.model.timetable.Direction;

/**
 * Immutable snapshot of the alert repository state, published at commit time. It holds the alerts
 * of all sources merged into a single index, keyed by the transit entities they affect.
 * <p>
 * When an alert is added with more than one transit entity, e.g. a Stop and a Trip, both conditions
 * must be met for the alert to be displayed. This is the case in both the SIRI Nordic Profile, and
 * the GTFS-RT alerts specification.
 */
class DefaultTransitAlertRepositorySnapshot implements TransitAlertRepositorySnapshot {

  private final ImmutableListMultimap<EntityKey, TransitAlert> alertsByKey;
  private final List<TransitAlert> allAlerts;
  private final Map<FeedScopedId, TransitAlert> alertsById;

  /**
   * The state the next mutable repository is created from, see {@link #copyOnWrite()}. The alerts
   * are kept partitioned by feed because that is the granularity the updaters write at: a
   * differential feed must be able to add to and remove from its own alerts without touching the
   * alerts of any other feed.
   */
  private final Map<String, Map<FeedScopedId, TransitAlert>> alertsByFeedId;

  DefaultTransitAlertRepositorySnapshot(
    ImmutableListMultimap<EntityKey, TransitAlert> alertsByKey,
    List<TransitAlert> allAlerts,
    Map<FeedScopedId, TransitAlert> alertsById,
    Map<String, Map<FeedScopedId, TransitAlert>> alertsByFeedId
  ) {
    this.alertsByKey = alertsByKey;
    this.allAlerts = allAlerts;
    this.alertsById = alertsById;
    this.alertsByFeedId = alertsByFeedId;
  }

  /** used by the repository life-cycle */
  DefaultTransitAlertRepository copyOnWrite() {
    return new DefaultTransitAlertRepository(alertsByFeedId);
  }

  @Override
  public Collection<TransitAlert> getAllAlerts() {
    return allAlerts;
  }

  @Override
  @Nullable
  public TransitAlert getAlertById(FeedScopedId id) {
    return alertsById.get(id);
  }

  @Override
  public Collection<TransitAlert> getStopAlerts(
    FeedScopedId stopId,
    Set<StopCondition> stopConditions
  ) {
    return findMatchingAlerts(new EntitySelector.Stop(stopId, stopConditions));
  }

  @Override
  public Set<TransitAlert> getStopLocationsAlerts(List<FeedScopedId> stopLocationIds) {
    return stopLocationIds
      .stream()
      .flatMap(stopLocationId ->
        findMatchingAlerts(new EntitySelector.Stop(stopLocationId)).stream()
      )
      .collect(Collectors.toSet());
  }

  @Override
  public Collection<TransitAlert> getRouteAlerts(FeedScopedId route) {
    return alertsByKey.get(new EntityKey.Route(route));
  }

  @Override
  public Collection<TransitAlert> getTripAlerts(FeedScopedId trip) {
    return findMatchingAlerts(new EntitySelector.Trip(trip));
  }

  @Override
  public Collection<TransitAlert> getTripAlerts(FeedScopedId trip, LocalDate serviceDate) {
    return findMatchingAlerts(new EntitySelector.Trip(trip, serviceDate));
  }

  @Override
  public Collection<TransitAlert> getAgencyAlerts(FeedScopedId agency) {
    return alertsByKey.get(new EntityKey.Agency(agency));
  }

  @Override
  public Collection<TransitAlert> getStopAndRouteAlerts(
    FeedScopedId stop,
    FeedScopedId route,
    Set<StopCondition> stopConditions,
    Direction direction
  ) {
    return findMatchingAlerts(
      new EntitySelector.StopAndRoute(stop, route, stopConditions, List.of(direction))
    );
  }

  @Override
  public Collection<TransitAlert> getStopAndTripAlerts(
    FeedScopedId stop,
    FeedScopedId trip,
    LocalDate serviceDate,
    Set<StopCondition> stopConditions
  ) {
    return findMatchingAlerts(
      new EntitySelector.StopAndTrip(stop, trip, serviceDate, stopConditions)
    );
  }

  @Override
  public Collection<TransitAlert> getRouteTypeAndAgencyAlerts(int routeType, FeedScopedId agency) {
    return alertsByKey.get(new EntityKey.RouteTypeAndAgency(agency, routeType));
  }

  @Override
  public Collection<TransitAlert> getRouteTypeAlerts(int routeType, String feedId) {
    return alertsByKey.get(new EntityKey.RouteType(feedId, routeType));
  }

  @Override
  public Collection<TransitAlert> getDirectionAndRouteAlerts(
    Direction direction,
    FeedScopedId route
  ) {
    return alertsByKey.get(new EntityKey.DirectionAndRoute(route, direction));
  }

  private Collection<TransitAlert> findMatchingAlerts(EntitySelector entitySelector) {
    Set<TransitAlert> result = new HashSet<>();
    for (TransitAlert alert : alertsByKey.get(entitySelector.key())) {
      if (
        alert
          .entities()
          .stream()
          .anyMatch(selector -> selector.matches(entitySelector))
      ) {
        result.add(alert);
      }
    }
    return result;
  }
}
