package org.opentripplanner.ext.vectortiles.layers.stops;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;

public class DigitransitStopPropertyMapper extends PropertyMapper<RegularStop> {

  private final TransitService transitService;
  private final I18NStringMapper i18NStringMapper;

  private DigitransitStopPropertyMapper(TransitService transitService, Locale locale) {
    this.transitService = transitService;
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  protected static DigitransitStopPropertyMapper create(
    TransitService transitService,
    Locale locale
  ) {
    return new DigitransitStopPropertyMapper(transitService, locale);
  }

  @Override
  protected Collection<KeyValue> map(RegularStop stop) {
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
    return List.of(
      new KeyValue("gtfsId", stop.getId().toString()),
      new KeyValue("name", i18NStringMapper.mapNonnullToApi(stop.getName())),
      new KeyValue("code", stop.getCode()),
      new KeyValue("platform", stop.getPlatformCode()),
      new KeyValue("desc", i18NStringMapper.mapToApi(stop.getDescription())),
      new KeyValue(
        "parentStation",
        stop.getParentStation() != null ? stop.getParentStation().getId() : "null"
      ),
      new KeyValue("type", type),
      new KeyValue("routes", routes)
    );
  }
}
