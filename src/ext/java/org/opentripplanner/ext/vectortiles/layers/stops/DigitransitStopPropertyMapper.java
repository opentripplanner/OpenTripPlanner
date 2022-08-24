package org.opentripplanner.ext.vectortiles.layers.stops;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;

public class DigitransitStopPropertyMapper extends PropertyMapper<RegularStop> {

  private final TransitService transitService;

  private DigitransitStopPropertyMapper(TransitService transitService) {
    this.transitService = transitService;
  }

  public static DigitransitStopPropertyMapper create(TransitService transitService) {
    return new DigitransitStopPropertyMapper(transitService);
  }

  @Override
  public Collection<T2<String, Object>> map(RegularStop stop) {
    Collection<TripPattern> patternsForStop = transitService.getPatternsForStop(stop);

    String type = patternsForStop
      .stream()
      .map(TripPattern::getMode)
      .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
      .entrySet()
      .stream()
      .max(Map.Entry.comparingByValue())
      .map(Map.Entry::getKey)
      .map(Enum::name)
      .orElse(null);

    String routes = JSONArray.toJSONString(
      transitService
        .getRoutesForStop(stop)
        .stream()
        .map(route -> {
          JSONObject routeObject = new JSONObject();
          routeObject.put("gtfsType", route.getGtfsType());
          return routeObject;
        })
        .toList()
    );
    String desc = stop.getDescription() != null ? stop.getDescription().toString() : null;
    return List.of(
      new T2<>("gtfsId", stop.getId().toString()),
      // Name is I18NString now, we return default name
      new T2<>("name", stop.getName().toString()),
      new T2<>("code", stop.getCode()),
      new T2<>("platform", stop.getPlatformCode()),
      new T2<>("desc", desc),
      new T2<>(
        "parentStation",
        stop.getParentStation() != null ? stop.getParentStation().getId() : "null"
      ),
      new T2<>("type", type),
      new T2<>("routes", routes)
    );
  }
}
