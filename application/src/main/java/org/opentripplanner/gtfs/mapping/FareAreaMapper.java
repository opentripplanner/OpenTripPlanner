package org.opentripplanner.gtfs.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.onebusaway.gtfs.model.Area;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopAreaElement;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.site.FareArea;

/**
 * Maps GTFS {@code areas.txt} into {@link FareArea}s and keeps the assignments of
 * {@code stop_areas.txt}, so that the stop and station mappers can look up the areas of a stop.
 * <p>
 * NOTE! This class has state. Each GTFS area is mapped to exactly one {@link FareArea} instance.
 * The assignments must be added with {@link #map(Collection)} before stops and stations are
 * mapped.
 */
class FareAreaMapper {

  private final IdFactory idFactory;
  private final Map<Area, FareArea> mappedAreas = new HashMap<>();
  private final Multimap<FeedScopedId, FareArea> areasByStopId = ArrayListMultimap.create();

  FareAreaMapper(IdFactory idFactory) {
    this.idFactory = idFactory;
  }

  /** Add the GTFS {@code stop_areas.txt} rows, mapping the referenced areas. */
  void map(Collection<StopAreaElement> stopAreaElements) {
    for (var element : stopAreaElements) {
      var stopId = idFactory.createId(element.getStop().getId(), "stop area assignment's stop");
      areasByStopId.put(stopId, map(element.getArea()));
    }
  }

  /** Map from GTFS to OTP model, returns the same instance when called twice for the same area. */
  FareArea map(Area area) {
    return mappedAreas.computeIfAbsent(area, this::doMap);
  }

  /**
   * The areas the given stop or station is assigned to in {@code stop_areas.txt}. Only the rows of
   * the stop itself are returned, areas of the parent station are not included. Empty if the stop
   * has no assignment.
   */
  Collection<FareArea> areasFor(Stop stop) {
    return areasByStopId.get(idFactory.createId(stop.getId(), "stop"));
  }

  private FareArea doMap(Area area) {
    return FareArea.of(idFactory.createId(area.getId(), "area")).withName(area.getName()).build();
  }
}
