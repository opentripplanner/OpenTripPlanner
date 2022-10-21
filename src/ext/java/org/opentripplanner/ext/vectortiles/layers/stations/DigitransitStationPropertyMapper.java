package org.opentripplanner.ext.vectortiles.layers.stations;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.simple.JSONArray;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;

public class DigitransitStationPropertyMapper extends PropertyMapper<Station> {

  private final TransitService transitService;

  private DigitransitStationPropertyMapper(TransitService transitService) {
    this.transitService = transitService;
  }

  public static DigitransitStationPropertyMapper create(TransitService transitService) {
    return new DigitransitStationPropertyMapper(transitService);
  }

  @Override
  public Collection<T2<String, Object>> map(Station station) {
    var childStops = station.getChildStops();

    return List.of(
      new T2<>("gtfsId", station.getId().toString()),
      // Name is I18NString now, we return default name
      new T2<>("name", station.getName().toString()),
      new T2<>(
        "type",
        childStops
          .stream()
          .flatMap(stop -> transitService.getPatternsForStop(stop).stream())
          .map(tripPattern -> tripPattern.getMode().name())
          .distinct()
          .collect(Collectors.joining(","))
      ),
      new T2<>(
        "stops",
        JSONArray.toJSONString(
          childStops.stream().map(StopLocation::getId).map(FeedScopedId::toString).toList()
        )
      ),
      new T2<>(
        "routes",
        JSONArray.toJSONString(
          childStops
            .stream()
            .flatMap(stop -> transitService.getRoutesForStop(stop).stream())
            .distinct()
            .map(route ->
              route.getShortName() == null
                ? Map.of("mode", route.getMode().name())
                : Map.of("mode", route.getMode().name(), "shortName", route.getShortName())
            )
            .collect(Collectors.toList())
        )
      )
    );
  }
}
