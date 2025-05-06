package org.opentripplanner.gtfs.mapping;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StationElement;
import org.opentripplanner.transit.model.site.StopLevel;

/**
 * Wrap GTFS Stop to provide a common base mapping for all {@link StationElement}s.
 */
class StopMappingWrapper {

  private final IdFactory idFactory;
  private final Stop stop;

  public StopMappingWrapper(IdFactory idFactory, Stop stop) {
    this.idFactory = idFactory;
    this.stop = stop;
  }

  public FeedScopedId getId() {
    return idFactory.createId(stop.getId());
  }

  public String getName() {
    return stop.getName();
  }

  public String getCode() {
    return stop.getCode();
  }

  public String getDescription() {
    return stop.getDesc();
  }

  public WgsCoordinate getCoordinate() {
    return WgsCoordinateMapper.mapToDomain(stop);
  }

  public Accessibility getWheelchairAccessibility() {
    return WheelchairAccessibilityMapper.map(stop.getWheelchairBoarding());
  }

  public StopLevel getLevel() {
    if (stop.getLevel() == null) {
      return null;
    }
    return new StopLevel(stop.getLevel().getName(), stop.getLevel().getIndex());
  }

  public FeedScopedId getParentStationId() {
    return stop.getParentStation() == null
      ? null
      : new FeedScopedId(stop.getId().getAgencyId(), stop.getParentStation());
  }
}
