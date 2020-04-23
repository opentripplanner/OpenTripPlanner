package org.opentripplanner.gtfs.mapping;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.StationElement;
import org.opentripplanner.model.StopLevel;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.WheelChairBoarding;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

/**
 * Wrap GTFS Stop to provide a common base mapping for all {@link StationElement}s.
 */
class StopMappingWrapper {

  final Stop stop;

  public StopMappingWrapper(Stop stop) {
    this.stop = stop;
  }

  public FeedScopedId getId() {
    return mapAgencyAndId(stop.getId());
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

  public WheelChairBoarding getWheelchairBoarding() {
    return WheelChairBoarding.valueOfGtfsCode(stop.getWheelchairBoarding());
  }

  public StopLevel getLevel() {
    if (stop.getLevel() == null) { return null; }
    return new StopLevel(stop.getLevel().getName(), stop.getLevel().getIndex());
  }
}
