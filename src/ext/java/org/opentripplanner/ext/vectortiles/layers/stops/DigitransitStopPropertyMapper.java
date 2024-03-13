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

  DigitransitStopPropertyMapper(TransitService transitService, Locale locale) {
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
    return getKeyValues(
      stop,
      i18NStringMapper.mapNonnullToApi(stop.getName()),
      i18NStringMapper.mapToApi(stop.getDescription()),
      getType(transitService, stop),
      getRoutes(transitService, stop)
    );
  }

  protected static Collection<KeyValue> getKeyValues(
    RegularStop stop,
    String name,
    String description,
    String type,
    String routes
  ) {
    return List.of(
      new KeyValue("gtfsId", stop.getId().toString()),
      new KeyValue("name", name),
      new KeyValue("code", stop.getCode()),
      new KeyValue("platform", stop.getPlatformCode()),
      new KeyValue("desc", description),
      new KeyValue(
        "parentStation",
        stop.getParentStation() != null ? stop.getParentStation().getId() : null
      ),
      new KeyValue("type", type),
      new KeyValue("routes", routes)
    );
  }

  protected static String getRoutes(TransitService transitService, RegularStop stop) {
    return JSONArray.toJSONString(
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
  }

  protected static String getType(TransitService transitService, RegularStop stop) {
    Collection<TripPattern> patternsForStop = transitService.getPatternsForStop(stop);

    return patternsForStop
      .stream()
      .map(TripPattern::getMode)
      .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
      .entrySet()
      .stream()
      .max(Map.Entry.comparingByValue())
      .map(Map.Entry::getKey)
      .map(Enum::name)
      .orElse(null);
  }
}
