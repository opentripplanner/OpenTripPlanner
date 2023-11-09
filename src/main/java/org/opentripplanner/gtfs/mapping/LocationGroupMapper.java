package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.onebusaway.gtfs.model.Location;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.framework.collection.MapUtils;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.GroupStopBuilder;
import org.opentripplanner.transit.service.StopModelBuilder;

public class LocationGroupMapper {

  private final StopMapper stopMapper;
  private final LocationMapper locationMapper;
  private final StopModelBuilder stopModelBuilder;

  private final Map<org.onebusaway.gtfs.model.LocationGroup, GroupStop> mappedLocationGroups = new HashMap<>();

  public LocationGroupMapper(
    StopMapper stopMapper,
    LocationMapper locationMapper,
    StopModelBuilder stopModelBuilder
  ) {
    this.stopMapper = stopMapper;
    this.locationMapper = locationMapper;
    this.stopModelBuilder = stopModelBuilder;
  }

  Collection<GroupStop> map(Collection<org.onebusaway.gtfs.model.LocationGroup> allLocationGroups) {
    return MapUtils.mapToList(allLocationGroups, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  GroupStop map(org.onebusaway.gtfs.model.LocationGroup original) {
    return original == null ? null : mappedLocationGroups.computeIfAbsent(original, this::doMap);
  }

  private GroupStop doMap(org.onebusaway.gtfs.model.LocationGroup element) {
    GroupStopBuilder groupStopBuilder = stopModelBuilder
      .groupStop(mapAgencyAndId(element.getId()))
      .withName(new NonLocalizedString(element.getName()));

    for (org.onebusaway.gtfs.model.StopLocation location : element.getLocations()) {
      if (location instanceof Stop) {
        groupStopBuilder.addLocation(stopMapper.map((Stop) location));
      } else if (location instanceof Location) {
        groupStopBuilder.addLocation(locationMapper.map((Location) location));
      } else if (location instanceof org.onebusaway.gtfs.model.LocationGroup) {
        throw new RuntimeException("Nested GroupStops are not allowed");
      } else {
        throw new RuntimeException("Unknown location type: " + location.getClass().getSimpleName());
      }
    }

    return groupStopBuilder.build();
  }
}
