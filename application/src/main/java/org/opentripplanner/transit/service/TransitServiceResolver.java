package org.opentripplanner.transit.service;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.SiteResolver;
import org.opentripplanner.transit.StopResolver;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * A small adapter for resolving site entities by ID from a {@link TransitService}.
 */
public class TransitServiceResolver implements SiteResolver, StopResolver {

  private final TransitService transitService;

  public TransitServiceResolver(TransitService transitService) {
    this.transitService = transitService;
  }

  @Override
  public RegularStop getStop(FeedScopedId id) {
    return Objects.requireNonNull(transitService.getRegularStop(id));
  }

  @Override
  public StopLocation getStopLocation(FeedScopedId id) {
    return Objects.requireNonNull(transitService.getStopLocation(id));
  }

  @Override
  public Entrance getEntrance(FeedScopedId id) {
    return transitService.getEntrance(id);
  }

  @Override
  public List<FeedScopedId> findStopOrChildIds(FeedScopedId id) {
    return transitService.findStopOrChildIds(id);
  }

  @Override
  @Nullable
  public WgsCoordinate findCoordinate(GenericLocation location) {
    if (location.wgsCoordinate() != null) {
      return location.wgsCoordinate();
    }
    var id = location.stopId();
    if (id == null) {
      return null;
    }
    var stop = transitService.getStopLocation(id);
    if (stop != null) {
      return stop.getCoordinate();
    }
    var group = transitService.getStopLocationsGroup(id);
    if (group != null) {
      return group.getCoordinate();
    }
    return null;
  }
}
