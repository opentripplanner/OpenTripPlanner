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

/**
 * For a while GTFS Flex location groups were replaced by GTFS Fares v2 stop areas. After a few
 * months, this decision was reverted and a new style of location groups were re-added to the Flex
 * spec.
 * @deprecated Arcadis tooling still produces stop areas and for a while we will support both. Please don't rely
 * on this as the class will be removed in the future!
 */
@Deprecated
public class StopAreaMapper {

  private final StopMapper stopMapper;

  private final LocationMapper locationMapper;

  private final Map<org.onebusaway.gtfs.model.StopArea, GroupStop> mappedStopAreas = new HashMap<>();
  private final StopModelBuilder stopModel;

  public StopAreaMapper(
    StopMapper stopMapper,
    LocationMapper locationMapper,
    StopModelBuilder stopModel
  ) {
    this.stopMapper = stopMapper;
    this.locationMapper = locationMapper;
    this.stopModel = stopModel;
  }

  Collection<GroupStop> map(Collection<org.onebusaway.gtfs.model.StopArea> allAreas) {
    return MapUtils.mapToList(allAreas, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  GroupStop map(org.onebusaway.gtfs.model.StopArea original) {
    return original == null ? null : mappedStopAreas.computeIfAbsent(original, this::doMap);
  }

  private GroupStop doMap(org.onebusaway.gtfs.model.StopArea element) {
    GroupStopBuilder groupStopBuilder = stopModel
      .groupStop(mapAgencyAndId(element.getId()))
      .withName(new NonLocalizedString(element.getName()));

    for (org.onebusaway.gtfs.model.StopLocation location : element.getLocations()) {
      switch (location) {
        case Stop stop -> groupStopBuilder.addLocation(stopMapper.map(stop));
        case Location loc -> groupStopBuilder.addLocation(locationMapper.map(loc));
        case org.onebusaway.gtfs.model.StopArea ignored -> throw new RuntimeException(
          "Nested GroupStops are not allowed"
        );
        case null, default -> throw new RuntimeException(
          "Unknown location type: " + location.getClass().getSimpleName()
        );
      }
    }

    return groupStopBuilder.build();
  }
}
