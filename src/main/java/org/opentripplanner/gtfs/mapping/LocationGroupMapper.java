package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.util.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

public class LocationGroupMapper {
  private static Logger LOG = LoggerFactory.getLogger(LocationGroupMapper.class);

  private final StopMapper stopMapper;

  private final LocationMapper locationMapper;

  private final Map<org.onebusaway.gtfs.model.LocationGroup, FlexLocationGroup> mappedLocationGroups = new HashMap<>();

  public LocationGroupMapper(StopMapper stopMapper, LocationMapper locationMapper) {
    this.stopMapper = stopMapper;
    this.locationMapper = locationMapper;
  }

  Collection<FlexLocationGroup> map(Collection<org.onebusaway.gtfs.model.LocationGroup> allLocationGroups) {
    return MapUtils.mapToList(allLocationGroups, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe.  */
  FlexLocationGroup map(org.onebusaway.gtfs.model.LocationGroup orginal) {
    return orginal == null ? null : mappedLocationGroups.computeIfAbsent(orginal, this::doMap);
  }

  private FlexLocationGroup doMap(org.onebusaway.gtfs.model.LocationGroup element) {
    FlexLocationGroup locationGroup = new FlexLocationGroup(mapAgencyAndId(element.getId()));
    locationGroup.setName(element.getName());

    for (org.onebusaway.gtfs.model.StopLocation location : element.getLocations()) {
      if (location instanceof org.onebusaway.gtfs.model.Stop) {
        locationGroup.addLocation(stopMapper.map((org.onebusaway.gtfs.model.Stop) location));
      }
      else if (location instanceof org.onebusaway.gtfs.model.Location) {
        locationGroup.addLocation(locationMapper.map((org.onebusaway.gtfs.model.Location) location));
      }
      else if (location instanceof org.onebusaway.gtfs.model.LocationGroup) {
        throw new RuntimeException("Nested LocationGroups are not allowed");
      }
      else {
        throw new RuntimeException("Unknown location type: " + location.getClass().getSimpleName());
      }
    }

    return locationGroup;
  }
}
