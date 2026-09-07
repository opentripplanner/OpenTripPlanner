package org.opentripplanner.service.transitalert.internal;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.service.transitalert.TransitAlertRepositorySnapshot;
import org.opentripplanner.service.transitalert.TransitAlertService;
import org.opentripplanner.transit.model.timetable.Direction;

/**
 * A request-scoped view over a {@link TransitAlertRepositorySnapshot}.
 * <p>
 * A new instance should be created for each request, with a snapshot resolved from the request's
 * transaction scope, so that the whole request sees one consistent view of the realtime data.
 */
public class DefaultTransitAlertService implements TransitAlertService {

  private final TransitAlertRepositorySnapshot snapshot;

  public DefaultTransitAlertService(TransitAlertRepositorySnapshot snapshot) {
    this.snapshot = snapshot;
  }

  @Override
  public Collection<TransitAlert> getAllAlerts() {
    return snapshot.getAllAlerts();
  }

  @Override
  @Nullable
  public TransitAlert getAlertById(FeedScopedId id) {
    return snapshot.getAlertById(id);
  }

  @Override
  public Collection<TransitAlert> getStopAlerts(
    FeedScopedId stop,
    Set<StopCondition> stopConditions
  ) {
    return snapshot.getStopAlerts(stop, stopConditions);
  }

  @Override
  public Set<TransitAlert> getStopLocationsAlerts(List<FeedScopedId> stopLocationIds) {
    return snapshot.getStopLocationsAlerts(stopLocationIds);
  }

  @Override
  public Collection<TransitAlert> getRouteAlerts(FeedScopedId route) {
    return snapshot.getRouteAlerts(route);
  }

  @Override
  public Collection<TransitAlert> getTripAlerts(FeedScopedId trip) {
    return snapshot.getTripAlerts(trip);
  }

  @Override
  public Collection<TransitAlert> getTripAlerts(FeedScopedId trip, LocalDate serviceDate) {
    return snapshot.getTripAlerts(trip, serviceDate);
  }

  @Override
  public Collection<TransitAlert> getAgencyAlerts(FeedScopedId agency) {
    return snapshot.getAgencyAlerts(agency);
  }

  @Override
  public Collection<TransitAlert> getStopAndRouteAlerts(
    FeedScopedId stop,
    FeedScopedId route,
    Set<StopCondition> stopConditions,
    Direction direction
  ) {
    return snapshot.getStopAndRouteAlerts(stop, route, stopConditions, direction);
  }

  @Override
  public Collection<TransitAlert> getStopAndTripAlerts(
    FeedScopedId stop,
    FeedScopedId trip,
    LocalDate serviceDate,
    Set<StopCondition> stopConditions
  ) {
    return snapshot.getStopAndTripAlerts(stop, trip, serviceDate, stopConditions);
  }

  @Override
  public Collection<TransitAlert> getRouteTypeAndAgencyAlerts(int routeType, FeedScopedId agency) {
    return snapshot.getRouteTypeAndAgencyAlerts(routeType, agency);
  }

  @Override
  public Collection<TransitAlert> getRouteTypeAlerts(int routeType, String feedId) {
    return snapshot.getRouteTypeAlerts(routeType, feedId);
  }

  @Override
  public Collection<TransitAlert> getDirectionAndRouteAlerts(
    Direction direction,
    FeedScopedId route
  ) {
    return snapshot.getDirectionAndRouteAlerts(direction, route);
  }
}
