package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.onebusaway.gtfs.model.Location;
import org.onebusaway.gtfs.model.LocationGroup;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;
import org.opentripplanner.utils.collection.MapUtils;

class LocationGroupMapper {

  private static final LocalizedString FALLBACK_NAME = new LocalizedString("locationGroup");
  private final IdFactory idFactory;
  private final StopMapper stopMapper;
  private final LocationMapper locationMapper;
  private final SiteRepositoryBuilder siteRepositoryBuilder;

  private final Map<LocationGroup, GroupStop> mappedLocationGroups = new HashMap<>();

  public LocationGroupMapper(
    IdFactory idFactory,
    StopMapper stopMapper,
    LocationMapper locationMapper,
    SiteRepositoryBuilder siteRepositoryBuilder
  ) {
    this.idFactory = idFactory;
    this.stopMapper = stopMapper;
    this.locationMapper = locationMapper;
    this.siteRepositoryBuilder = siteRepositoryBuilder;
  }

  Collection<GroupStop> map(Collection<LocationGroup> allLocationGroups) {
    return MapUtils.mapToList(allLocationGroups, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  GroupStop map(@Nullable LocationGroup original) {
    return original == null ? null : mappedLocationGroups.computeIfAbsent(original, this::doMap);
  }

  private GroupStop doMap(LocationGroup element) {
    var id = idFactory.createId(element.getId(), "location group");
    // the GTFS spec allows name-less location groups: https://gtfs.org/documentation/schedule/reference/#location_groupstxt
    var name = NonLocalizedString.ofNullableOrElse(element.getName(), FALLBACK_NAME);
    var groupStopBuilder = siteRepositoryBuilder.groupStop(id).withName(name);

    for (var location : element.getLocations()) {
      Objects.requireNonNull(
        location,
        "Location group '%s' contains a null element.".formatted(element.getId())
      );
      switch (location) {
        case Stop stop -> groupStopBuilder.addLocation(stopMapper.map(stop));
        case Location loc -> groupStopBuilder.addLocation(locationMapper.map(loc));
        case LocationGroup ignored -> throw new RuntimeException(
          "Nested GroupStops are not allowed"
        );
        default -> throw new RuntimeException(
          "Unknown location type: " + location.getClass().getSimpleName()
        );
      }
    }

    return groupStopBuilder.build();
  }
}
