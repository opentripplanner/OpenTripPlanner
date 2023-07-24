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

public class StopAreaMapper {

  private final StopMapper stopMapper;

  private final LocationMapper locationMapper;

  private final Map<org.onebusaway.gtfs.model.StopArea, GroupStop> mappedStopAreas = new HashMap<>();

  public StopAreaMapper(StopMapper stopMapper, LocationMapper locationMapper) {
    this.stopMapper = stopMapper;
    this.locationMapper = locationMapper;
  }

  Collection<GroupStop> map(Collection<org.onebusaway.gtfs.model.StopArea> allLocationGroups) {
    return MapUtils.mapToList(allLocationGroups, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  GroupStop map(org.onebusaway.gtfs.model.StopArea original) {
    return original == null ? null : mappedStopAreas.computeIfAbsent(original, this::doMap);
  }

  private GroupStop doMap(org.onebusaway.gtfs.model.StopArea element) {
    GroupStopBuilder groupStopBuilder = GroupStop
      .of(mapAgencyAndId(element.getId()))
      .withName(new NonLocalizedString(element.getName()));

    for (org.onebusaway.gtfs.model.StopLocation location : element.getLocations()) {
      if (location instanceof Stop stop) {
        groupStopBuilder.addLocation(stopMapper.map(stop));
      } else if (location instanceof Location loc) {
        groupStopBuilder.addLocation(locationMapper.map(loc));
      } else if (location instanceof org.onebusaway.gtfs.model.StopArea) {
        throw new RuntimeException("Nested GroupStops are not allowed");
      } else {
        throw new RuntimeException("Unknown location type: " + location.getClass().getSimpleName());
      }
    }

    return groupStopBuilder.build();
  }
}
