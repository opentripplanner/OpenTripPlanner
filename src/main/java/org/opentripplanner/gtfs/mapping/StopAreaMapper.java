package org.opentripplanner.gtfs.mapping;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.onebusaway.gtfs.model.Location;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopArea;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.GroupStopBuilder;
import org.opentripplanner.transit.model.site.StopLocation;

public class StopAreaMapper {

  private final String feedId;
  private final StopMapper stopMapper;

  private final LocationMapper locationMapper;

  private final Map<org.onebusaway.gtfs.model.StopArea, GroupStop> mappedLocationGroups = new HashMap<>();

  public StopAreaMapper(String feedId, StopMapper stopMapper, LocationMapper locationMapper) {
    this.feedId = feedId;
    this.stopMapper = stopMapper;
    this.locationMapper = locationMapper;
  }

  Collection<GroupStop> map(Collection<org.onebusaway.gtfs.model.StopArea> allAreas) {
    ImmutableMultimap<StopArea, StopLocation> mappedResults = allAreas.stream().collect(
      ImmutableListMultimap.<StopArea, StopArea, StopLocation>flatteningToImmutableListMultimap(
        x -> x,
        stopArea ->  {
          mapStopLocation(stopArea.getStopId());
        }
      )
    );

    mappedResults.
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  GroupStop map(org.onebusaway.gtfs.model.StopArea original) {
    return original == null ? null : mappedLocationGroups.computeIfAbsent(original, this::doMap);
  }

  private GroupStop doMap(Collection<StopArea> element) {
    GroupStopBuilder groupStopBuilder = GroupStop
      .of(new FeedScopedId(feedId, element.getId()))
      .withName(I18NString.of(element.getAreaId()));

    for (org.onebusaway.gtfs.model.StopLocation location : element.getLocations()) {
      mapStopLocation(groupStopBuilder, location);
    }

    return groupStopBuilder.build();
  }

  private StopLocation mapStopLocation(org.onebusaway.gtfs.model.StopLocation location) {
    if (location instanceof Stop stop) {
      return stopMapper.map(stop);
    } else if (location instanceof Location loc) {
      return locationMapper.map(loc);
    } else {
      throw new RuntimeException("Unknown location type: " + location.getClass().getSimpleName());
    }
  }
}
