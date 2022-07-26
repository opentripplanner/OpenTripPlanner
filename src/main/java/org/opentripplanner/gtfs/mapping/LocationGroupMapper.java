package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.site.FlexLocationGroup;
import org.opentripplanner.transit.model.site.FlexLocationGroupBuilder;
import org.opentripplanner.util.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationGroupMapper {

  private static final Logger LOG = LoggerFactory.getLogger(LocationGroupMapper.class);

  private final StopMapper stopMapper;

  private final LocationMapper locationMapper;

  private final Map<org.onebusaway.gtfs.model.LocationGroup, FlexLocationGroup> mappedLocationGroups = new HashMap<>();

  public LocationGroupMapper(StopMapper stopMapper, LocationMapper locationMapper) {
    this.stopMapper = stopMapper;
    this.locationMapper = locationMapper;
  }

  Collection<FlexLocationGroup> map(
    Collection<org.onebusaway.gtfs.model.LocationGroup> allLocationGroups
  ) {
    return MapUtils.mapToList(allLocationGroups, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  FlexLocationGroup map(org.onebusaway.gtfs.model.LocationGroup original) {
    return original == null ? null : mappedLocationGroups.computeIfAbsent(original, this::doMap);
  }

  private FlexLocationGroup doMap(org.onebusaway.gtfs.model.LocationGroup element) {
    FlexLocationGroupBuilder flexLocationGroupBuilder = FlexLocationGroup
      .of(mapAgencyAndId(element.getId()))
      .withName(new NonLocalizedString(element.getName()));

    for (org.onebusaway.gtfs.model.StopLocation location : element.getLocations()) {
      if (location instanceof org.onebusaway.gtfs.model.Stop) {
        flexLocationGroupBuilder.addLocation(
          stopMapper.map((org.onebusaway.gtfs.model.Stop) location)
        );
      } else if (location instanceof org.onebusaway.gtfs.model.Location) {
        flexLocationGroupBuilder.addLocation(
          locationMapper.map((org.onebusaway.gtfs.model.Location) location)
        );
      } else if (location instanceof org.onebusaway.gtfs.model.LocationGroup) {
        throw new RuntimeException("Nested LocationGroups are not allowed");
      } else {
        throw new RuntimeException("Unknown location type: " + location.getClass().getSimpleName());
      }
    }

    return flexLocationGroupBuilder.build();
  }
}
